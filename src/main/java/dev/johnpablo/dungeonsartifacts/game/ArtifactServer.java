package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.*;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class ArtifactServer {
    private static final List<Holder<MobEffect>> GOOD = List.of(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.REGENERATION, MobEffects.RESISTANCE, MobEffects.FIRE_RESISTANCE, MobEffects.JUMP_BOOST);
    private static final List<Holder<MobEffect>> BAD = List.of(MobEffects.POISON, MobEffects.WEAKNESS, MobEffects.SLOWNESS);

    public static long now(ServerPlayer p) { return PlayerLedger.clock(p.level().getServer().overworld().getGameTime()); }
    public static PlayerLedger ledger(ServerPlayer p) {
        return PlayerLedger.restore(capacity(p), p.getAttachedOrCreate(PlayerData.STATE));
    }
    public static void save(ServerPlayer p, PlayerLedger state) {
        if(state.souls()==state.capacity()&&p.getAttachedOrCreate(PlayerData.FRACTION)!=0)p.setAttached(PlayerData.FRACTION,0);
        var snapshot = state.snapshot();
        if (!snapshot.equals(p.getAttachedOrCreate(PlayerData.STATE))) p.setAttached(PlayerData.STATE, snapshot);
        sync(p, PlayerData.SOULS, state.souls());
        sync(p, PlayerData.CAPACITY, state.capacity());
        sync(p, PlayerData.BOOTS_TIME, (int)state.bootsRemainingHalfTicks(now(p)));
        sync(p, PlayerData.BOOTS_COOLDOWN, (int)state.remainingHalfTicks(Artifact.BOOTS_OF_LEVITATION, now(p)));
    }
    private static void sync(ServerPlayer p, net.fabricmc.fabric.api.attachment.v1.AttachmentType<Integer> type, int n) {
        if (p.getAttachedOrCreate(type) != n) p.setAttached(type, n);
    }
    public static int capacity(ServerPlayer p) {
        int amount = DungeonsArtifacts.CONFIG.soulCapacity;
        var registry = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var entry : Map.of("soul_vessel", DungeonsArtifacts.CONFIG.vesselCapacityPerLevel, "soul_reservoir", DungeonsArtifacts.CONFIG.reservoirCapacityPerLevel).entrySet()) {
            var enchantment = registry.get(ResourceKey.create(Registries.ENCHANTMENT, DungeonsArtifacts.id(entry.getKey())));
            if (enchantment.isPresent()) for (EquipmentSlot slot : entry.getKey().equals("soul_vessel")
                    ? List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
                    : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND))
                amount += EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), p.getItemBySlot(slot)) * entry.getValue();
        }
        return amount;
    }
    public static void initialize() {
        ServerTickEvents.START_SERVER_TICK.register(ShieldSystem::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> AttunementSystem.tick(server.overworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                PlayerLedger state = ledger(p);
                if (state.tickBoots(now(p)) == PlayerLedger.Boots.EXPIRED) land(p);
                if (state.bootsActive()) {
                    if (!p.isAlive() || p.isSpectator() || p.isPassenger()) { state.stopBoots(now(p)); land(p); }
                    else effect(p, MobEffects.LEVITATION, 5, 1);
                }
                GhostMovement.tick(p);
                AttunementSystem.tickPlayer(p);
                save(p, state);
                int denied = p.getAttachedOrCreate(PlayerData.DENIED);
                if (denied > 0) p.setAttached(PlayerData.DENIED, denied - 1);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Interrupted sessions must not retain flight or unsafe phasing.
            cleanup(handler.player, true);
            save(handler.player, ledger(handler.player));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cleanup(handler.player, true));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, origin, destination) -> cleanup(p, true));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) cleanup(p, false);
            Entity killer = source.getEntity();
            if (killer instanceof TamableAnimal pet) killer = pet.getOwner();
            if (entity instanceof Enemy && killer instanceof ServerPlayer p && !allied(p, entity)) {
                var state = ledger(p);
                int gained = SoulGathering.collectKill(p, state, source);
                save(p, state);
                if (gained > 0) { particles(p.level(), ParticleTypes.SOUL, p.position().add(0, 1, 0), 12); sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, .5f); }
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof LivingEntity && entity.hasEffect(ArtifactEffects.STUNNED) && source.getEntity() == entity) return false;
            if (source.getEntity() instanceof LivingEntity attacker && attacker.hasEffect(ArtifactEffects.STUNNED)) return false;
            return true;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            server.getPlayerList().getPlayers().forEach(p -> cleanup(p, true));
            ShieldSystem.clear();
            AttunementSystem.clear();
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity.entityTags().contains("dungeons_artifacts_shield") && !ShieldSystem.owns(entity.getUUID())) entity.discard();
        });
    }
    private static void cleanup(ServerPlayer p, boolean safeExit) {
        AttunementSystem.release(p.getUUID());
        var state = ledger(p);
        if (state.bootsActive()) { state.stopBoots(now(p)); land(p); }
        GhostMovement.end(p, safeExit);
        ShieldSystem.removeOwner(p.getUUID());
        save(p, state);
    }
    private static void land(ServerPlayer p) {
        p.removeEffect(MobEffects.LEVITATION);

        p.fallDistance = 0;
    }
    public static InteractionResult use(ServerPlayer p, InteractionHand hand, Artifact artifact) {
        if (!p.isAlive() || p.isSpectator() || p.hasEffect(ArtifactEffects.STUNNED)) return InteractionResult.FAIL;
        // Vanilla use packets contain only hand/sequence: the held item chooses the ability.
        if (p.getItemInHand(hand).getItem() != ArtifactItems.ITEMS.get(artifact)) return InteractionResult.FAIL;
        var state = ledger(p);
        if (artifact == Artifact.BOOTS_OF_LEVITATION) {
            if (p.isPassenger()) return InteractionResult.FAIL;
            var result = state.toggleBoots(now(p));
            if (result == PlayerLedger.Boots.COOLDOWN) return deny(p, "cooldown");
            if (result == PlayerLedger.Boots.STARTED) { effect(p, MobEffects.LEVITATION, 5, 1); sound(p, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1); }
            else land(p);
            save(p, state);
            return InteractionResult.SUCCESS;
        }
        int cooldown = artifact == Artifact.TOTEM_OF_SHIELDING ? DungeonsArtifacts.CONFIG.shieldCooldownTicks : artifact.cooldownTicks;
        var result = state.activate(artifact, now(p), artifact.soulCost, cooldown, () -> activate(p, hand, artifact));
        save(p, state);
        if (result == PlayerLedger.Activation.SUCCESS) {
            if (cooldown > 0) p.getCooldowns().addCooldown(p.getItemInHand(hand), cooldown);
            if (artifact == Artifact.DEATH_CAP_MUSHROOM) p.getItemInHand(hand).consume(1, p);
            return InteractionResult.SUCCESS;
        }
        return deny(p, result == PlayerLedger.Activation.INSUFFICIENT_SOULS ? "souls" : result == PlayerLedger.Activation.COOLDOWN ? "cooldown" : "target");
    }
    private static InteractionResult deny(ServerPlayer p, String reason) {
        p.sendOverlayMessage(Component.translatable("message.dungeons_artifacts." + reason));
        p.setAttached(PlayerData.DENIED, 30);
        sound(p, SoundEvents.NOTE_BLOCK_BASS.value(), .5f);
        return InteractionResult.FAIL;
    }
    private static boolean activate(ServerPlayer p, InteractionHand hand, Artifact a) {
        var c = DungeonsArtifacts.CONFIG;
        ServerLevel level = p.level();
        switch (a) {
            case SOUL_HEALER -> {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10), e -> allied(p, e) && e.isAlive() && e.distanceToSqr(p) <= 100)) {
                    // Instant Health is inverted for undead; allied undead must be healed, never harmed.
                    if (target.isInvertedHealAndHarm()) target.heal(8);
                    else MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(level, p, p, target, 1, 1);
                    particles(level, ParticleTypes.HEART, target.position().add(0, 1, 0), 8);
                }
                sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1);
            }
            case SATCHEL_OF_ELIXIRS -> {
                if (p.isShiftKeyDown()) {
                    ItemStack potion = new ItemStack(Items.SPLASH_POTION);
                    potion.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(BAD.get(level.getRandom().nextInt(BAD.size())), 200, 0)));
                    Projectile.spawnProjectileFromRotation(ThrownSplashPotion::new, level, potion, p, -20, .7f, 1);
                    sound(p, SoundEvents.SPLASH_POTION_THROW, 1);
                } else {
                    int first = level.getRandom().nextInt(GOOD.size());
                    int second = (first + 1 + level.getRandom().nextInt(GOOD.size() - 1)) % GOOD.size();
                    effect(p, GOOD.get(first), 300, 0); effect(p, GOOD.get(second), 300, 0);
                    particles(level, ParticleTypes.WITCH, p.position(), 20);
                    sound(p, SoundEvents.GENERIC_DRINK.value(), 1);
                }
            }
            case TOTEM_OF_SHIELDING -> { return ShieldSystem.deploy(p); }
            case LIGHTNING_ROD -> {
                var hit = level.clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(c.lightningRange)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (hit.getType() == HitResult.Type.MISS) return false;
                Vec3 pos = hit.getLocation();
                lightning(p, pos);
                // Preserve the rod's immediate ten HP; real flashes obey vanilla hurt immunity.
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(c.lightningRadius), e -> enemy(p, e) && e.distanceToSqr(pos) <= c.lightningRadius*c.lightningRadius))
                    e.hurtServer(level, level.damageSources().indirectMagic(p, p), 10);
            }
            case CORRUPTED_SEEDS -> {
                var targets = enemies(p, c.seedsRadius); if (targets.isEmpty()) return false;
                for (LivingEntity e : targets) { effect(e, ArtifactEffects.ROOTED, 100, 0); effect(e, MobEffects.POISON, 300, 1); particles(level, ParticleTypes.SPORE_BLOSSOM_AIR, e.position(), 30); }
                sound(p, SoundEvents.ROOTED_DIRT_BREAK, 1);
            }
            case GONG_OF_WEAKENING -> {
                var targets = enemies(p, 10); if (targets.isEmpty()) return false;
                targets.forEach(e -> effect(e, MobEffects.WEAKNESS, 600, 0));
                ring(level, p.position(), 10, ParticleTypes.ENCHANT);
                sound(p, SoundEvents.BELL_BLOCK, 1);
            }
            case SATCHEL_OF_ELEMENTS -> {
                var targets = enemies(p, c.elementsRadius); if (targets.isEmpty()) return false;
                int element = level.getRandom().nextInt(3);
                for (LivingEntity e : targets) switch (element) {
                    case 0 -> { lightning(p, e.position()); e.hurtServer(level, level.damageSources().indirectMagic(p, p), c.elementalLightningDamage); }
                    case 1 -> { e.igniteForSeconds(c.elementalFireSeconds); particles(level, ParticleTypes.FLAME, e.position(), 25); }
                    default -> { effect(e, MobEffects.LEVITATION, c.elementalWindTicks, 0); particles(level, ParticleTypes.CLOUD, e.position(), 25); }
                }
                sound(p, element == 1 ? SoundEvents.FIRECHARGE_USE : SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1);
            }
            case DEATH_CAP_MUSHROOM -> {
                effect(p, MobEffects.SPEED, 300, 1); effect(p, MobEffects.STRENGTH, 200, 1);
                float damage = level.getDifficulty() == Difficulty.HARD ? 2 : level.getDifficulty() == Difficulty.PEACEFUL ? 0 : 1;
                if (damage > 0) p.hurtServer(level, level.damageSources().magic(), damage);
                particles(level, new ItemParticleOption(ParticleTypes.ITEM, p.getItemInHand(hand).getItem()), p.getEyePosition(), 15);
                sound(p, SoundEvents.GENERIC_EAT.value(), 1);
            }
            case GHOST_CLOAK -> { return GhostMovement.begin(p); }
            case UPDRAFT_TOME -> {
                var targets = enemies(p, 15); if (targets.isEmpty()) return false;
                targets.sort(Comparator.comparingDouble(p::distanceToSqr));
                for (LivingEntity e : targets.stream().limit(6).toList()) {
                    effect(e, MobEffects.LEVITATION, 60, 1);
                    e.setAttached(PlayerData.UPDRAFT, 1);
                    particles(level, ParticleTypes.CLOUD, e.position(), 35);
                }
                sound(p, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1);
            }
            case BOOTS_OF_LEVITATION -> throw new IllegalStateException("Boots use their own toggle state machine");
        }
        return true;
    }
    public static boolean allied(ServerPlayer p, LivingEntity e) {
        if (e == p || p.isAlliedTo(e)) return true;
        if (e instanceof Player) return !DungeonsArtifacts.CONFIG.hostilePlayers;
        return e instanceof TamableAnimal pet && pet.getOwner() != null && (pet.isOwnedBy(p) || p.isAlliedTo(pet.getOwner()));
    }
    public static boolean enemy(ServerPlayer p, LivingEntity e) {
        if (!e.isAlive() || e.isSpectator() || allied(p, e)) return false;
        return e instanceof Enemy || e instanceof Player && DungeonsArtifacts.CONFIG.hostilePlayers
            || e instanceof Mob mob && mob.getTarget() != null && allied(p, mob.getTarget());
    }
    public static List<LivingEntity> enemies(ServerPlayer p, double range) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), e -> enemy(p,e) && e.distanceToSqr(p) <= range*range);
    }
    public static void effect(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) { e.addEffect(new MobEffectInstance(effect, ticks, amp, false, false, true)); }
    public static void particles(ServerLevel level, ParticleOptions type, Vec3 pos, int count) { level.sendParticles(type, pos.x, pos.y, pos.z, count, .4, .6, .4, .04); }
    public static void sound(ServerPlayer p, SoundEvent sound, float pitch) { p.level().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, 1, pitch); }
    public static void ring(ServerLevel level, Vec3 pos, double radius, ParticleOptions particle) {
        for (int i=0; i<48; i++) { double theta = i*Math.PI/24; level.sendParticles(particle, pos.x+Math.cos(theta)*radius, pos.y+.3, pos.z+Math.sin(theta)*radius, 1, 0, .1, 0, 0); }
    }
    private static void lightning(ServerPlayer p, Vec3 pos) {
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, p.level());
        bolt.setPos(pos); bolt.setVisualOnly(false); bolt.setCause(p); p.level().addFreshEntity(bolt);
        particles(p.level(), ParticleTypes.ELECTRIC_SPARK, pos, 30);
    }
}

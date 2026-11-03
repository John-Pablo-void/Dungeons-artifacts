package dev.johnpablo.dungeonsartifacts;

import java.util.*;
import dev.johnpablo.dungeonsartifacts.core.*;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import dev.johnpablo.dungeonsartifacts.game.*;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class ArtifactGameTests {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p=connect(h,UUID.randomUUID());
        p.setGameMode(GameType.CREATIVE);
        p.teleportTo(h.absoluteVec(new Vec3(3,2,3)).x,h.absoluteVec(new Vec3(3,2,3)).y,h.absoluteVec(new Vec3(3,2,3)).z);
        for(int x=0;x<8;x++)for(int z=0;z<8;z++) h.setBlock(x,1,z,Blocks.STONE);
        p.setYRot(0);p.setXRot(60);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var s=ArtifactServer.ledger(p);s.collect(50);ArtifactServer.save(p,s);
        return p;
    }
    private static ServerPlayer connect(GameTestHelper h,UUID uuid) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(uuid,"ArtifactTest"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        h.getLevel().getServer().getPlayerList().loadPlayerData(p.nameAndId()).ifPresent(tag ->
            p.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag)));
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        return p;
    }
    private static void finish(GameTestHelper h,ServerPlayer p){p.level().getServer().getPlayerList().remove(p);p.discard();h.succeed();}
    private static InteractionResult use(ServerPlayer p,Artifact a){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ArtifactItems.ITEMS.get(a)));return ArtifactServer.use(p,InteractionHand.MAIN_HAND,a);}
    private static Zombie zombie(GameTestHelper h){Zombie e=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,4));e.setNoAi(true);return e;}
    @GameTest(padding=20) public void registryAndHealing(GameTestHelper h){
        check(ArtifactItems.ITEMS.size()==11,"Eleven items registered");
        ServerPlayer p=player(h);p.setHealth(4);
        Zombie ally=zombie(h);ally.setHealth(4);
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("artifact_healers");
        board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
        check(use(p,Artifact.SOUL_HEALER)==InteractionResult.SUCCESS,"Heal use");
        check(p.getHealth()==12,"Instant Health II restores 8 HP");check(ArtifactServer.ledger(p).souls()==45,"Five souls charged");
        check(ally.getHealth()==12,"Undead allies are healed rather than harmed");board.removePlayerTeam(team);
        check(use(p,Artifact.SOUL_HEALER)==InteractionResult.FAIL,"Cooldown blocks different stack");
        check(ArtifactServer.ledger(p).souls()==45,"Denied use free");finish(h,p);
    }
    @GameTest(padding=20) public void insufficientSouls(GameTestHelper h){ServerPlayer p=player(h);ArtifactServer.save(p,new PlayerLedger(50));check(use(p,Artifact.GHOST_CLOAK)==InteractionResult.FAIL,"Ghost denied without souls");check(!p.hasEffect(ArtifactEffects.GHOST),"No ghost effect");finish(h,p);}
    @GameTest(padding=20) public void elixirsAndSplash(GameTestHelper h){ServerPlayer p=player(h);check(use(p,Artifact.SATCHEL_OF_ELIXIRS)==InteractionResult.SUCCESS,"Drink elixirs");check(p.getActiveEffects().size()==2,"Two distinct effects");ArtifactServer.save(p,new PlayerLedger(50));p.setShiftKeyDown(true);check(use(p,Artifact.SATCHEL_OF_ELIXIRS)==InteractionResult.SUCCESS,"Throw potion");check(h.getLevel().getEntitiesOfClass(ThrownSplashPotion.class,p.getBoundingBox().inflate(3)).size()==1,"Real splash projectile spawned");finish(h,p);}
    @GameTest(padding=20) public void seedsAndGong(GameTestHelper h){ServerPlayer p=player(h);Zombie z=zombie(h);use(p,Artifact.CORRUPTED_SEEDS);check(z.getEffect(MobEffects.POISON).getAmplifier()==1&&z.getEffect(MobEffects.POISON).getDuration()==300,"Poison II 15s");check(z.getEffect(ArtifactEffects.ROOTED).getDuration()==100,"Root 5s");Vec3 old=z.position();z.move(MoverType.SELF,new Vec3(1,0,1));check(z.position().equals(old),"Root really blocks movement");use(p,Artifact.GONG_OF_WEAKENING);check(z.getEffect(MobEffects.WEAKNESS).getDuration()==600,"Weakness 30s");check(!p.hasEffect(MobEffects.WEAKNESS),"Friendly player not weakened");finish(h,p);}
    @GameTest(padding=20) public void updraftStun(GameTestHelper h){ServerPlayer p=player(h);Zombie z=zombie(h);use(p,Artifact.UPDRAFT_TOME);check(z.getEffect(ArtifactEffects.STUNNED).getDuration()==90,"Stun 4.5s");check(z.getEffect(MobEffects.LEVITATION).getDuration()==60,"Levitation 3s");Vec3 old=z.position();z.move(MoverType.SELF,new Vec3(1,.2,0));check(z.getX()==old.x&&z.getY()>old.y,"Stun keeps upward motion but blocks horizontal movement");finish(h,p);}
    @GameTest(padding=20) public void mushroomAndBoots(GameTestHelper h){ServerPlayer p=player(h);use(p,Artifact.DEATH_CAP_MUSHROOM);check(p.getEffect(MobEffects.SPEED).getDuration()==300&&p.getEffect(MobEffects.SPEED).getAmplifier()==1,"Speed II");check(p.getEffect(MobEffects.STRENGTH).getDuration()==200,"Strength duration");check(p.getMainHandItem().getCount()==1,"Creative stack retained");check(use(p,Artifact.BOOTS_OF_LEVITATION)==InteractionResult.SUCCESS,"Boots on");check(p.hasEffect(MobEffects.LEVITATION),"Boots levitate");check(use(p,Artifact.BOOTS_OF_LEVITATION)==InteractionResult.SUCCESS,"Boots off");check(p.hasEffect(MobEffects.SLOW_FALLING)&&!p.hasEffect(MobEffects.LEVITATION),"Safe landing");finish(h,p);}
    @GameTest(padding=20) public void ghostCollisionAndExit(GameTestHelper h){ServerPlayer p=player(h);Zombie z=zombie(h);use(p,Artifact.GHOST_CLOAK);check(p.hasEffect(ArtifactEffects.GHOST),"Ghost active");check(ArtifactServer.ledger(p).souls()==35,"Ghost costs 15");z.setTarget(p);check(z.getTarget()==null&&!p.canBeSeenAsEnemy(),"Ghost cannot be targeted");h.setBlock(4,2,3,Blocks.STONE);h.setBlock(4,3,3,Blocks.STONE);Vec3 old=p.position();p.move(MoverType.SELF,new Vec3(1,0,0));check(p.getX()>old.x+.9,"Horizontal phase");Vec3 below=p.position();p.move(MoverType.SELF,new Vec3(0,-1,0));check(p.getY()==below.y,"Cannot cross floor");GhostMovement.end(p,true);check(!p.hasEffect(ArtifactEffects.GHOST)&&h.getLevel().noCollision(p),"Safe ghost exit");finish(h,p);}
    @GameTest(padding=20) public void lightningAndElements(GameTestHelper h){ServerPlayer p=player(h);Zombie z=zombie(h);z.setHealth(20);use(p,Artifact.LIGHTNING_ROD);check(ArtifactServer.ledger(p).souls()==48,"Lightning soul cost");check(z.getHealth()<20,"Lightning damages strike area");check(ArtifactServer.ledger(p).remainingHalfTicks(Artifact.LIGHTNING_ROD,ArtifactServer.now(p))==0,"No lightning cooldown");use(p,Artifact.SATCHEL_OF_ELEMENTS);check(ArtifactServer.ledger(p).remainingHalfTicks(Artifact.SATCHEL_OF_ELEMENTS,ArtifactServer.now(p))==600,"Elements 15 second cooldown");finish(h,p);}
    @GameTest(padding=20) public void shieldIntercepts(GameTestHelper h){ServerPlayer p=player(h);check(use(p,Artifact.TOTEM_OF_SHIELDING)==InteractionResult.SUCCESS,"Shield deployed");var displays=h.getLevel().getEntitiesOfClass(Display.ItemDisplay.class,p.getBoundingBox().inflate(10));check(displays.size()==1,"3D totem display");Vec3 center=displays.getFirst().position();Arrow arrow=new Arrow(EntityTypes.ARROW,h.getLevel());arrow.setPos(center.add(6,0,0));arrow.setDeltaMovement(-8,0,0);h.getLevel().addFreshEntity(arrow);ShieldSystem.tick(h.getLevel().getServer());check(arrow.isRemoved(),"Fast projectile intercepted by swept segment");ShieldSystem.removeOwner(p.getUUID());finish(h,p);}
    @GameTest(padding=20) public void soulsFromKillsAndEnchantments(GameTestHelper h){ServerPlayer p=player(h);ArtifactServer.save(p,new PlayerLedger(50));Zombie z=zombie(h);z.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(p),100);check(ArtifactServer.ledger(p).souls()==2,"Hostile kill grants souls");ItemStack helmet=new ItemStack(Items.IRON_HELMET);var enchant=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,DungeonsArtifacts.id("soul_vessel")));helmet.enchant(enchant,3);p.setItemSlot(EquipmentSlot.HEAD,helmet);check(ArtifactServer.capacity(p)==80,"Soul Vessel III adds capacity");finish(h,p);}
    @GameTest(padding=20) public void survivalConsumptionAndDamage(GameTestHelper h){
        ServerPlayer p=player(h);p.setGameMode(GameType.SURVIVAL);p.setHealth(20);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ArtifactItems.ITEMS.get(Artifact.DEATH_CAP_MUSHROOM),3));
        check(ArtifactServer.use(p,InteractionHand.MAIN_HAND,Artifact.DEATH_CAP_MUSHROOM)==InteractionResult.SUCCESS,"Survival mushroom");
        check(p.getMainHandItem().getCount()==2,"Exactly one mushroom consumed");
        float damage=h.getLevel().getDifficulty()==Difficulty.HARD?2:h.getLevel().getDifficulty()==Difficulty.PEACEFUL?0:1;
        check(p.getHealth()==20-damage,"Magic bypasses armor; health="+p.getHealth()+", expected="+(20-damage));
        check(ArtifactServer.use(p,InteractionHand.MAIN_HAND,Artifact.DEATH_CAP_MUSHROOM)==InteractionResult.FAIL&&p.getMainHandItem().getCount()==2,"Cooldown prevents stacked consumption");
        finish(h,p);
    }
    @GameTest(padding=20) public void persistenceAcrossReconnect(GameTestHelper h){
        ServerPlayer p=player(h);p.setHealth(1);use(p,Artifact.SOUL_HEALER);UUID uuid=p.getUUID();
        var before=p.getAttachedOrCreate(PlayerData.STATE);
        p.level().getServer().getPlayerList().remove(p);p.discard();
        ServerPlayer restored=connect(h,uuid);
        check(restored.getAttachedOrCreate(PlayerData.STATE).equals(before),"Soul and cooldown attachment survives real player file reload");
        check(ArtifactServer.ledger(restored).souls()==45,"Souls restored");finish(h,restored);
    }
    @GameTest(padding=20) public void shrineTemplatesAndLoot(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(0,2,0));
        for(String name:List.of("soul","grove","tempest")) {
            var template=level.getStructureManager().get(DungeonsArtifacts.id(name+"_shrine")).orElseThrow();
            check(template.placeInWorld(level,pos,pos,new StructurePlaceSettings(),level.getRandom(),2),"Structure placed");
            var chest=(ChestBlockEntity)level.getBlockEntity(pos.offset(4,2,4));check(chest!=null,"Shrine chest");
            check(chest.getLootTable()!=null,"Chest has loot table");boolean found=false;
            for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).getItem() instanceof ArtifactItems.ArtifactItem) found=true;
            check(found,"Guaranteed artifact loot");
        }
        check(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,DungeonsArtifacts.id("artifact_shrine"))).isPresent(),"Natural structure registered");h.succeed();
    }
}

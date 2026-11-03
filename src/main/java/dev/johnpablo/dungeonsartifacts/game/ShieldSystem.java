package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.Artifact;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class ShieldSystem {
    private record Shield(UUID owner, Display.ItemDisplay display, long expires, double radius) { }
    private static final Map<UUID,Shield> SHIELDS = new HashMap<>();
    public static boolean owns(UUID id) { return SHIELDS.containsKey(id); }
    public static boolean deploy(ServerPlayer p) {
        var hit = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(5)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) return false;
        Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, p.level());
        display.setPos(hit.getLocation().add(0,.7,0));
        display.setItemStack(new ItemStack(ArtifactItems.ITEMS.get(Artifact.TOTEM_OF_SHIELDING)));
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.addTag("dungeons_artifacts_shield");
        var c = DungeonsArtifacts.CONFIG;
        removeOwner(p.getUUID());
        SHIELDS.put(display.getUUID(), new Shield(p.getUUID(), display, p.level().getServer().overworld().getGameTime()+c.shieldDurationTicks, c.shieldRadius));
        if (!p.level().addFreshEntity(display)) { SHIELDS.remove(display.getUUID()); return false; }
        ArtifactServer.sound(p, SoundEvents.BEACON_ACTIVATE, 1);
        return true;
    }
    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        var iterator = SHIELDS.values().iterator();
        while (iterator.hasNext()) {
            Shield s = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(s.owner);
            if (now >= s.expires || s.display.isRemoved() || owner == null || owner.level() != s.display.level()) {
                s.display.discard(); iterator.remove(); continue;
            }
            Vec3 center = s.display.position();
            if (now%5 == 0) {
                ArtifactServer.ring(owner.level(), center, s.radius, ParticleTypes.ELECTRIC_SPARK);
                ArtifactServer.ring(owner.level(), center.add(0, s.radius*.65,0), s.radius*.75, ParticleTypes.END_ROD);
            }
            // Sweep every loaded projectile before entities tick: fast projectiles cannot tunnel through.
            for (Entity e : owner.level().getAllEntities()) if (e instanceof Projectile projectile && !e.isRemoved()) {
                Entity shooter = projectile.getOwner();
                if (shooter instanceof LivingEntity living && ArtifactServer.allied(owner,living)) continue;
                Vec3 start = e.position().subtract(center), delta = e.getDeltaMovement();
                if (start.lengthSqr() <= s.radius*s.radius && start.dot(delta) >= 0) continue;
                double t = delta.lengthSqr() < 1e-12 ? 0 : Math.clamp(-start.dot(delta)/delta.lengthSqr(),0,1);
                if (start.add(delta.scale(t)).lengthSqr() <= s.radius*s.radius) {
                    ArtifactServer.particles(owner.level(), ParticleTypes.ELECTRIC_SPARK, e.position().add(delta.scale(t)), 8);
                    e.discard();
                }
            }
        }
    }
    public static void removeOwner(UUID owner) { SHIELDS.values().removeIf(s -> { if (s.owner.equals(owner)) {s.display.discard(); return true;} return false; }); }
    public static void clear() { SHIELDS.values().forEach(s -> s.display.discard()); SHIELDS.clear(); }
}

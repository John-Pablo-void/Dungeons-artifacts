package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import com.google.gson.Gson;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class GhostMovement {
    private static final Gson GSON = new Gson();
    private record Anchor(String dimension, double x, double y, double z) { }
    public static boolean ghost(Entity entity) { return entity instanceof LivingEntity living && living.hasEffect(ArtifactEffects.GHOST); }
    public static boolean begin(ServerPlayer p) {
        if (p.isPassenger() || p.isSleeping() || ghost(p) || !p.level().noCollision(p)) return false;
        remember(p);
        ArtifactServer.effect(p, ArtifactEffects.GHOST, 200, 0);
        ArtifactServer.sound(p, SoundEvents.SOUL_ESCAPE.value(), 1);
        return true;
    }
    private static void remember(ServerPlayer p) {
        p.setAttached(PlayerData.GHOST_ANCHOR, GSON.toJson(new Anchor(p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ())));
    }
    public static void tick(ServerPlayer p) {
        if (ghost(p)) {
            if (p.isPassenger() || p.isSleeping()) { end(p, true); return; }
            if (p.onGround() && p.level().noCollision(p)) remember(p);
        } else if (!p.getAttachedOrCreate(PlayerData.GHOST_ANCHOR).isEmpty()) end(p, true);
    }
    /** Ignore horizontal block shapes and entities, retain vertical block/world-border collision. */
    public static Vec3 clip(Entity entity, AABB box, Vec3 requested) {
        var level = entity.level();
        double x = requested.x, z = requested.z;
        AABB moved = box.move(x, 0, z);
        if (!level.getWorldBorder().isWithinBounds(moved)) { x=0; z=0; moved=box; }
        double y = requested.y;
        List<VoxelShape> shapes = new ArrayList<>();
        level.getBlockCollisions(entity, box.expandTowards(x,y,z).inflate(1e-7)).forEach(shapes::add);
        y = Shapes.collide(Direction.Axis.Y, box, shapes, y);
        y = Shapes.collide(Direction.Axis.Y, moved, shapes, y);
        return new Vec3(x,y,z);
    }
    public static void end(ServerPlayer p, boolean safeExit) {
        String saved = p.getAttachedOrCreate(PlayerData.GHOST_ANCHOR);
        p.removeEffect(ArtifactEffects.GHOST);
        if (safeExit && p.isAlive() && !p.level().noCollision(p)) {
            Vec3 destination = findFree(p, p.position(), 8);
            if (destination == null && !saved.isEmpty()) {
                Anchor a = GSON.fromJson(saved, Anchor.class);
                if (a.dimension.equals(p.level().dimension().identifier().toString())) {
                    Vec3 pos = new Vec3(a.x,a.y,a.z);
                    p.level().getChunk(BlockPos.containing(pos));
                    destination = findFree(p, pos, 8);
                }
            }
            if (destination == null) {
                // Last resort: an empty supported spot above terrain, never spectator/noPhysics.
                var pos = p.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition());
                destination = findFree(p, Vec3.atBottomCenterOf(pos), 16);
            }
            if (destination == null) {
                // Keep the safety anchor and collision effect until a safe exit exists.
                ArtifactServer.effect(p, ArtifactEffects.GHOST, 20, 0);
                return;
            }
            p.teleportTo(p.level(), destination.x, destination.y, destination.z, Set.of(), p.getYRot(), p.getXRot(), false);
            p.fallDistance = 0;
        }
        p.setAttached(PlayerData.GHOST_ANCHOR, "");
    }
    private static Vec3 findFree(ServerPlayer p, Vec3 center, int radius) {
        for (int r=0;r<=radius;r++) for (int dy=-r;dy<=r;dy++) for (int dx=-r;dx<=r;dx++) for (int dz=-r;dz<=r;dz++) {
            if (Math.max(Math.abs(dy), Math.max(Math.abs(dx),Math.abs(dz))) != r) continue;
            Vec3 pos = Vec3.atBottomCenterOf(BlockPos.containing(center).offset(dx,dy,dz));
            AABB box = p.getBoundingBox().move(pos.subtract(p.position()));
            if (p.level().getWorldBorder().isWithinBounds(box) && p.level().noCollision(p, box)
                && !p.level().containsAnyLiquid(box) && !p.level().getBlockState(BlockPos.containing(pos).below()).getCollisionShape(p.level(), BlockPos.containing(pos).below()).isEmpty()) return pos;
        }
        return null;
    }
}

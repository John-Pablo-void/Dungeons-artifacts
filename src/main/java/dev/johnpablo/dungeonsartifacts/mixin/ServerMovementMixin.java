package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerMovementMixin {
    @Inject(method="isEntityCollidingWithAnythingNew",at=@At("HEAD"),cancellable=true)
    private void allowHorizontalGhost(LevelReader level, Entity e, AABB old, double x,double y,double z, CallbackInfoReturnable<Boolean> cir) {
        if(GhostMovement.ghost(e)) {
            Vec3 desired=new Vec3(x-(old.minX+old.maxX)/2,y-old.minY,z-(old.minZ+old.maxZ)/2);
            Vec3 allowed=GhostMovement.clip(e,old,desired);
            cir.setReturnValue(Math.abs(allowed.y-desired.y)>1e-6 || Math.abs(allowed.x-desired.x)>1e-6 || Math.abs(allowed.z-desired.z)>1e-6);
        }
    }
}

package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method="spawnSprintParticle",at=@At("HEAD"),cancellable=true)
    private void hideGhostSprint(CallbackInfo ci) { if(GhostMovement.ghost((Entity)(Object)this)) ci.cancel(); }
    @Inject(method="collide",at=@At("HEAD"),cancellable=true)
    private void ghostCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity e=(Entity)(Object)this;
        if (GhostMovement.ghost(e)) cir.setReturnValue(GhostMovement.clip(e,e.getBoundingBox(),movement));
    }
    @ModifyVariable(method="move",at=@At("HEAD"),argsOnly=true)
    private Vec3 restrictMovement(Vec3 movement) {
        if ((Object)this instanceof LivingEntity living) {
            if (living.hasEffect(ArtifactEffects.ROOTED)) return new Vec3(0,Math.min(0,movement.y),0);
            if (living.hasEffect(ArtifactEffects.STUNNED)) return new Vec3(0,movement.y,0);
        }
        return movement;
    }
    @Inject(method="isInWall",at=@At("HEAD"),cancellable=true)
    private void ghostSuffocation(CallbackInfoReturnable<Boolean> cir) { if (GhostMovement.ghost((Entity)(Object)this)) cir.setReturnValue(false); }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void ghostCollision(Entity other,CallbackInfoReturnable<Boolean> cir) { if(GhostMovement.ghost((Entity)(Object)this)||GhostMovement.ghost(other)) cir.setReturnValue(false); }
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void ghostPush(Entity other,CallbackInfo ci) { if(GhostMovement.ghost((Entity)(Object)this)||GhostMovement.ghost(other)) ci.cancel(); }
}

package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
abstract class MobMixin {
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity ignoreGhost(LivingEntity target) { return target!=null&&GhostMovement.ghost(target)?null:target; }
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void restrictAi(CallbackInfo ci) {
        Mob mob=(Mob)(Object)this;
        if(mob.getTarget()!=null&&GhostMovement.ghost(mob.getTarget())) mob.setTarget(null);
        if(mob.hasEffect(ArtifactEffects.STUNNED)) { mob.getNavigation().stop(); mob.setTarget(null); ci.cancel(); }
        else if(mob.hasEffect(ArtifactEffects.ROOTED)) mob.getNavigation().stop();
    }
}

package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void corruptedPoison(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        // Corrupted vines make even undead targets susceptible during application.
        if(effect.is(MobEffects.POISON)&&((LivingEntity)(Object)this).hasEffect(ArtifactEffects.ROOTED)) cir.setReturnValue(true);
    }
    @Shadow protected abstract void removeEffectParticles();
    @Inject(method="updateSynchronizedMobEffectParticles",at=@At("TAIL"))
    private void hideGhostParticles(CallbackInfo ci) { if(GhostMovement.ghost((LivingEntity)(Object)this)) removeEffectParticles(); }
    @Inject(method={"canBeSeenByAnyone","canBeSeenAsEnemy","isPushable"},at=@At("HEAD"),cancellable=true)
    private void ghostHidden(CallbackInfoReturnable<Boolean> cir) { if(GhostMovement.ghost((LivingEntity)(Object)this)) cir.setReturnValue(false); }
}

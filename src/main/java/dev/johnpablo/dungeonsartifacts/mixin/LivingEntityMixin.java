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
    @Inject(method="tick", at=@At("TAIL"))
    private void updraftTick(CallbackInfo ci) {
        LivingEntity e=(LivingEntity)(Object)this;
        if(e.level().isClientSide()) return;
        int phase=e.getAttachedOrCreate(dev.johnpablo.dungeonsartifacts.soul.PlayerData.UPDRAFT);
        if(phase==1 && !e.hasEffect(MobEffects.LEVITATION)) {
            e.addEffect(new MobEffectInstance(ArtifactEffects.STUNNED,90,0));
            e.setAttached(dev.johnpablo.dungeonsartifacts.soul.PlayerData.UPDRAFT,2);
        } else if(phase==2 && e.onGround()) e.setAttached(dev.johnpablo.dungeonsartifacts.soul.PlayerData.UPDRAFT,0);
    }
    @Inject(method="calculateFallDamage", at=@At("RETURN"), cancellable=true)
    private void updraftFall(double distance,float multiplier,CallbackInfoReturnable<Integer> cir) {
        LivingEntity e=(LivingEntity)(Object)this;
        if(e.getAttachedOrCreate(dev.johnpablo.dungeonsartifacts.soul.PlayerData.UPDRAFT)>0) {
            cir.setReturnValue(cir.getReturnValue()*3);
            if(!e.level().isClientSide()) e.setAttached(dev.johnpablo.dungeonsartifacts.soul.PlayerData.UPDRAFT,0);
        }
    }
    @Shadow protected abstract void removeEffectParticles();
    @Inject(method="updateSynchronizedMobEffectParticles",at=@At("TAIL"))
    private void hideGhostParticles(CallbackInfo ci) { if(GhostMovement.ghost((LivingEntity)(Object)this)) removeEffectParticles(); }
    @Inject(method={"canBeSeenByAnyone","canBeSeenAsEnemy","isPushable"},at=@At("HEAD"),cancellable=true)
    private void ghostHidden(CallbackInfoReturnable<Boolean> cir) { if(GhostMovement.ghost((LivingEntity)(Object)this)) cir.setReturnValue(false); }
}

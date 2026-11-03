package dev.johnpablo.dungeonsartifacts.client.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.johnpablo.dungeonsartifacts.client.GhostRenderState;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntityRenderer.class)
abstract class LivingRendererMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void extractGhost(LivingEntity e, LivingEntityRenderState state,float partial,CallbackInfo ci) { ((GhostRenderState)state).dungeonsArtifacts$setGhost(GhostMovement.ghost(e)); }
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"),cancellable=true)
    private void hideGhost(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) { if(((GhostRenderState)state).dungeonsArtifacts$isGhost()) ci.cancel(); }
}

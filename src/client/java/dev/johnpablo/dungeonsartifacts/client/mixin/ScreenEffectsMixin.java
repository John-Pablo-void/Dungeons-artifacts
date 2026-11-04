package dev.johnpablo.dungeonsartifacts.client.mixin;

import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectsMixin {
    @Inject(method="getViewBlockingState",at=@At("HEAD"),cancellable=true)
    private static void replaceOverlay(Player player,CallbackInfoReturnable<BlockState> cir) {
        if(GhostMovement.ghost(player))cir.setReturnValue(null);
    }
    @Inject(method="submit",at=@At("HEAD"))
    private void localSurfaces(boolean firstPerson,boolean sleeping,float partial,SubmitNodeCollector collector,boolean hideGui,CallbackInfo ci) {
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!firstPerson||sleeping||!GhostMovement.ghost(p))return;
        BlockPos eye=BlockPos.containing(p.getEyePosition());
        var inside=p.level().getBlockState(eye);
        if(!inside.isViewBlocking(p.level(),eye))return;
        BlockPos floor=BlockPos.containing(p.getX(),p.getY()-.01,p.getZ());
        BlockPos ceiling=BlockPos.containing(p.getX(),Math.ceil(p.getBoundingBox().maxY),p.getZ());
        // Inward-facing opaque surfaces enclose the camera while phasing. Only the
        // immediate floor/ceiling textures are sampled, so culled caves never show.
        for(int face=0;face<6;face++) {
            var state=face==4?p.level().getBlockState(floor):face==5?p.level().getBlockState(ceiling):inside;
            if(state.isAir())state=inside;
            var sprite=mc.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
            PoseStack pose=new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(p.getXRot()));
            pose.mulPose(Axis.YP.rotationDegrees(p.getYRot()+180));
            if(face<4)pose.mulPose(Axis.YP.rotationDegrees(face*90));
            else pose.mulPose(Axis.XP.rotationDegrees(face==4?-90:90));
            collector.submitCustomGeometry(pose,RenderTypes.blockScreenEffect(sprite.atlasLocation()),(transform,builder) -> {
                var matrix=transform.pose();
                builder.addVertex(matrix,-1,-1,-1).setUv(sprite.getU0(),sprite.getV1()).setColor(0xffa0a0a0);
                builder.addVertex(matrix,1,-1,-1).setUv(sprite.getU1(),sprite.getV1()).setColor(0xffa0a0a0);
                builder.addVertex(matrix,1,1,-1).setUv(sprite.getU1(),sprite.getV0()).setColor(0xffa0a0a0);
                builder.addVertex(matrix,-1,1,-1).setUv(sprite.getU0(),sprite.getV0()).setColor(0xffa0a0a0);
            });
        }
    }
}

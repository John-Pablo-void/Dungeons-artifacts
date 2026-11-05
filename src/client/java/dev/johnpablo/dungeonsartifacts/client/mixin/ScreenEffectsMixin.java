package dev.johnpablo.dungeonsartifacts.client.mixin;

import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import dev.johnpablo.dungeonsartifacts.client.GhostWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
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
        if(p!=null&&firstPerson&&!sleeping&&GhostMovement.ghost(p))GhostWindow.submit(mc,partial,collector);
    }
}

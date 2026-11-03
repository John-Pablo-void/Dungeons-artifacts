package dev.johnpablo.dungeonsartifacts.client.mixin;

import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectsMixin {
    @Inject(method="getViewBlockingState", at=@At("HEAD"), cancellable=true)
    private static void seeWhilePhasing(Player player, CallbackInfoReturnable<BlockState> result) {
        if (GhostMovement.ghost(player)) result.setReturnValue(null);
    }
}

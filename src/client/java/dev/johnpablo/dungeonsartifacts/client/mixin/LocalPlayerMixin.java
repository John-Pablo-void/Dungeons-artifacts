package dev.johnpablo.dungeonsartifacts.client.mixin;

import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method="moveTowardsClosestSpace", at=@At("HEAD"), cancellable=true)
    private void doNotPushGhostOutOfWalls(double x, double z, CallbackInfo result) {
        if (GhostMovement.ghost((LocalPlayer)(Object)this)) result.cancel();
    }
}

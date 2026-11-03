package dev.johnpablo.dungeonsartifacts.client.mixin;
import dev.johnpablo.dungeonsartifacts.game.GhostMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemInHandRenderer.class)
abstract class HandsMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void hideGhostHands(CallbackInfo ci) { if(Minecraft.getInstance().player!=null&&GhostMovement.ghost(Minecraft.getInstance().player)) ci.cancel(); }
}

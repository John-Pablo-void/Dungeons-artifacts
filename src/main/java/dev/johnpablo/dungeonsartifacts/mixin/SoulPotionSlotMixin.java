package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.item.SoulItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
abstract class SoulPotionSlotMixin {
    @Inject(method="mayPlaceItem",at=@At("HEAD"),cancellable=true)
    private static void bottle(ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(stack.is(SoulItems.FULL_BOTTLE)||stack.is(SoulItems.LIQUID))cir.setReturnValue(true);}
}

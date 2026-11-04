package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.item.SoulItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BrewingStandBlockEntity.class)
abstract class SoulBrewingStandMixin {
    @Inject(method="canPlaceItem",at=@At("HEAD"),cancellable=true)
    private void bottle(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(slot>=0&&slot<3&&stack.is(SoulItems.FULL_BOTTLE))cir.setReturnValue(((BrewingStandBlockEntity)(Object)this).getItem(slot).isEmpty());}
}

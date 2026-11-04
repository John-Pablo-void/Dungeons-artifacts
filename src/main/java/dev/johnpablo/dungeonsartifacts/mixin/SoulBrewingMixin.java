package dev.johnpablo.dungeonsartifacts.mixin;
import dev.johnpablo.dungeonsartifacts.item.SoulItems;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PotionBrewing.class)
abstract class SoulBrewingMixin {
    @Inject(method="isIngredient",at=@At("HEAD"),cancellable=true)
    private void ice(ItemStack ingredient,CallbackInfoReturnable<Boolean> cir){if(ingredient.is(Items.ICE))cir.setReturnValue(true);}
    @Inject(method="hasMix",at=@At("HEAD"),cancellable=true)
    private void recipe(ItemStack source,ItemStack ingredient,CallbackInfoReturnable<Boolean> cir){if(source.is(SoulItems.FULL_BOTTLE)&&ingredient.is(Items.ICE))cir.setReturnValue(true);}
    @Inject(method="mix",at=@At("HEAD"),cancellable=true)
    private void brew(ItemStack ingredient,ItemStack source,CallbackInfoReturnable<ItemStack> cir){if(source.is(SoulItems.FULL_BOTTLE)&&ingredient.is(Items.ICE))cir.setReturnValue(new ItemStack(SoulItems.LIQUID));}
}

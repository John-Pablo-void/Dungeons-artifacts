package dev.johnpablo.dungeonsartifacts.item;

import java.util.*;
import java.util.function.Consumer;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.game.ArtifactServer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

public final class SoulItems {
    public static Item BOTTLE, FULL_BOTTLE, LIQUID, COMPASS;
    private static Item.Properties props(String name) {return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,DungeonsArtifacts.id(name))).stacksTo(1);}
    private static Item register(String name,Item item) {return Registry.register(BuiltInRegistries.ITEM,DungeonsArtifacts.id(name),item);}
    public static void initialize() {
        BOTTLE=register("souls_in_a_bottle",new BottleItem(props("souls_in_a_bottle")));
        FULL_BOTTLE=register("full_soul_bottle",new BottleItem(props("full_soul_bottle")));
        LIQUID=register("liquid_souls",new Item(props("liquid_souls").component(DataComponents.CONSUMABLE,Consumables.DEFAULT_DRINK).usingConvertsTo(Items.GLASS_BOTTLE)) {
            @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
                if(entity instanceof ServerPlayer p) {var state=ArtifactServer.ledger(p);state.collect(25);ArtifactServer.save(p,state);}
                return super.finishUsingItem(stack,level,entity);
            }
        });
        COMPASS=register("attuned_compass",new CompassItem(props("attuned_compass")));
    }
    public static int stored(ItemStack stack) {
        return stack.is(FULL_BOTTLE)?200:Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("soul_quarters",0),0,199);
    }
    public static ItemStack bottle(int quarters) {
        if(quarters>=200) return new ItemStack(FULL_BOTTLE);
        ItemStack stack=new ItemStack(BOTTLE);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> tag.putInt("soul_quarters",Math.max(0,quarters)));
        return stack;
    }
    public static boolean fillOffhand(ServerPlayer p,int quarters) {
        var held=p.getOffhandItem();
        if(!held.is(Items.GLASS_BOTTLE)&&!held.is(BOTTLE)) return false;
        int amount=held.is(BOTTLE)?stored(held):0;
        if(held.is(Items.GLASS_BOTTLE)&&held.getCount()>1) {
            var remaining=held.copyWithCount(held.getCount()-1);
            if(!p.getInventory().add(remaining)) p.drop(remaining,false);
        }
        p.setItemInHand(InteractionHand.OFF_HAND,bottle(amount+quarters));
        return true;
    }
    private static final class BottleItem extends Item {
        BottleItem(Properties properties){super(properties);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag) {
            text.accept(Component.translatable("tooltip.dungeons_artifacts.bottle",stored(stack)/4));
        }
    }
}

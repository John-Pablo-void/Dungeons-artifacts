package dev.johnpablo.dungeonsartifacts.game;

import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.PlayerLedger;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import dev.johnpablo.dungeonsartifacts.item.SoulItems;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Quarter-souls retain both the 0.5 base rate and the 1.5x enchantment exactly. */
public final class SoulGathering {
    public static int rewardQuarters(int artifacts,int attunement,boolean vacuum) {
        int base=artifacts==0?2:Math.min(3,artifacts)*4;
        return (base+Math.max(0,attunement)*4)*(vacuum?3:2)/2;
    }
    public static int reward(ServerPlayer p) {return reward(p,p.getMainHandItem());}
    public static int reward(ServerPlayer p,net.minecraft.world.item.ItemStack weapon) {
        int artifacts=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++) {
            var stack=p.getInventory().getItem(i);
            if(stack.getItem() instanceof ArtifactItems.ArtifactItem item && item.artifact.soulCost>0) artifacts+=stack.getCount();
        }
        var enchant=p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,DungeonsArtifacts.id("soul_vacuum")));
        boolean vacuum=enchant.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(enchant.get(),weapon)>0;
        return rewardQuarters(artifacts,p.getAttachedOrCreate(PlayerData.ATTUNEMENT),vacuum);
    }
    public static int collectKill(ServerPlayer p,PlayerLedger state,net.minecraft.world.damagesource.DamageSource source) {
        var weapon=source.getWeaponItem();
        int reward=reward(p,weapon==null?p.getMainHandItem():weapon);
        // Bottling captures this kill's souls instead of duplicating them into the meter.
        if(SoulItems.fillOffhand(p,reward)) return reward;
        int quarters=p.getAttachedOrCreate(PlayerData.FRACTION)+reward;
        int gained=state.collect(quarters/4);
        p.setAttached(PlayerData.FRACTION,state.souls()==state.capacity()?0:quarters%4);
        return gained;
    }
}

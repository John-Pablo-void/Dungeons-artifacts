package dev.johnpablo.dungeonsartifacts.client;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.Locale;
public final class ArtifactsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, DungeonsArtifacts.id("souls"), (g,delta) -> {
            var mc=Minecraft.getInstance();
            if(mc.player==null||mc.player.isSpectator()) return;
            int souls=mc.player.getAttachedOrCreate(PlayerData.SOULS), cap=mc.player.getAttachedOrCreate(PlayerData.CAPACITY);
            if(cap<=0) return;
            int x=8,y=8, color=mc.player.getAttachedOrCreate(PlayerData.DENIED)>0?0xffff7070:0xff80eddf;
            g.fill(x-2,y-2,x+126,y+25,0xb0000000);
            g.text(mc.font,Component.translatable("hud.dungeons_artifacts.souls",souls,cap),x,y,color);
            g.fill(x,y+13,x+120,y+19,0xff263b45);
            g.fill(x,y+13,x+(int)(120L*souls/cap),y+19,color);
            int active=mc.player.getAttachedOrCreate(PlayerData.BOOTS_TIME), cooldown=mc.player.getAttachedOrCreate(PlayerData.BOOTS_COOLDOWN);
            if(active>0||cooldown>0) g.text(mc.font,Component.translatable(active>0?"hud.dungeons_artifacts.boots":"hud.dungeons_artifacts.cooldown",String.format(Locale.ROOT,"%.3f",(active>0?active:cooldown)/40.0)),x,y+29,0xffd9efff);
        });
    }
}

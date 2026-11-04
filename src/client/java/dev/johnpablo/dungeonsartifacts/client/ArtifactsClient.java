package dev.johnpablo.dungeonsartifacts.client;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
public final class ArtifactsClient implements ClientModInitializer {
    public static boolean showSouls;
    public static KeyMapping toggle;
    @Override public void onInitializeClient() {
        toggle=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dungeons_artifacts.soul_meter",GLFW.GLFW_KEY_J,KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {while(toggle.consumeClick())showSouls=!showSouls;});
        HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR,original -> (g,delta) -> {
            var mc=Minecraft.getInstance();
            if(!showSouls||mc.player==null||mc.player.isSpectator()||mc.player.isPassenger()) {original.extractRenderState(g,delta);return;}
            int souls=mc.player.getAttachedOrCreate(PlayerData.SOULS), cap=mc.player.getAttachedOrCreate(PlayerData.CAPACITY);
            if(cap<=0)return;
            int x=g.guiWidth()/2-91,y=g.guiHeight()-29,color=mc.player.getAttachedOrCreate(PlayerData.DENIED)>0?0xffff7070:0xff80eddf;
            double total=souls+mc.player.getAttachedOrCreate(PlayerData.FRACTION)/4.0;
            g.fill(x,y,x+182,y+5,0xff263b45);
            g.fill(x,y,x+(int)(182*total/cap),y+5,color);
            String amount=total==(int)total?Integer.toString((int)total):Double.toString(total);
            var label=Component.translatable("hud.dungeons_artifacts.souls",amount,cap);
            g.pose().pushMatrix();
            g.pose().translate(g.guiWidth()/2f,y-1);
            g.pose().scale(.65f,.65f);
            g.text(mc.font,label,-mc.font.width(label)/2,0,0xffffffff);
            g.pose().popMatrix();
        });
        HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL,original -> (g,delta) -> {
            if(!showSouls||Minecraft.getInstance().player==null||Minecraft.getInstance().player.isPassenger())original.extractRenderState(g,delta);
        });
    }
}

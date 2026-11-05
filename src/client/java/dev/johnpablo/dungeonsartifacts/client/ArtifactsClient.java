package dev.johnpablo.dungeonsartifacts.client;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import net.minecraft.client.renderer.RenderPipelines;
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
            int x=g.guiWidth()/2-91,y=g.guiHeight()-29;
            double total=souls+mc.player.getAttachedOrCreate(PlayerData.FRACTION)/4.0;
            g.blitSprite(RenderPipelines.GUI_TEXTURED,DungeonsArtifacts.id("hud/soul_bar_background"),x,y,182,5);
            int fill=cap>0?(int)(182*total/cap):0;
            if(fill>0)g.blitSprite(RenderPipelines.GUI_TEXTURED,DungeonsArtifacts.id("hud/soul_bar_progress"),182,5,0,0,x,y,fill,5);
            // Stored fractions affect progress; the displayed number is always floored.
            var label=Component.literal(Integer.toString(souls));
            g.text(mc.font,label,g.guiWidth()/2-mc.font.width(label)/2,y-6,
                mc.player.getAttachedOrCreate(PlayerData.DENIED)>0?0xffff7070:0xff80ffff);

        });
        HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL,original -> (g,delta) -> {
            if(!showSouls||Minecraft.getInstance().player==null||Minecraft.getInstance().player.isPassenger())original.extractRenderState(g,delta);
        });
    }
}

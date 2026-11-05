package dev.johnpablo.dungeonsartifacts;

import dev.johnpablo.dungeonsartifacts.config.ArtifactConfig;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import dev.johnpablo.dungeonsartifacts.game.ArtifactServer;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class DungeonsArtifacts implements ModInitializer {
    public static final String ID = "dungeons_artifacts";
    public static ArtifactConfig CONFIG;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    @Override public void onInitialize() {
        CONFIG = ArtifactConfig.load();
        ArtifactEffects.initialize();
        ArtifactItems.initialize();
        // Register attachments before Fabric's configuration handshake advertises supported IDs.
        org.slf4j.LoggerFactory.getLogger(ID).info("Registered player state {}", PlayerData.STATE.identifier());
        dev.johnpablo.dungeonsartifacts.game.AttunementSystem.SITES.identifier();
        dev.johnpablo.dungeonsartifacts.game.SoulEchoes.ECHOES.identifier();
        ArtifactServer.initialize();
    }
}

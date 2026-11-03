package dev.johnpablo.dungeonsartifacts.config;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArtifactConfig {
    public int soulCapacity = 50;
    public int soulsPerHostileKill = 2;
    public int vesselCapacityPerLevel = 10;
    public int reservoirCapacityPerLevel = 15;
    public double shieldRadius = 4;
    public int shieldDurationTicks = 200;
    public int shieldCooldownTicks = 600;
    public double seedsRadius = 6;
    public double elementsRadius = 8;
    public float elementalLightningDamage = 6;
    public int elementalFireSeconds = 5;
    public int elementalWindTicks = 60;
    public double lightningRange = 32;
    public double lightningRadius = 3;
    public boolean hostilePlayers = false;

    public static ArtifactConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("dungeons_artifacts.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            ArtifactConfig c = Files.exists(path) ? gson.fromJson(Files.readString(path), ArtifactConfig.class) : new ArtifactConfig();
            if (c == null) throw new IllegalArgumentException("Empty configuration");
            c.validate();
            if (!Files.exists(path)) Files.writeString(path, gson.toJson(c) + "\n");
            return c;
        } catch (Exception e) { throw new IllegalStateException("Cannot load " + path + "; fix the configuration rather than silently losing it", e); }
    }

    private void validate() {
        if (soulCapacity < 1 || soulCapacity > 100000 || soulsPerHostileKill < 0 || soulsPerHostileKill > 1000
            || vesselCapacityPerLevel < 0 || vesselCapacityPerLevel > 1000 || reservoirCapacityPerLevel < 0 || reservoirCapacityPerLevel > 1000
            || shieldDurationTicks < 1 || shieldDurationTicks > 12000 || shieldCooldownTicks < 0 || shieldCooldownTicks > 72000
            || elementalFireSeconds < 1 || elementalFireSeconds > 60 || elementalWindTicks < 1 || elementalWindTicks > 1200
            || !Float.isFinite(elementalLightningDamage) || elementalLightningDamage < 0 || elementalLightningDamage > 100)
            throw new IllegalArgumentException("Artifact configuration outside supported bounds");
        for (double r : new double[]{shieldRadius, seedsRadius, elementsRadius, lightningRange, lightningRadius})
            if (!Double.isFinite(r) || r < 1 || r > 64) throw new IllegalArgumentException("Radius/range must be 1..64");
    }
}

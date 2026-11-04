package dev.johnpablo.dungeonsartifacts.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.Artifact;
import dev.johnpablo.dungeonsartifacts.core.PlayerLedger;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.Map;

public final class PlayerData {
    private static final Codec<PlayerLedger.Snapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("souls").forGetter(PlayerLedger.Snapshot::souls),
        Codec.unboundedMap(Codec.STRING.xmap(Artifact::valueOf, Artifact::name), Codec.LONG).fieldOf("cooldowns").forGetter(PlayerLedger.Snapshot::cooldowns),
        Codec.LONG.fieldOf("boots_started").forGetter(PlayerLedger.Snapshot::bootsStarted)
    ).apply(i, PlayerLedger.Snapshot::new));
    public static final AttachmentType<PlayerLedger.Snapshot> STATE = AttachmentRegistry.create(DungeonsArtifacts.id("state"),
        b -> b.initializer(() -> new PlayerLedger.Snapshot(0, Map.of(), -1)).persistent(CODEC).copyOnDeath());
    public static final AttachmentType<Integer> FRACTION = AttachmentRegistry.create(DungeonsArtifacts.id("soul_fraction"), b -> b.initializer(() -> 0).persistent(Codec.INT).copyOnDeath().syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));
    public static final AttachmentType<Integer> ATTUNEMENT = AttachmentRegistry.create(DungeonsArtifacts.id("attunement"), b -> b.initializer(() -> 0).persistent(Codec.INT).copyOnDeath());
    public static final AttachmentType<Integer> UPDRAFT = AttachmentRegistry.create(DungeonsArtifacts.id("updraft"), b -> b.initializer(() -> 0).persistent(Codec.INT));
    public static final AttachmentType<Integer> SOULS = synced("souls");
    public static final AttachmentType<Integer> CAPACITY = synced("capacity");
    public static final AttachmentType<Integer> BOOTS_TIME = synced("boots_time");
    public static final AttachmentType<Integer> BOOTS_COOLDOWN = synced("boots_cooldown");
    public static final AttachmentType<Integer> DENIED = synced("denied");
    public static final AttachmentType<String> GHOST_ANCHOR = AttachmentRegistry.create(DungeonsArtifacts.id("ghost_anchor"), b -> b.initializer(() -> "").persistent(Codec.STRING));
    private static AttachmentType<Integer> synced(String name) {
        return AttachmentRegistry.create(DungeonsArtifacts.id(name), b -> b.initializer(() -> 0).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));
    }
}

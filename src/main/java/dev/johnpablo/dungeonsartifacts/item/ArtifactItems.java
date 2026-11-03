package dev.johnpablo.dungeonsartifacts.item;

import java.util.EnumMap;
import java.util.function.Consumer;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.Artifact;
import dev.johnpablo.dungeonsartifacts.game.ArtifactServer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ArtifactItems {
    public static final EnumMap<Artifact, Item> ITEMS = new EnumMap<>(Artifact.class);
    public static void initialize() {
        for (Artifact artifact : Artifact.values()) {
            var key = ResourceKey.create(Registries.ITEM, DungeonsArtifacts.id(artifact.id));
            Item item = new ArtifactItem(artifact, new Item.Properties().setId(key)
                .stacksTo(artifact == Artifact.DEATH_CAP_MUSHROOM ? 16 : 1).rarity(Rarity.RARE).enchantable(10));
            ITEMS.put(artifact, Registry.register(BuiltInRegistries.ITEM, key, item));
        }
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DungeonsArtifacts.id("artifacts"), FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dungeons_artifacts"))
            .icon(() -> new ItemStack(ITEMS.get(Artifact.SOUL_HEALER)))
            .displayItems((parameters, output) -> ITEMS.values().forEach(output::accept)).build());
    }
    public static final class ArtifactItem extends Item {
        public final Artifact artifact;
        ArtifactItem(Artifact artifact, Properties properties) { super(properties); this.artifact = artifact; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) return ArtifactServer.use(serverPlayer, hand, artifact);
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> text, TooltipFlag flag) {
            text.accept(Component.translatable("tooltip.dungeons_artifacts." + artifact.id));
            if (artifact.soulCost > 0) text.accept(Component.translatable("tooltip.dungeons_artifacts.cost", artifact.soulCost));
        }
    }
}

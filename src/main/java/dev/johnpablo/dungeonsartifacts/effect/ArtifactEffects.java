package dev.johnpablo.dungeonsartifacts.effect;

import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ArtifactEffects {
    public static final Holder<MobEffect> GHOST = register("ghost", MobEffectCategory.BENEFICIAL, 0x92e9e0);
    public static final Holder<MobEffect> ROOTED = register("rooted", MobEffectCategory.HARMFUL, 0x694080);
    public static final Holder<MobEffect> STUNNED = register("stunned", MobEffectCategory.HARMFUL, 0xc9eaff);
    private static Holder<MobEffect> register(String id, MobEffectCategory category, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DungeonsArtifacts.id(id), new MobEffect(category, color) {});
    }
    public static void initialize() { }
}

package dev.johnpablo.dungeonsartifacts.core;

/** Shared identifiers and specified costs. Durations are in server ticks (20/s). */
public enum Artifact {
    SOUL_HEALER("soul_healer", 5, 100),
    SATCHEL_OF_ELIXIRS("satchel_of_elixirs", 0, 600),
    TOTEM_OF_SHIELDING("totem_of_shielding", 0, 600),
    LIGHTNING_ROD("lightning_rod", 2, 0),
    CORRUPTED_SEEDS("corrupted_seeds", 0, 500),
    GONG_OF_WEAKENING("gong_of_weakening", 0, 900),
    SATCHEL_OF_ELEMENTS("satchel_of_elements", 0, 300),
    DEATH_CAP_MUSHROOM("death_cap_mushroom", 0, 200),
    GHOST_CLOAK("ghost_cloak", 15, 600),
    UPDRAFT_TOME("updraft_tome", 0, 400),
    BOOTS_OF_LEVITATION("boots_of_levitation", 0, 0);

    public final String id;
    public final int soulCost;
    public final int cooldownTicks;

    Artifact(String id, int soulCost, int cooldownTicks) {
        this.id = id;
        this.soulCost = soulCost;
        this.cooldownTicks = cooldownTicks;
    }
}

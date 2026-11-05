# Dungeons Artifacts — Fabric / Minecraft 26.2

Eleven server-controlled artifacts, recoverable soul echoes, a toggleable soul meter, three
enchantments, framed shrine artifacts, and eight soul attunement altars per world.
Minecraft **26.2** and **Java 25** are required. This mod is required on both the
server and participating clients.

## Installation

Install Fabric Loader **0.20.1 or newer** for Minecraft **26.2**. Place these files
in the instance's `mods` folder (and the dedicated server's `mods` folder):

- `dungeons-artifacts-0.3.0-dev.jar`
- Fabric API **0.161.0+26.2** or a compatible newer release for 26.2.

The `-sources.jar` is for development; it is not the playable mod.
No additional mod library is required. Punchy is optional; see compatibility below.

## Playing

All artifacts appear in the **Dungeons Artifacts** creative tab. Right-click with
an artifact in either hand. Starting soul capacity is **50**. Credited mob kills grant
**0.5 souls** without soul artifacts, **1 / 2 / 3 souls** with **1 / 2 / 3+**
soul artifacts anywhere in the inventory (including the offhand). Soul artifacts
are Soul Healer, Lightning Rod and Ghost Cloak; duplicate items count. Pet kills
credited to their owner also count. Recent player hits retain vanilla kill credit
when a mob subsequently dies from falling, fire or another cause. Passive mobs
also count; allied mobs and players do not. Soul particles rise from the killed
mob. Fractional souls are retained across saves.

Press **J** (rebindable in Controls) to replace the experience bar and level with
a cyan XP-style soul bar; press it again to restore XP. Only your current whole
soul count is displayed, rounded down. Fractional amounts still count normally
and contribute to the progress bar. Mounted jump bars remain visible.
Failed activations show a reason and do not consume souls. Boots have no HUD timer.

| Artifact | Action | Souls | Cooldown |
| --- | --- | ---: | --- |
| Soul Healer | Instant Health II (8 HP) to allies within 10 blocks, including healing undead allies safely | 5 | 5 s |
| Satchel of Elixirs | Two distinct random positive effects for 15 s; sneak + use throws a real negative splash potion | 0 | 30 s |
| Totem of Shielding | Aim at ground within 5 blocks; deploy a visible 4-block projectile shield for 10 s | 0 | 30 s |
| Lightning Rod | Raycast to a block up to 32 blocks away; real vanilla lightning and 10 immediate HP to nearby enemies | 2 | None |
| Corrupted Seeds | Nearby enemies: Poison II for 15 s and entanglement for 5 s | 0 | 25 s |
| Gong of Weakening | Weakness for 30 s to enemies within 10 blocks | 0 | 45 s |
| Satchel of Elements | Enemies within 8 blocks: random real lightning with 6 immediate HP, 5 s fire, or 3 s levitation | 0 | 15 s |
| Death Cap Mushroom | Consumed: Speed II for 15 s and Strength II for 10 s; 1 HP magic damage on Easy/Normal, 2 on Hard | 0 | 10 s |
| Ghost Cloak | Horizontal wall/mob phasing and concealment for 10 s; floors/ceilings remain solid | 15 | 30 s |
| Updraft Tome | Up to 6 nearest enemies within 15 blocks: Levitation II for 3 s, then stun for 4.5 s; next fall damage ×3 | 0 | 20 s |
| Boots of Levitation | Toggle Levitation II for at most 25 s; no added Slow Falling or HUD countdown | 0 | Half the active duration when stopped early; 15 s at the limit |

Timers use server ticks at 20 ticks/second. Boots preserve half-tick cooldowns
(0.025 s); enforcement occurs on server ticks. Cooldowns belong to the player and artifact type, so changing
stacks or hands does not reset them. Souls and cooldowns survive reconnects. Cooldowns survive death. Boots stop on logout, death or dimension change; stopping removes levitation and clears accumulated fall distance without adding
Slow Falling. Falling afterward can hurt you. The mushroom
stacks to 16, consumes exactly one on successful survival use, and is retained in
creative. Peaceful mode causes no mushroom self-damage.

Players are allies by default, as are their own pets and scoreboard teammates.
Hostile monsters and mobs attacking allies are enemies. The configuration can
enable targeting non-allied players. Potion splash follows vanilla splash rules;
throw harmful potions carefully. Corrupted vines allow Poison to affect undead;
roots prevent horizontal/upward movement while allowing gravity. Stun stops mob
AI, horizontal movement and attacks, while allowing the tome's upward motion.

Ghost Cloak does not use spectator mode or `noPhysics`. Its collision calculation
retains vertical collision, and server movement validation remains active. It
hides the player model, armor, held items and normal effect/sprint particles.
Ending inside a block searches for free supported space, then the saved safe
position. If the surroundings have been completely obstructed and no safe exit
can be found, the effect briefly extends and retries instead of trapping the
player. Dimension transitions discard anchors from other dimensions.

Inside a solid wall, Ghost Cloak reveals a five-block horizontal cutaway with a
diamond footprint and exactly two block layers: feet and head. Floor, ceiling and
perimeter use nearby block textures; the view remains closed beyond its range.
This is a client view only and never removes blocks from the world.

## Survival discovery and enchantments

Soul, Grove and Tempest shrines generate in new chunks in plains, forest, birch
forest, taiga, meadow and savanna biomes. There is one candidate per 32×32 chunk
region, with 12-chunk minimum separation; unsuitable biomes can prevent placement.
Each shrine has raised arches, one uniformly selected non-mushroom artifact in
an item frame, and two decorated pots with supplies. Pots may contain **2–4**
Death Cap Mushrooms; mushrooms are never the main framed reward. Existing shrines
already generated in saved worlds are preserved.

**Soul Vessel I–III** adds 10 capacity per level on equipped armor.
**Soul Reservoir I–II** adds 15 capacity per level on held non-consumable artifacts.
**Soul Vacuum I** multiplies soul gathering by **1.5×** when the killing weapon
is an enchanted sword, axe, spear, bow or mace (projectiles retain their weapon).
All three are available through the enchanting table, enchanted books, trades and
random enchantment loot. Equipped levels add together. Removing capacity-granting
items clamps souls to the new maximum.

With commands enabled:

```mcfunction
/give @s dungeons_artifacts:soul_healer
/locate structure dungeons_artifacts:artifact_shrine
/place structure dungeons_artifacts:artifact_shrine
/place template dungeons_artifacts:soul_shrine
/loot give @s loot dungeons_artifacts:pots/artifact_shrine
/enchant @s dungeons_artifacts:soul_reservoir 2
```

For natural generation, explore new chunks or create a new world. Structure sets,
biome tags and loot tables are standard data-pack resources and can be overridden
by a data pack.

## Bottles and soul attunement

Hold empty glass bottles in your **offhand** while receiving credit for mob kills to start
filling a soul bottle. Each kill is captured at your current soul gathering rate,
**instead of** adding those souls to your meter. The tooltip displays the amount rounded down, up to **50**, while storage retains
fractions exactly. Partial and full bottles have different textures. Spare
empty bottles in the offhand stack return to inventory (or drop if it is full).

Brew a **full** soul bottle with **one ice block** in a fueled brewing stand to
make **Liquid Souls**. Drinking it restores **25 souls**, limited by capacity,
and returns a glass bottle in survival.

Craft an **Attuned Compass** using this pattern (I = iron ingot, E = echo shard,
S = full soul bottle, C = compass):

```text
E S E
I C I
  E
```

The recipe uses one full bottle and unlocks when you obtain Soul Healer, Lightning
Rod or Ghost Cloak.

There are exactly **eight** altar sites in the Overworld, 2,000–20,000 horizontal
blocks from the world's spawn when the system first initializes. Their positions
are seed-derived and saved, so moving spawn afterward does not relocate them.
The structures appear when their chunks load, including in existing worlds.
The compass points to the nearest unused altar and breaks, with a message, when
you enter its 20-block horizontal radius in the Overworld. It cannot guide you
in other dimensions. Once all altars are spent, it has no target.

Stand on the central lodestone for a **ten-second ritual**. You are held in place
as souls and sculk particles rise, circle, and condense into you. Each completed altar increases your
persistent attunement by one and adds **+1 soul per kill** before Soul Vacuum's
multiplier. An altar is consumed globally, so only one player can claim each one.
Used centers turn into crying obsidian. Logout, death or leaving cancels an
unfinished ritual without spending the altar. Attunement survives death.

## Death echoes

With `keepInventory` disabled, death removes all carried souls and leaves a
**soul echo** at the death location: an orbit of cyan soul flames and sculk souls.
Return within two blocks to reclaim it. Only its owner can collect it; echoes
cannot burn or despawn, survive saving/unloading, and multiple deaths create
separate echoes. If your capacity is lower, the excess stays in the echo until
you can collect it. Death messages provide coordinates. Void deaths place the
echo on the surface at the same X/Z coordinates so it remains reachable.

With `keepInventory` enabled, souls (including fractions) stay with you and no
echo is created. Attunement and command-set capacity always survive death.

## Soul commands and advancements

Operator level 2 (or the corresponding server permission) is required. Both
`/Soul` and `/soul` work, including player selectors:

```mcfunction
/Soul add @p 25
/Soul clear @p
/Soul max @p 100
```

`add` respects current capacity; `clear` also clears fractional souls while
preserving cooldowns. `max` sets a persistent **absolute capacity** for each
target, overriding equipment bonuses, and clamps the current balance if needed.
Amounts range from 0 to 1,000,000. Commands affect the player's balance, not any
unclaimed echoes left elsewhere.

The **Artifacts** advancement tab unlocks on obtaining an artifact:

- **God of lightning:** ten successful Lightning Rod uses within five seconds.
- **You sold me your soul:** capture a credited kill in an offhand bottle.
- **I should have brought a bucket:** use levitation boots for their full 25 seconds.
- **DEATHcap shroom:** die from the mushroom's self-damage.
- **Soul searching:** reclaim a death echo.
- **The sculk chose me:** finish an attunement ritual.
- **A very heavy backpack:** carry all eleven artifacts at once.

## Configuration

`config/dungeons_artifacts.json` is created on first launch. Restart the server
(or integrated server) after editing it. Invalid values fail clearly instead of
being silently replaced.

| Setting | Default |
| --- | ---: |
| `soulCapacity` | 50 |
| `vesselCapacityPerLevel` / `reservoirCapacityPerLevel` | 10 / 15 |
| `shieldRadius` / `shieldDurationTicks` / `shieldCooldownTicks` | 4 / 200 / 600 |
| `seedsRadius` | 6 |
| `elementsRadius` | 8 |
| `elementalLightningDamage` | 6 HP |
| `elementalFireSeconds` / `elementalWindTicks` | 5 / 60 |
| `lightningRange` / `lightningRadius` | 32 / 3 |
| `hostilePlayers` | false |

The legacy `soulsPerHostileKill` config key is retained for old configs but no
longer controls rewards; the inventory/attunement formula above does. Real
lightning follows vanilla fire rules and can affect nearby allies and terrain.

Server configuration controls gameplay. No client activation payload supplies
soul values, costs, cooldown deadlines or target lists. The vanilla use packet
selects a hand; the server verifies its actual item and calculates the result.
Fabric data attachments synchronize HUD values and persist player state.

## Building and testing

Requires JDK **25**. The checked-in Gradle wrapper is **9.8.2**, with its official
SHA-256 pinned. Loom is pinned to **1.19.3**. Use the existing isolated checkout;
do not create an additional worktree unless explicitly requested.

```bash
./gradlew build
./gradlew runGametest
./gradlew runClientGametest
```

Client tests require a working graphical display (a local dummy Xorg display and
Mesa software rendering were used in this environment). They are separate from
`build`; server game tests also run during `build`. Test tasks accept the Minecraft EULA
for their disposable test instances through Loom's `configureTests` setting.

In the managed cloud workspace, activate the installed toolchain first:

```bash
cd /workspace/Dungeons-artifacts
source scripts/cloud-env.sh
./gradlew build --no-daemon --console=plain
```

`bash scripts/setup-toolchain.sh` installs a checksum-verified Java 25 when needed.
The cloud activation script preserves the configured proxy and Java CA trust.
Offline rebuilding works after dependencies have been cached. No running process
needs to be retained between tasks.

Dependency-free core checks and resource regeneration:

```bash
bash scripts/test-core.sh
python3 scripts/generate-assets.py
python3 scripts/generate-content.py
python3 scripts/generate-soul-content.py
python3 scripts/check-assets.py
```

Run both generators in that order: the second adds descriptions and content to the
base resources produced by the first. Models, textures and shrine templates are
original, reproducible assets; no Minecraft Dungeons artwork was copied.

## Punchy compatibility and verification

The mod uses ordinary Minecraft item models and the documented optional Punchy
resource compatibility format. `assets/minecraft/punchy/compat/dungeons_artifacts.json`
assigns the artifact group `physics: NONE`, avoiding extra item sway. It changes no
global Punchy settings and introduces no dependency on an invented Java API.

**Punchy itself has not been runtime-tested in this environment.** In particular,
its own hand rendering and other mods that replace player collision/rendering need
integration testing. Automated tests cover the base Fabric installation; consult
[the test report](docs/TEST_REPORT.md) for exact coverage and limitations.

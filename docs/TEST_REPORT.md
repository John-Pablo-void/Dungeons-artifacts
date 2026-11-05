# Verification — 0.3.0-dev, Minecraft 26.2 / Fabric

Validated on 2026-11-05 with Java 25, Gradle 9.8.2, Loom 1.19.3,
Fabric Loader 0.20.1 and Fabric API 0.161.0+26.2.

## Checks

- `./gradlew build --offline`: common/client compilation, JARs, JUnit/core ledger
  checks (79 assertions), and server game tests.
- Server suite: 26 mod game tests plus Minecraft's `always_pass`, 27 total.
- `./gradlew runClientGametest --offline`: real graphical client on dummy Xorg
  and Mesa software OpenGL.
- `python3 scripts/check-assets.py`: original eleven artifact resource references,
  model bounds, display transforms, PNG integrity and translations.

## New gameplay regression coverage

- Actual death and respawn empty the soul balance, preserving the exact quarter-
  soul amount in an owner-bound echo. Another player cannot collect it. Recovery
  obeys capacity, leaves excess behind, and ultimately returns all fractions.
  Echo location/owner/amount survive codec serialization. Actual keepInventory
  respawn retains the balance and produces no echo.
- A lethal mushroom grants its challenge and cannot restore the pre-death soul
  balance after the death callback. Ordinary mushroom consumption remains covered.
- A player hit followed by a fatal fall earns souls; an uncredited environmental
  death does not. Credited passive mobs also grant souls. Kill particles originate
  from the dead entity; fractional gains are retained.
- Both command spellings work, selector targets resolve, additions respect the
  command-set absolute capacity, clear removes fractions, and non-operators are
  denied. Command changes preserve cooldowns.
- Ten successful lightning uses award the challenge; nine do not, and a tenth
  use after the five-second window does not combine with expired uses.
- Early boot deactivation does not award the challenge. A real 500-tick activation
  expires and awards it. Capturing a kill in an offhand bottle awards its challenge.
- A soul artifact unlocks the compass recipe and Artifacts tab. The exact
  `ESE / ICI / [space]E[space]` crafting input matches with one full soul bottle;
  a bottom-left echo shard fails to match.
- Attunement remains incomplete after five seconds and completes after ten.
  Existing tests still cover one-use altars, deterministic eight-site placement,
  compass breakage, fractional vacuum rewards, full brewing cycles, shrine loot,
  supported generated item frames, shielding, artifact effects and persistence.

## Graphical verification

The client toggles J, synchronizes a 9.75-soul balance and displays only `9` above
a segmented cyan XP-style bar. It opens the synchronized Artifacts advancement
tab and captures its tree. Collecting all eleven artifacts earns the backpack
challenge in the actual client/server session.

Real keyboard movement crosses a solid wall without crossing the floor. Inside
a test volume, a five-block diamond cutaway renders different floor/ceiling
textures and separate stone/deepslate layers for legs/head. Screenshots inspect
forward, upward and downward views. Server checks confirm that real blocks in
the visible region remain intact. Normal-terrain shrine placement remains tested.

Screenshots are in `docs/screenshots/`, including `soul-hud.png`,
`ghost-five-block-window.png`, `ghost-adjacent-floor.png`,
`ghost-adjacent-ceiling.png`, and `artifacts-advancements.png`.

## Limits

Tests cover an integrated graphical client and simulated server players, not a
public multiplayer session or every combination with other mods. Echo persistence
is verified through its codec and ordinary server-world save lifecycle, rather
than a separate full process restart followed by recovery. Ritual timing is tested
server-side; its new sculk particles have not been visually judged in every biome.
Punchy remains uninstalled for runtime testing.

Cloud logs contain unavailable Mojang authentication endpoints, missing audio
hardware and a dummy-display cursor warning. These do not prevent the tests.

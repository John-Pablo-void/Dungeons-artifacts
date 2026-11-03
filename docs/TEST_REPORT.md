# Verification — Minecraft 26.2 / Fabric

Validated in the cloud environment on 2026-11-02 using Java 25, Gradle 9.8.2,
Loom 1.19.3, Fabric Loader 0.20.1 and Fabric API 0.161.0+26.2.
The installable output is `build/libs/dungeons-artifacts-0.1.0-dev.jar`.

## Results

| Check | Result |
| --- | --- |
| `./gradlew build` | Passed; common and client Java compiled and both mod/source JARs produced |
| JUnit regression suite | 1 test passed, executing 79 assertions against the core ledger |
| `coreTest` / `scripts/test-core.sh` | 79 assertions passed |
| `./gradlew runGametest` | 13 required artifact tests passed; the runner also executed Minecraft's optional `always_pass` test (14 total) |
| `./gradlew runClientGametest` | Passed in a real graphical client using dummy Xorg and Mesa software rendering |
| Resource validation | All 11 item/model/texture references, PNG integrity, geometry and translations passed |
| Packaged JAR inspection | Target/version, main/client classes and mixins, three NBT structures verified; test entrypoints excluded |

The tests were rerun after fixes; no failing checks were disabled or counted as
passes. Dependency checksums and TLS verification remained enabled throughout.

## Server game-test coverage

- All eleven items registered; item activation runs on server player objects.
- Soul Healer restores 8 HP, charges five souls, respects per-player cooldowns
  across replacement stacks, and safely heals scoreboard-allied undead.
- Insufficient souls prevent Ghost Cloak's effect.
- Elixirs select two distinct effects; sneaking spawns a real splash potion entity.
- Seeds apply Poison II for 300 ticks and root for 100 ticks, including undead;
  actual entity movement is restricted. Gong applies 600-tick Weakness to enemies
  without weakening the friendly caster.
- Updraft applies 90-tick stun and 60-tick levitation; horizontal movement is
  blocked while upward motion remains possible.
- Mushroom effect durations/amplifiers, creative retention, exact survival stack
  consumption, cooldown protection and difficulty-based armor-bypassing damage.
- Boots toggle on/off and provide Slow Falling. The core suite separately checks
  25-second expiry, the 15-second full cooldown, odd-tick half-duration cooldowns,
  reconnect state, and a minimum nonzero represented duration of 0.025 seconds.
- Ghost cost, targeting rejection, actual horizontal movement through a wall,
  floor collision, and safe exit from inside a block.
- Lightning damages the raycast strike area, costs two souls and has no cooldown;
  Elements activation installs its 15-second cooldown.
- Shield creates its 3D item-display totem and intercepts an 8-block-per-tick arrow
  using a segment sweep, rather than relying on a projectile endpoint.
- Hostile kills award souls; Soul Vessel III increases capacity by 30.
- Real player-file save/reload restores souls and cooldowns.
- Each of the three NBT shrine templates places successfully and produces a chest
  with guaranteed artifact loot. Structure registry registration also passed.

## Client and world-generation coverage

A real client connected to an integrated server and verified synchronized soul
values. Screenshots were inspected for the inventory models, visible first-person
artifact/HUD, and hidden hands while Ghost Cloak was active. No missing artifact
models or textures were reported.

The client test holds the forward movement key to cross a solid three-block-high
wall during Ghost Cloak. It checks the resulting server position and floor height;
this exercises client prediction, movement packets, collision and server validation.
It also verifies effect synchronization and removal.

A second, ordinary terrain world locates the custom structure using the real
structure generator, generates its chunk, verifies a valid structure start and
finds a chest referencing the artifact loot table inside the generated bounds.
This caught and fixed the initial jigsaw depth of zero, which produced no pieces.

## Practical limits

- **Punchy was not installed for runtime testing.** Its documented optional
  resource compatibility file is included. Coexistence, custom Punchy hand
  rendering and other mods' collision/render changes remain unverified.
- Tests used one real client and multiple server-side test players. A session with
  several separate human-operated clients was not exercised.
- Potion collision/splash behavior uses the actual vanilla projectile; the test
  asserts projectile spawning rather than every possible collision and effect.
  Random effect branches were not exhaustively sampled in gameplay tests.
- Boots' full-duration timing is covered by the deterministic core regression
  suite; a 25-second real-client flight was not timed manually.
- Authentication/skin services are unavailable in this environment. Local testing
  used Fabric's test profiles; online-account authentication was not tested.
- Dummy Xorg logged an unavailable cursor shape at shutdown; client tests still
  exited successfully. Audio uses vanilla sound events; this environment has no
  physical audio output for listening tests.

These limits do not replace the successful compilation, server tests, client
launch, movement/synchronization tests and natural structure-generation check.

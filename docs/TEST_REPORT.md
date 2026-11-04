# Verification — 0.2.0-dev, Minecraft 26.2 / Fabric

Validated on 2026-11-04 with Java 25, Gradle 9.8.2, Loom 1.19.3,
Fabric Loader 0.20.1 and Fabric API 0.161.0+26.2.

## Results

| Check | Result |
| --- | --- |
| `./gradlew build --offline` | Passed: main/client compilation, mod and source JARs, unit tests, server game tests |
| JUnit / coreTest | Passed: 79 ledger assertions |
| Server game tests | 18 mod tests plus vanilla's `always_pass`, 19 total, all passed |
| `./gradlew runClientGametest --offline` | Passed on dummy Xorg with Mesa OpenGL software rendering |
| `python3 scripts/check-assets.py` | Passed: eleven artifact models, resource links, PNG integrity and translations |

## Changes exercised in Minecraft

- Levitation boots apply amplifier 1 (II), toggle off without adding Slow Falling.
  The existing 25-second duration and half-duration/15-second cooldown rules are
  retained; their HUD countdown is removed.
- Updraft selects at most six mobs, applies Levitation II first, and applies stun
  after levitation ends. An actual five-block fall deals six HP instead of two;
  the fall modifier is consumed afterward.
- Lightning Rod spawns a functional bolt. Ticking that bolt charges a creeper and
  ignites an entity whose vanilla fire immunity has ended. The original immediate
  ten-HP rod damage is retained; subsequent real flashes obey vanilla hurt immunity.
- Soul Vessel on armor held in the main hand gives no capacity; equipping it gives
  the intended bonus once. Soul collection scales by inventory artifacts and caps
  at three before bonuses. Vacuum and attunement bonuses are applied correctly.
- Four kills at 0.75 souls give exactly three souls. A real player-file save/reload
  preserves fractional souls, attunement level, ordinary souls and cooldowns.
- Offhand bottles capture the normal gathering reward without duplicating it in
  the player's meter. Only full bottles accept ice. A fueled brewing stand runs
  its complete 400-tick cycle, consumes one ice, produces Liquid Souls, and leaves
  an adjacent partial bottle untouched. Drinking Liquid Souls restores 25 souls.
- Altar layout is deterministic with eight unique positions in the required
  spawn-distance range. Loading a pending site constructs its altar. Nearby
  compasses break. Completing the ritual raises attunement and marks the altar
  spent; it cannot grant a second reward.
- Shrine templates contain framed non-mushroom artifacts and decorated pot loot;
  mushroom stacks are at least two. A normal terrain world is searched using the
  real structure generator: its shrine has a valid structure start, loot pots,
  and a supported, nonempty item frame after world placement/rotation.
- Existing artifact healing, effects, cooldowns, consumption, shield interception,
  ghost collision, targeting rejection, safe exit and reconnect tests still pass.

## Client inspection

The client presses J and verifies the soul-meter toggle, synchronized souls,
original artifact inventory models and hidden ghost hands. A real W-key movement
crosses a solid wall while preserving floor collision on the server.

The camera is then placed inside stone, above gold and below diamond. The inspected
screenshots show only those adjacent floor/ceiling surfaces and the enclosing
stone, with no distant terrain visible. The soul count fits inside the replacement
XP bar without overlapping the hearts.

Current screenshots are in `docs/screenshots/`: `soul-hud.png`,
`ghost-adjacent-floor.png`, and `ghost-adjacent-ceiling.png`.

## Limits

- Tests use simulated server players and an integrated graphical client, not a
  multi-user public server or every possible combination with other mods.
- Punchy compatibility resources remain included; Punchy itself was not installed.
- The ritual and seeded generation are exercised separately; the tests do not
  explore every altar chunk in a full survival world or visually judge all rituals.
- Cloud runs log unavailable Mojang authentication endpoints, absent audio hardware,
  and a dummy-display cursor warning at shutdown. These did not prevent tests.
- Existing generated shrines are not rebuilt. Unopened legacy shrine chests use
  the corrected loot restrictions; new chunks get arches, frames and pots.

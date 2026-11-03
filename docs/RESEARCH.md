# Minecraft 26.2 research

Research performed 2026-11-02. Target remains **26.2**.

## Verified upstream configuration

The official [Fabric example, branch 26.2](https://github.com/FabricMC/fabric-example-mod/tree/26.2),
commit `1eb2bde4d23f98a785ab6fd9ed18b3502d035b3f`, specifies:

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Java release | 25 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.161.0+26.2 |
| Loom plugin | `net.fabricmc.fabric-loom`, 1.19-SNAPSHOT |
| Gradle | 9.8.2 |

This target uses unobfuscated Minecraft classes. The official template uses
`implementation` for Fabric dependencies and has no Yarn mappings declaration.
Do not transplant a 1.21-era Loom configuration.
The upstream example used a Loom snapshot; the project now pins verified stable
Loom 1.19.3, confirmed from official Maven metadata after network access was restored.

Gradle distribution SHA-256:
`21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607`

Gradle wrapper JAR SHA-256:
`3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`

Both were compared against the official `services.gradle.org` checksum files.

## Available source evidence

- [Fabric API 26.2](https://github.com/FabricMC/fabric/tree/26.2), commit
  `6a8ed6bc5a15a01cd6979998fc950f32a67010dc`: attachment synchronization and
  persistence examples, item APIs, HUD registration, networking tests.
- [Fabric documentation](https://github.com/FabricMC/fabric-docs), commit
  `2e637f701af37e6a5305f3ed11bbe3472073b0b0`: `reference/latest/build.gradle`
  explicitly targets 26.2. Item model documentation defines cuboids, UVs, and
  inventory/hand/ground display transforms used by the original assets here.
- [Punchy wiki](https://github.com/punchy-mod/punchy-wiki/wiki), commit
  `7c974c7e469700dce8db6a48f07e7584326148e4`: resource pack compatibility guide
  documents `assets/minecraft/punchy/compat/*.json`, `customGroups`, and
  `itemSpecific` transforms. This is a resource integration surface, not evidence
  of a server gameplay API. No Punchy Java methods were invented or added.

Fabric API already supplies networking, synced/persistent data attachments,
HUD registration and lifecycle hooks. Vanilla cuboid models avoid a mandatory
animation library. No additional library has been selected or claimed compatible
with 26.2. Advanced rendering libraries remain unevaluated beyond this decision;
the required vanilla entity/collision APIs still need game sources and compilation.

`Minecraft-vibecode.md` was not among the available files. The supplied full task
text is the current source specification. The selected GitHub repository returned
no refs from `git ls-remote origin HEAD`; the local `work` branch has no commits.

## Resumed implementation

The network additions were published by the user. Fabric and Minecraft downloads
now work in the attached environment. `genSources` completed successfully for
Minecraft 26.2, and implementation was compiled against these sources.
Loom 1.19.3 was confirmed in official Maven metadata and pinned in gradle.properties.
Player attachment APIs are registered during common initialization, before the
configuration handshake. Vanilla 26.2 source was inspected for item use, collision,
AI, potion projectiles, entity displays, player persistence and client rendering.

No extra gameplay or rendering library is necessary beyond Fabric API. JUnit 6.0.0
is a test-only dependency. Vanilla data pack schemas for structures, template pools,
enchantments and loot were checked against the 26.2 Minecraft JAR.

Implementation and runtime validation now supersede the earlier onboarding blocker.
See README.md and TEST_REPORT.md for the current state. The user controls environment
publication; no environment configuration was republished during mod development.

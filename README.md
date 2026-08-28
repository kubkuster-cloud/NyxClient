# Nyx Client

Standalone Fabric mod for Minecraft 1.21.4. Not affiliated with, injected into, or designed to
impersonate any third-party client (Lunar, Badlion, etc.). Intended for singleplayer, LAN, and
private servers you have permission to test on. Several modules (Fly, NoFall, Jesus, Fastbreak)
only actually work where the server trusts client-reported state, which most anti-cheat-protected
servers don't.

The AntiCheat module shapes the other combat modules' output to resemble real mouse input and
reacts to server setbacks. It exists to make behaviour observable during authorized testing on a
server whose operator has agreed to it — running it anywhere else is a straightforward way to get
banned, and is not what this repository is for.

## Build

Requires JDK 21.

```
./gradlew build          # Linux/macOS
.\gradlew.bat build      # Windows
```

If the project ever sits in a path with non-ASCII characters, `gradlew.bat` breaks on Windows
(`cmd.exe` mangles the path when computing `%~dp0`, independent of console code page — `chcp 65001`
does not fix it). Either move the project to an ASCII-only path, or bypass the batch script:

```
java -jar gradle\wrapper\gradle-wrapper.jar build
```

The first build downloads and decompiles Minecraft/mappings — it's slow and needs internet access.
Output jar lands in `build/libs/`. Drop it into `.minecraft/mods` alongside a matching Fabric
Loader + Fabric API install for 1.21.4.

### Versioning

`gradle.properties` carries `mod_version` as major.minor only; the patch comes from `.build-number`,
an untracked counter that increments after each jar is actually produced. So successive builds are
`nyxclient-0.1.0.jar`, `nyxclient-0.1.1.jar`, … and a fresh jar can never silently overwrite the one
already sitting in `mods/`. Bump `mod_version` by hand for a real release and delete `.build-number`
to restart the patch at 0.

## Dev run

```
./gradlew runClient
```

## Layout

- `module/` — module base class, `ModuleManager`, and all modules under `module/modules/<category>`
- `setting/` — typed, serializable module settings (bool/double/int)
- `gui/ClickGuiScreen` — ClickGUI with draggable per-category panels, expandable settings rows,
  checkboxes and sliders. Opened with **P** by default, rebindable in vanilla Controls under
  `key.nyxclient.open_gui`. P is vanilla's Social Interactions key, so that binding is unbound at
  startup for as long as it collides with ours. An install carrying the older Right Shift default in
  its options.txt is pulled forward to P on the next launch, unless the key was picked deliberately
- `config/ConfigManager` — saves enabled state + settings to `<gamedir>/config/nyxclient.json`
- `mixin/` — the seven hooks that can't be done through Fabric API events alone (see below)

## Modules

| Module | Category | Notes |
|---|---|---|
| Fly | Movement | Sets creative-style flight abilities each tick |
| Speed | Movement | Multiplies horizontal velocity while walking/sprinting on the ground |
| Scaffold | Movement | Places a hotbar block underfoot when walking over a gap |
| NoFall | Movement | Spoofs `onGround=true` in outgoing movement packets so the server never accumulates fall damage |
| Jesus | Movement | Holds you on top of water; sneak to sink through |
| AntiCheat | Combat | Quantizes/rate-limits the combat modules' rotations, jitters attack timing, pauses and self-tunes on setbacks |
| Killaura | Combat | Attacks nearest valid entity on a tick cooldown |
| AimAssist | Combat | Eases the view toward the entity closest to the crosshair (FOV cone, capped turn rate) |
| AutoTotem | Combat | Keeps a Totem of Undying in the offhand, refilling from inventory |
| AutoArmor | Combat | Equips the best armor carried, swapping in any upgrade found in the inventory |
| ESP | Render | Uses vanilla's glow-outline pass to show entities through walls |
| Fullbright | Render | Renders every block at full brightness; reloads chunks on toggle |
| NoHurtCam | Render | Removes the camera tilt played when you take damage |
| Fastbreak | World | Overrides calculated mining speed to break blocks instantly |
| AutoTool | World | Switches to the fastest suitable tool for the block being mined |
| AutoRespawn | World | Respawns automatically instead of sitting on the death screen |

## Mixins

Compiled end-to-end against real Minecraft 1.21.4 / Yarn 1.21.4+build.8 (not written against assumed
API names), and confirmed against the generated refmap:

| Mixin | Target | Serves |
|---|---|---|
| `ClientPlayerEntityMixin` | `sendMovementPackets` → redirects `isOnGround()` | NoFall |
| `ClientPlayNetworkHandlerMixin` | `onPlayerPositionLook` (TAIL) | AntiCheat setback detection |
| `EntityMixin` | `setGlowing` → redirects `isGlowing()` to `isGlowingLocal()` | ESP |
| `GameRendererMixin` | `tiltViewWhenHurt` (cancelled) | NoHurtCam |
| `MinecraftClientMixin` | `isAmbientOcclusionEnabled` | Fullbright |
| `PlayerEntityMixin` | `getBlockBreakingSpeed(BlockState)` | Fastbreak |
| `WorldRendererMixin` | `getLightmapCoordinates(BlockRenderView, BlockState, BlockPos)` | Fullbright |

Two spots needed fixing from the initial draft (now corrected and build-verified):

- `PlayerAbilities.flySpeed` is private — use `setFlySpeed(float)` (Fly)
- `PlayerInventory` has no `getSelectedSlot()` — the field is `selectedSlot` (read directly), paired with `setSelectedSlot(int)` to write (Scaffold)

Everything here compiles against the real classes; the modules have not been systematically
runtime-tested across a game session, so treat visual/behavioural details as unconfirmed until you
have watched them in `runClient`.

# Nyx Client

Standalone Fabric mod for Minecraft 1.21.4. Not affiliated with, injected into, or designed to
impersonate any third-party client (Lunar, Badlion, etc.). Intended for singleplayer, LAN, and
private servers you have permission to test on. Several modules (Fly, NoFall, Fastbreak) only
actually work where the server trusts client-reported state, which most anti-cheat-protected
servers don't.

The AntiCheat module shapes the other combat modules' output to resemble real mouse input and
reacts to server setbacks. It exists to make behaviour observable during authorized testing on a
server whose operator has agreed to it — running it anywhere else is a straightforward way to get
banned, and is not what this repository is for.

## Build

Requires JDK 21. **Verified**: this project builds cleanly against real Minecraft 1.21.4 /
Yarn 1.21.4+build.8 mappings — `nyxclient-0.1.0.jar` and `nyxclient-0.1.0-sources.jar` compile
with zero errors, including both mixins.

This folder's name (`NIE_PODGLĄDAĆ`) has non-ASCII characters, which breaks `gradlew.bat` on
Windows (`cmd.exe` mangles the path when computing `%~dp0`, independent of console code page —
`chcp 65001` does not fix it). Two options:

```
# Option A: call the wrapper jar directly, bypassing gradlew.bat's path handling
java -jar gradle\wrapper\gradle-wrapper.jar build

# Option B: move/rename the project to an ASCII-only path, then gradlew.bat works normally
.\gradlew.bat build
```

On Linux/macOS (or any ASCII path on Windows), `./gradlew build` works as normal.

The first build downloads and decompiles Minecraft/mappings — it's slow and needs internet
access. Output jar lands in `build/libs/`. Drop it into `.minecraft/mods` alongside a matching
Fabric Loader + Fabric API install for 1.21.4.

## Dev run

```
java -jar gradle\wrapper\gradle-wrapper.jar runClient
```

## Layout

- `module/` — module base class, `ModuleManager`, and all modules under `module/modules/<category>`
- `setting/` — typed, serializable module settings (bool/double/int)
- `gui/ClickGuiScreen` — minimal ClickGUI, opened with **P** by default (rebindable in vanilla
  Controls once a keybinding entry exists — see `key.nyxclient.open_gui`). P is vanilla's Social
  Interactions key, so that binding is unbound at startup while it collides with ours
- `config/ConfigManager` — saves enabled state + settings to `config/nyxclient.json`
- `mixin/` — the two hooks that can't be done through Fabric API events alone (Xray block culling,
  Fastbreak mining speed)

## Modules

| Module | Category | Notes |
|---|---|---|
| Fly | Movement | Sets creative-style flight abilities each tick |
| Speed | Movement | Nudges horizontal velocity while walking |
| Scaffold | Movement | Places a hotbar block underfoot when walking over a gap |
| NoFall | Movement | Zeroes client fall distance every tick |
| Killaura | Combat | Attacks nearest valid entity on a tick cooldown |
| AimAssist | Combat | Eases the view toward the entity closest to the crosshair (FOV cone, capped turn rate) |
| AntiCheat | Combat | Quantizes/rate-limits the combat modules' rotations, jitters attack timing, pauses and self-tunes on setbacks |
| ESP | Render | Uses vanilla's glow-outline pass to show entities through walls |
| Xray | Render | Cancels chunk-mesh rendering for non-ore blocks |
| Fastbreak | World | Overrides calculated mining speed to break blocks instantly |

## Mapping verification

Built and compiled end-to-end against real Minecraft 1.21.4 / Yarn 1.21.4+build.8 (not just
written against assumed API names). Confirmed via the generated mixin refmap that both mixins
resolve to real methods:

- `BlockRenderManagerMixin` → `BlockRenderManager.renderBlock(BlockState, BlockPos, BlockRenderView, MatrixStack, VertexConsumer, boolean, Random)` (Xray)
- `PlayerEntityMixin` → `PlayerEntity.getBlockBreakingSpeed(BlockState)` (Fastbreak)

Two spots needed fixing from the initial draft (now corrected and build-verified):

- `PlayerAbilities.flySpeed` is private — use `setFlySpeed(float)` (Fly)
- `PlayerInventory` has no `getSelectedSlot()` — the field is `selectedSlot` (read directly), paired with `setSelectedSlot(int)` to write (Scaffold)

Not runtime-tested in an actual game session yet (only compiled) — `Entity#setGlowing` behavior
(ESP), `WorldRenderer#reload()` (Xray chunk refresh on toggle), and `Input#movementForward`/`movementSideways`
(Speed) compile against the real classes but haven't been visually confirmed in `runClient`.

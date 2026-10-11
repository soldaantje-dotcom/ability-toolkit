# Ability Toolkit (Fabric, Minecraft Java 26.2)

An **educational** mod that adds three configurable abilities - **No Knockback**, **Flight** and **Speed Boost** -
with keybinds, a settings GUI, a HUD and saved settings. It is built so the **server stays in charge**: abilities only
work in singleplayer/LAN worlds you host, or on servers whose admin has explicitly authorized you.

## Verified target

| Item | Version | Notes |
|---|---|---|
| Minecraft Java Edition | **26.2** | Released; Fabric published support on 2026-06-15 |
| Mod loader | **Fabric Loader 0.19.3** (newer is fine) | Loader supports 26.2 |
| Fabric API | **0.152.2+26.2** | Newer 26.2 builds exist: see https://fabricmc.net/develop |
| Fabric Loom | **1.17-SNAPSHOT** | Required for 26.2 |
| Gradle | **9.5.1** | Required for 26.2 |
| Java | **25** | Minecraft 26.1+ requires Java 25 |
| Mappings | **none needed** | Since 26.1 the game is unobfuscated; the code uses official Mojang names |

## Install (players)

1. Install Java 25 and the Fabric installer from https://fabricmc.net/use/installer/ (select Minecraft 26.2).
2. Put **Fabric API for 26.2** and `abilitytoolkit-1.0.0.jar` into `.minecraft/mods`.
3. Launch the `fabric-loader-26.2` profile.

Singleplayer works immediately. **Dedicated servers are locked by default** (see below).

### Dedicated server setup

Install Fabric Loader + Fabric API + this jar on the server. On first start it writes
`config/abilitytoolkit-server.json`:

```json
{
  "integratedServerAllowAll": true,
  "authorizedPlayers": [],
  "allowNoKnockback": true,
  "allowFlight": true,
  "allowSpeed": true,
  "maxSpeedLevel": 4,
  "integratedMaxSpeedLevel": 10
}
```

Add player UUIDs (with dashes) to `authorizedPlayers` and restart. Players not on the list, and players on a server
without this mod, have every ability locked. If you run anti-cheat, test it and exempt authorized players as needed;
this mod does not try to hide from or bypass any anti-cheat.

Note: when you open a singleplayer world to LAN, everyone on it counts as authorized (the host owns the world). Set
`integratedServerAllowAll` to `false` and list UUIDs to change that.

## Build (developers)

```bash
# The Gradle wrapper script/jar are not included. Either:
gradle wrapper --gradle-version 9.5.1        # if Gradle is installed
# or copy gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.jar from https://github.com/FabricMC/fabric-example-mod

./gradlew build         # jar is written to build/libs/
./gradlew runClient     # start a dev client
```

Open the folder in IntelliJ IDEA (2025+) with a Java 25 SDK.

## Controls

| Action | Default key | Rebind in |
|---|---|---|
| Open ability menu | Right Shift | Options > Controls > Key Binds > Ability Toolkit |
| Toggle No Knockback | K | same |
| Toggle Flight | G | same |
| Toggle Speed Boost | V | same |

Flight uses vanilla creative controls: **Space** = up, **Sneak** = down, WASD = any horizontal direction, and
**Sprint** while flying is faster. Speed level 1-10 maps to x1.5 ... x6.0.

Settings are saved to `config/abilitytoolkit-client.json` (toggles, speed level, HUD on/off). Key bindings are saved by
vanilla in `options.txt`.

## Project layout

```
src/main/java/.../abilitytoolkit/          (one source set; client/ package is only used by the client entrypoint)
  AbilityToolkit.java        entrypoint: registers packets and server events
  AbilityLimits.java         speed-level -> multiplier maths shared by both sides
  payload/AbilityRequestPayload.java   client -> server: "I'd like these on"
  payload/PermissionPayload.java       server -> client: "you may use these"
  server/ServerPolicy.java   permission gate + JSON config (default deny on dedicated servers)
  server/AbilityManager.java applies abilities with vanilla mechanisms
  client/ (client only)
  AbilityToolkitClient.java  client entrypoint
  ClientConfig.java          saved preferences
  ClientState.java           runtime state (permissions, HUD message)
  AbilityController.java     toggles, sync, per-tick flight start
  Keybinds.java              KeyMappings
  AbilityHud.java            HUD overlay
  AbilityScreen.java / LevelSlider.java   settings GUI
```

## How it works (for learning)

### The big idea: logical sides
Minecraft always runs a **logical server** (even in singleplayer) and a **logical client**. The client *requests*,
the server *decides*. If the client only edited its own copy of the player, the server would disagree and "rubber-band"
you back. So this mod does the real work on the server, with the same tools vanilla uses.

Flow: join -> server sends `PermissionPayload` -> client replies with `AbilityRequestPayload` (saved preferences) ->
server intersects request with policy -> server applies the result -> vanilla syncs it back to the client.

### No Knockback = attributes
Every living entity has **attributes** (health, speed, ...). When something is hit, vanilla scales knockback by
`1 - knockback_resistance`. A resistance of `1.0` makes the multiplier `0`, so there is no knockback. The mod adds a
**transient `AttributeModifier`** (not saved to disk) with `+1.0` to `knockback_resistance`, plus the same on
`explosion_knockback_resistance` so explosions are covered. Identifiers (`abilitytoolkit:no_knockback`) let us add and
remove *our* modifier without touching anyone else's. This is the same mechanism netherite armor uses.

Limits: it covers the vanilla knockback paths that use these attributes. Other mods may add knockback that ignores them.

### Speed Boost = a multiplicative modifier
`movement_speed` has three modifier operations. We use `ADD_MULTIPLIED_TOTAL`: `final = value * (1 + amount)`, so
`amount = multiplier - 1` (x2.5 -> `1.5`). Because the attribute lives on the server and is synced to the client, both
sides predict the same speed, which keeps vanilla movement validation happy. While *flying*, Minecraft ignores
`movement_speed` and uses `Abilities.flyingSpeed` (default `0.05`), so the mod scales that too.

### Flight = the creative-mode flag
Flight is the `Abilities.mayfly` flag. The server sets it and calls `onUpdateAbilities()`, which sends a vanilla
abilities packet. That is why vanilla's fall-damage and "Flying is not enabled on this server" checks behave: the server
itself knows the player may fly. When you toggle flight on, the client waits for that packet and sets `flying = true`,
so you lift off immediately instead of double-tapping jump. Space/Sneak for up/down are vanilla creative controls.

Clean-up matters because `Abilities` is **saved with the player**. The mod removes granted flight on disconnect and
server stop, and tags the player (`abilitytoolkit_flight`) while it is granted so leftovers from a crash are removed on
the next login.

### Keybinds, GUI, HUD, config
- **Keybinds**: `KeyMapping` + `KeyMappingHelper.registerKeyMapping`; `consumeClick()` once per press.
- **GUI**: a `Screen` subclass; widgets are created in `init()`, drawing happens in `extractRenderState`.
- **HUD**: `HudElementRegistry.attachElementBefore(...)` with a `GuiGraphicsExtractor`.
- **Config**: Gson reading/writing JSON in the Fabric config directory.

### Permissions & fair play
- Dedicated servers: default deny, UUID allowlist, per-ability switches, speed cap.
- A server without the mod never receives requests (`ClientPlayNetworking.canSend` is false), so the client stays vanilla.
- The server validates every request and never trusts client values.

## 26.x porting notes (things that differ from older tutorials)
- Official Mojang names, no Yarn, `net.fabricmc.fabric-loom` plugin and `implementation` instead of `modImplementation`.
- `ResourceLocation` is now `Identifier`.
- Screen API: `Minecraft.getInstance().gui.setScreen(...)` / `gui.screen()` (26.2 moved these into `Gui`).
- Rendering: `GuiGraphicsExtractor` (`graphics.text(...)`, `graphics.fill(...)`) and `Screen#extractRenderState`.
- Payload registration: `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()`.

## Status / honesty note
The API names were checked against the official Fabric documentation and the NeoForged 26.2 primer, but **this
project was not compiled or run in a Minecraft 26.2 environment while it was written**. If the build fails, these are
the most likely places (all small):

| File | Call | What to check |
|---|---|---|
| `AbilityManager` | `abilities.mayfly`, `abilities.flying`, `setFlyingSpeed` | field vs. getter/setter on `Abilities` |
| `AbilityManager` | `player.addTag/removeTag` | `Entity` tag API names |
| `AbilityToolkit` | `handler.getPlayer()` | may be `handler.player` |
| `LevelSlider` | `AbstractSliderButton` constructor/`value` | swap for +/- buttons if it changed |
| `AbilityHud` | `client.font.width(Component)` | use `FormattedText` overload |

Use the IDE's "go to declaration" on the unobfuscated Minecraft sources (`./gradlew genSources`) to fix any of them.

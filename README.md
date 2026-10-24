# Sky Lyrics

Fabric client mod bringing Beautiful Lyrics into the Minecraft world. Beautiful
Lyrics presentation and animation are by **surfbryce**, adapted for Minecraft.
See [ATTRIBUTION.md](ATTRIBUTION.md).

**Ready for user Spotify testing.** Both adapters compile and the shared tests
pass under their respective Java runtimes. Live Spotify verification is assigned
to the user; multiplayer, footage, Vulkan and Iris checks are outside this handoff.
See [docs/VERIFICATION.md](docs/VERIFICATION.md) for evidence and outstanding checks.

| Minecraft | Required Java | Mod file | Fabric API used to compile |
|---|---|---|---|
| 1.21.11 | 21 or newer | `skylyrics-0.1.0+mc1.21.11.jar` | `0.141.6+1.21.11` |
| 26.2 | 25 or newer | `skylyrics-0.1.0+mc26.2.jar` | `0.161.0+26.2` |

Fabric Loader is pinned to **0.20.1** for both targets. Compatibility declarations
use exact Minecraft versions. Do not use either jar with another Minecraft release.

## Installation

1. Create a Fabric profile for your exact Minecraft version, using Loader 0.20.1.
2. Select Java 21 for 1.21.11, or Java 25 for 26.2, in the launcher's runtime settings.
3. Put the matching Sky Lyrics jar and matching Fabric API jar in that profile's
   `mods` folder. Install only one Sky Lyrics jar in each profile.
4. The same `beautiful-lyrics-skylyrics.mjs` extension is used for both versions.
   In a shell with Spicetify installed, run `spicetify path -e root` to locate
   its extension directory, and copy the bundle there.
5. Disable the existing Beautiful Lyrics extension before enabling the bridge
   bundle. Other lyrics replacement extensions may conflict with its views.
   Run `spicetify config extensions beautiful-lyrics-skylyrics.mjs`, then
   `spicetify apply`. Restart Spotify if necessary.
6. Enter a Minecraft world, press **K**, and click **Pair with Spotify**.
   In Spotify's profile menu choose **Pair Sky Lyrics…** and enter the code.
   It expires after two minutes. The local pairing token permits automatic
   reconnection and is shared across Minecraft versions.

**Controls:** K opens settings, J recenters the world panel, F8 toggles the original
demo without Spotify. Key mappings can be changed in Minecraft's Controls menu.
Timing offsets, depth occlusion, blur, glow, text size and visibility are in settings.
The panel is anchored in the world in front of the current camera when centered.
Turn **Skybox anchor** on in K settings to keep its direction and apparent
distance as you move. You can look away from it; J chooses a new direction.
Turn it off to place it at a fixed point in the world. Existing preferences
load with Skybox anchor off.

Preferences are stored at `${user.home}/.skylyrics/preferences.json`, using the
same schema and token on both versions. A positive timing offset advances the
lyrics. To test without changing ordinary preferences, set
`-Dskylyrics.configDir=<directory>`; development launches already use
`test-runs/shared-config`.

## Reproducible Windows build

Requires Git, Node.js, PowerShell, network access and sufficient disk space.
The bootstrap installs portable tools into this project, with pinned URLs and
SHA-256 verification; it does not change the system Java installation.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/bootstrap.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Target all
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-bridge.ps1
node tools/verify-release.mjs
powershell -NoProfile -ExecutionPolicy Bypass -File tools/package-source.ps1
```

The two mod jars and single bridge bundle are placed in `dist/`. Build one target
with `-Target 1.21.11` or `-Target 26.2`. Tests run independently on the Java runtime
for that target, and the shared core always compiles with `--release 21`.

Launch isolated development clients (one at a time):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Target 1.21.11 -RunClient
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Target 26.2 -RunClient
```

Close the development client before rebuilding. Its shared core jar is on the
live classpath; replacing that jar during play can invalidate class loading.
Both runs use separate worlds/options/cache directories and shared preferences.
Demo mode starts automatically in these development runs.

The bridge build checks the Beautiful Lyrics revision, applies three small hooks
(bridge import, lyric-service fallback, resilient font loading), and uses the
original offline bundler. `Extension/deno.lock` records its dependency resolution.

## Source layout

| Directory | Responsibility |
|---|---|
| `core` | Clock, immutable lyrics/frames, protocol validation, configuration, shared spring/spline animation |
| `fabric-1.21.11` | Mojang mappings/remapping, world hooks, texture submission, settings, lifecycle |
| `fabric-26.2` | Unobfuscated Loom build, extraction and feature submission, settings, lifecycle |
| `spicetify-bridge` | Original Beautiful Lyrics event integration and shared local protocol |
| `test-fixtures` | Original demo, recorded messages, reference animation values |
| `presentation` | Backend-independent font metrics and preparation of transparent text masks, glow and blur |

Minecraft/Fabric classes and GPU resources are absent from `core`. Each adapter
attaches a prepared frame to Fabric's render state, then submits its prepared pixels
and geometry without consulting playback state during drawing. All GPU allocation
and texture upload use Minecraft/Blaze3D facilities. No raw OpenGL calls are used.

## Current limitations

- The original Beautiful Lyrics lyric-service domain returned NXDOMAIN
  during development. Preserved fonts are embedded locally in both builds and the bridge.
  Spotify-native lyrics are used when the original provider is unavailable; this
  fallback provides line/static timing, not fabricated syllable timing.
- The original lead/background typefaces and interlude calculations are ported.
  Unsupported Unicode glyphs use the system fallback. Complete browser-to-game
  visual fidelity and scrolling behavior still require final review.
- Text effects are prepared on the CPU and uploaded at up to 30 Hz. The tested
  OpenGL/Sodium configurations are listed in the verification ledger; performance
  on other hardware has not been measured.
- Sodium compatibility is tested separately for each Minecraft version. Footage,
  Vulkan testing, and Iris assessment were removed from the requested scope on
  23 October 2026.

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for pairing, clock, and reconnection details.

If an older bridge loads Spotify lyrics but leaves them frozen while audio
plays, replace it with the latest bundle from `dist` and run `spicetify apply`.
The updated clock reads public playback progress and keeps running through
temporary position-read failures.

For the live Spotify check, follow [docs/SPOTIFY-TEST.md](docs/SPOTIFY-TEST.md).

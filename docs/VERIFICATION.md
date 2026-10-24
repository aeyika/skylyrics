# Verification ledger

## Subsequent skybox and playback-clock update

Both targets now include an optional Skybox anchor. Shared placement tests cover
movement/teleportation and fixed-world behavior; preference tests cover older
configuration files and persistence through other setting changes. Both adapters
are rebuilt for their intended Java versions.
The bridge and Beautiful Lyrics use public playback progress, tested for
pause/resume, seeking/repeat and an initial progress-read failure with recovery.
Live Spotify behavior and the new skybox setting need a user retest after replacing
the installed files. The earlier in-game/Sodium matrix below predates this update.
Logs: `tools/build-skybox.log` and `tools/build-bridge-playback.log`.

## Original verification

Verified on 23 October 2026. Live Spotify testing is assigned to the user.
Multiplayer, demonstration footage, Vulkan and Iris testing are outside the
requested handoff. No compatibility claim is made for other versions or GPUs.

## Pinned build matrix

| Item | Minecraft 1.21.11 | Minecraft 26.2 |
|---|---|---|
| Java runtime | Temurin 21.0.12.1+1 | Temurin 25.0.4.1+1 |
| Adapter bytecode | Java 21 | Java 25 |
| Shared core bytecode | Java 21 | Java 21 |
| Fabric Loader | 0.20.1 | 0.20.1 |
| Fabric API | 0.141.6+1.21.11 | 0.161.0+26.2 |
| Loom | 1.17.13, remapping plugin | 1.17.13, unobfuscated plugin |
| Gradle | 9.5.1 | 9.5.1 |
| Mappings and packaging | Official Mojang; remapJar | Unobfuscated; jar |

Both builds independently run the shared clock, protocol, pairing/reconnection,
preferences and animation tests on their intended Java runtime. Presentation
tests check transparent masks, fractional alpha, effect toggles and glow spread.
The release audit checks class versions, metadata, entrypoints, nested core and
WebSocket dependencies, fonts, shaders, and exclusion of the test harness.

## Graphics and in-game checks

Hardware: Windows 11, NVIDIA GeForce RTX 3060 Ti, NVIDIA driver 566.36.
Both targets ran with OpenGL. These checks use real Minecraft clients and their
framebuffers through Fabric's client game-test API, in separate game directories.

| Configuration | 1.21.11 | 26.2 |
|---|---|---|
| Fabric API, no renderer mod | PASS | PASS |
| Sodium | 0.8.14+mc1.21.11: PASS | 0.9.2+mc26.2: PASS |

Both versions passed the following singleplayer checks:

- Visible world-space lyrics, transparent background and fractional alpha.
- Syllable demo, line-synced track changes, background/opposite vocals,
  three-dot interlude, long-line wrapping and representative Unicode.
- Settings, pairing-code controls, real local WebSocket authentication,
  lyric/track messages, pause, seek, repeat and token reconnection.
- Blur/glow toggles, terrain occlusion and the optional see-through mode.
- Window resize, fullscreen transitions and resource-pack reload.
- Recentering, first/third-person changes and world disconnect.

The local WebSocket peer in the automated test is a test client, not Spotify.
Live extension loading, Spotify playback and the provider fallback remain for
the user's [Spotify check](SPOTIFY-TEST.md). Shared clock tests cover resume;
the in-game sequence checks deterministic paused/seek/repeat positions.

Evidence in the working checkout:

- `tools/gametest-final-baseline.log`: both baseline clients.
- `tools/gametest-final-sodium.log`: both Sodium clients.
- `tools/gametest-final-sodium-1.21.11.log`: final 1.21.11 renderer check.
- `test-results/ingame-{version}/`: baseline screenshots, frames and results.
- `test-results/ingame-{version}-default-sodium/`: Sodium evidence.
- `tools/build-final.log`: independent final builds and unit tests.
- `tools/build-bridge-final.log`: bridge bundling and conversion tests.

An earlier dedicated-server smoke test also completed, but multiplayer is
excluded from this handoff and no multiplayer support claim is made here.
Authentication errors mentioning the Fabric test account/Realms are unrelated
to the loopback lyric bridge.

## Animation and cross-version comparisons

The shared spring and natural-cubic-spline port is checked against unmodified
Beautiful Lyrics calculations with a 1e-10 tolerance. Interlude intersections
are checked at 1e-8 and arc-length sampling at 1e-5 against curve-interpolator 3.3.1.
Preserved Bold/Medium fonts are used for lead/background vocals.

Actual runtime snapshots at 0, 1500, 3000, 9000, 15500, 21000 and 29000 ms
matched exactly between clients: 13,380 numeric fields, maximum difference 0,
permitted tolerance 1e-9. See `test-results/cross-version-frames.json`.
These cover positions, highlighting, scale/lift, opacity, blur/glow, alignment,
roles and panel placement. Screenshots were inspected for visible glyphs,
wrapping, interlude dots, soft effects and wall occlusion.

This is a port into a world-space panel, not a browser pixel-equivalence claim.
Java font rasterization and software Gaussian masks differ from Chromium CSS.
Panel scrolling uses deterministic smoothing; browser-native scrolling and CSS
opacity-transition timing are not reproduced exactly. Unsupported glyphs use
the system Dialog fallback. The GPU receives a prepared transparent texture,
refreshed at most 30 times per second; performance on other hardware is untested.

## Reproduce the checks

From the source root after bootstrap:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Target all
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-bridge.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/test-clients.ps1 -Target all
powershell -NoProfile -ExecutionPolicy Bypass -File tools/fetch-sodium.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/test-clients.ps1 -Target all -Compatibility sodium
node tools/compare-frames.mjs
node tools/verify-release.mjs
```

Sodium downloads are pinned and checked with SHA-512. The test script verifies
the files before launch, so selecting Sodium cannot silently run without it.
No footage is captured by default. Test-only classes stay out of release jars.
Scripts hold a shared run lock and reject rebuilding while a development client
is running. Close clients before starting another build or test command.

The source package excludes Minecraft files, dependency caches, development
worlds, credentials and portable tool binaries. Bootstrap downloads the pinned
tools and upstream revision again. `dist/SHA256SUMS.txt` records release hashes.


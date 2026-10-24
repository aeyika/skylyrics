# Sky Lyrics attribution

Beautiful Lyrics was created by **surfbryce**:
https://github.com/surfbryce/beautiful-lyrics

Sky Lyrics adapts Beautiful Lyrics' lyric presentation and animation for Minecraft.
Beautiful Lyrics remains credited in the mod metadata,
in-game settings, bridge bundle, and this source tree. This does not imply endorsement.

The upstream source is pinned to commit
`61ac582da092311e893423269ca7f09003108705` (Beautiful Lyrics 5.1.0).
The original source is preserved under `upstream/beautiful-lyrics`.
The bridge adds an import to `Extension/Source/main.ts`; the original views/styles remain.

Ported calculations originate in:

- `Universal/Modules/LegacySpring.ts`
- `Extension/Source/Modules/LyricsRenderer/Components/SyllableVocals.ts`
- `Extension/Source/Modules/LyricsRenderer/Components/LineVocals.ts`
- `Extension/Source/Modules/LyricsRenderer/LyricsScroller.ts`
- `Extension/Source/Stylings/Lyrics.scss`

Original lyrics in `test-fixtures/demo.json` were written for Sky Lyrics.
They are not transcriptions of a commercial recording.

Fabric, Minecraft, Java-WebSocket, Gson and Deno are their respective authors' work.
Their applicable licenses and metadata remain in dependencies. Minecraft game files
and Mojang's complete mappings must not be redistributed in a Sky Lyrics release.

The original font host failed DNS resolution during development. Preserved
`LyricsRegular`, `LyricsMedium`, `LyricsSemibold`, and `LyricsBold` WOFF2 files
were obtained from https://github.com/awdev1/beautitifullyricsfont at revision
`4d4ae4105dcd91a405d20f4cb818362287938c44`; see `tools/fonts.lock.json` for
checksums. The bridge embeds those files; Java uses lossless WOFF2-to-SFNT
conversion via wawoff2 2.0.1. Unsupported Unicode glyphs use the system Dialog
fallback. Matching file names from the preservation repository do not by
themselves establish complete visual fidelity; comparison remains required.

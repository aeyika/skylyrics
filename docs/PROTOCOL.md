# Shared bridge protocol v1

Both Minecraft versions listen at `ws://127.0.0.1:19584`. No LAN bind, Spotify
credentials, cookies, or access tokens are exposed by the bridge. The only stored
credential is a random local bridge token. Spotify's token remains within the
original Beautiful Lyrics service integration.

Every client message is a JSON object with `v: 1`, `type`, and monotonically
increasing integer `seq`. A new WebSocket starts its own sequence at zero.
The server rejects unsupported versions, replayed sequences, malformed data,
non-finite times, out-of-range timings, and stale-track lyric/playback messages.
Text messages have a 2 MiB application limit. The server binds to IPv4 loopback.
Browser origins must be `https://xpui.app.spotify.com`; non-browser local test
clients may omit Origin. One authenticated bridge is active at a time.

1. In Minecraft choose Pair. A six-digit, one-use code expires in two minutes
   and permits at most five guesses.
2. Spotify sends `{"v":1,"type":"hello","seq":0,"code":"123456"}`.
3. The server sends `{"v":1,"type":"welcome","token":"<64 hex characters>"}`.
4. The extension stores that token in localStorage. Future connections send
   `token` instead of `code`. Reconnection replays track, lyrics, and playback.

Messages after authentication:

| type | Fields |
|---|---|
| `track` | `trackId`, `title`, `durationMs` |
| `playback` | `trackId`, `positionMs`, `durationMs`, `playing`, `sentAtMs` |
| `lyrics` | `trackId`, `sync` (`STATIC`, `LINE`, `SYLLABLE`), `lines` |
| `ping` | no additional fields; receives `pong` with matching `seq` |

Each line has `startMs`, `endMs`, `alignment` (`LEFT`/`RIGHT`),
`role` (`LEAD`/`BACKGROUND`/`INTERLUDE`), and `segments`.
Each segment has `text`, `startMs`, `endMs`, and `partOfWord`.
All intervals are positive, finite, ordered, and bounded to 24 hours.

Playback anchors use `System.nanoTime`. The receiver compensates for up to two
seconds of local message age on arrival; pausing never adds message age.
Every update re-anchors the clock, so seeking and repeat require no special
message. Offsets apply at snapshot time. Disconnect freezes the last clock.
The extension sends updates at 250 ms intervals and on explicit playback events,
with a two-second heartbeat. Reconnection uses exponential backoff up to 15 s
with jitter. A rejected credential requires pairing again.

Configuration v1 lives at `${user.home}/.skylyrics/preferences.json` for both
versions, or at `-Dskylyrics.configDir=<directory>`. Only ordinary preferences
and the local token are stored there; textures and buffers are disposable.

# Live Spotify check

The user will perform this check. Automated bridge tests use a real local
WebSocket connection, but do not establish that the extension works inside the
user's Spotify installation.

1. Install the matching mod jar, Fabric Loader and Fabric API from the README.
2. Run `spicetify path -e root`, then copy `beautiful-lyrics-skylyrics.mjs` from
   `dist` into the extension directory it prints. On this computer that is
   `C:\Users\ItsDa\AppData\Local\spicetify\Extensions`.
3. Disable other Beautiful Lyrics/lyrics replacement extensions, enable this
   bundle with `spicetify config extensions beautiful-lyrics-skylyrics.mjs`, and
   run `spicetify apply`.
4. Enter a singleplayer world. Press K, then **Pair with Spotify**. In Spotify's
   profile menu select **Pair Sky Lyrics…** and enter Minecraft's six-digit code.
   The code expires after two minutes.
5. Play a track with Spotify lyrics. Check that the title and lyrics change with
   the track, pause freezes the highlight, resume continues, and seeking/repeating
   moves the lyrics to the new position. J recenters the panel.
6. Close and reopen Minecraft using the other supported version. The same
   extension and saved pairing token should reconnect automatically. Do not run
   both clients at once: they use the same local bridge port.

The original Beautiful Lyrics provider was unreachable during development.
The bundled fallback uses the logged-in Spotify client's native lyric service.
It preserves line or static timing; it cannot supply syllable timing when the
provider returns only line timing. F8 demonstrates the bundled syllable lyrics
without requiring Spotify or a lyric-service connection.

If pairing fails, keep Minecraft open in a world, request a fresh code, then
retry from Spotify's menu. Check Minecraft's `logs/latest.log` for Sky Lyrics
errors. A track with no available lyrics intentionally clears the old lyrics.

If audio plays and Spotify's progress bar moves but lyrics stay frozen, copy the
updated `dist/beautiful-lyrics-skylyrics.mjs` over the old extension file, then
run `spicetify apply` and test pause/resume and seeking again. The updated bundle
starts its animation clock without waiting on an internal position request.
Existing pairing is retained.

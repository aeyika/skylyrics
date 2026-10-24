import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const upstream = path.join(root, 'upstream/beautiful-lyrics/Extension');
const entry = path.join(upstream, 'Source/main.ts');
let main = fs.readFileSync(entry, 'utf8').replaceAll('\r\n', '\n');
const bridgeImport = "import '../../../../spicetify-bridge/src/bridge.ts';";
if (!main.includes(bridgeImport)) main = bridgeImport + '\n' + main;
main = main.replace('font.load().then(font => document.fonts.add(font))',
  'font.load().then(font => document.fonts.add(font)).catch(() => document.fonts.ready)');
for(const weight of ['Regular','Medium','Semibold','Bold']){
  const bytes=fs.readFileSync(path.join(root,`presentation/src/main/resources/assets/skylyrics/fonts/Lyrics${weight}.woff2`));
  main=main.replace(`https://fonts.socalifornian.live/Lyrics${weight}.woff2`,`data:font/woff2;base64,${bytes.toString('base64')}`);
}
fs.writeFileSync(entry, main);
const playerFile = path.join(upstream, 'Spices/Spicetify/Services/Player/mod.ts');
let player = fs.readFileSync(playerFile, 'utf8').replaceAll('\r\n', '\n');
if (!player.includes('loadProviderLyrics')) {
  const startMarker = '\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\tGetSpotifyAccessToken()';
  const endMarker = '\n\t\t\t\t\t\t.then(\n\t\t\t\t\t\t\t(providerLyrics) => {';
  const start = player.indexOf(startMarker);
  const end = player.indexOf(endMarker, start);
  if (start < 0 || end < 0) throw new Error('Pinned upstream lyric provider hook changed');
  const provider = path.relative(path.dirname(playerFile),path.join(root,'spicetify-bridge/src/provider.ts')).replaceAll('\\','/');
  player = `import { loadProviderLyrics } from '${provider}';\n` + player.slice(0,start)
    + '\n\t\t\t\t\t\tGetSpotifyAccessToken().then(token => loadProviderLyrics(songAtUpdate.Id, songAtUpdate.Duration * 1000, token))'
    + player.slice(end);
  fs.writeFileSync(playerFile,player);
}
if (!player.includes('// Sky Lyrics public playback clock')) {
  const start = player.indexOf('\t\t\tconst syncTimings =');
  const end = player.indexOf('\n\t\t\t// Handle frame updating', start);
  if (start < 0 || end < 0) throw new Error('Pinned upstream playback clock hook changed');
  const playback = path.relative(path.dirname(playerFile),path.join(root,'spicetify-bridge/src/playback.mjs')).replaceAll('\\','/');
  const replacement = `\t\t\t// Sky Lyrics public playback clock: never gate animation on a private Spotify request.
\t\t\tconst RequestPositionSync = () => {
\t\t\t\tconst sample = readSpotifyPlayback(SpotifyPlayer)
\t\t\t\tif (typeof sample.playing === 'boolean' && sample.playing !== IsPlaying) {
\t\t\t\t\tIsPlaying = sample.playing
\t\t\t\t\tIsPlayingChangedSignal.Fire()
\t\t\t\t}
\t\t\t\tif (sample.positionMs !== undefined) {
\t\t\t\t\tsyncedPosition = IsPlaying
\t\t\t\t\t\t? { Position: sample.positionMs, StartedSyncAt: performance.now() }
\t\t\t\t\t\t: { Position: sample.positionMs }
\t\t\t\t}
\t\t\t\tPlayerMaid.Give(Timeout(1 / 30, RequestPositionSync), 'TimestampPositionSync')
\t\t\t\treturn Promise.resolve()
\t\t\t}
`;
  player = `import { readSpotifyPlayback } from '${playback}';\n` + player.slice(0,start) + replacement + player.slice(end);
  player = player.replace('const isPaused = SpotifyPlayer.data.isPaused', 'const isPaused = !SpotifyPlayer.isPlaying()');
}
fs.writeFileSync(playerFile,player);
console.log('Applied the pinned Beautiful Lyrics bridge/provider/font/playback hooks');

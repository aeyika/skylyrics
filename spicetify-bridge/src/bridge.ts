// The bridge is bundled into Beautiful Lyrics, sharing its already loaded lyric data.
import * as Player from '../../upstream/beautiful-lyrics/Extension/Spices/Spicetify/Services/Player/mod.ts';
import { GlobalMaid, OnSpotifyReady } from '../../upstream/beautiful-lyrics/Extension/Spices/Spicetify/Services/Session.ts';
import { normalizeLyrics } from './normalize.mjs';
import { SpotifyPlaybackStream } from './playback.mjs';
import { Maid } from '../../upstream/beautiful-lyrics/Universal/Modules/Maid.ts';

const TOKEN_KEY = 'skylyrics.bridge.v1.token';
const PORT_KEY = 'skylyrics.bridge.v1.port';
let socket: WebSocket | undefined;
let connected = false, stopped = false, sequence = 0, retry = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
let pairingCode: string | undefined;
let generation = 0;
const playbackStream=new SpotifyPlaybackStream();
let lastLyrics='';
const bridgeMaid=new Maid();
const instanceKey=Symbol.for('skylyrics.bridge.v1.instance');
const instances=globalThis as any;
instances[instanceKey]?.();
instances[instanceKey]=()=>bridgeMaid.Destroy();
GlobalMaid.Give(bridgeMaid);
bridgeMaid.Give(()=>{stopped=true;++generation;if(timer)clearTimeout(timer);socket?.close();});

function notify(message: string) {
  (globalThis as any).Spicetify?.showNotification(`Sky Lyrics: ${message}`);
}
function send(type: string, payload: Record<string, unknown> = {}) {
  if (!connected || socket?.readyState !== WebSocket.OPEN) return;
  socket.send(JSON.stringify({ v: 1, type, seq: ++sequence, ...payload }));
}
function track() {
  const song = Player.Song;
  send('track', {
    trackId: song?.Uri ?? '', title: Player.SongDetails?.Name ?? 'Spotify',
    durationMs: song && song.Type !== 'DJ' ? song.Duration * 1000 : 0,
  });
}
function playback() {
  const song = Player.Song;
  if (!song || song.Type === 'DJ') return;
  const current=playbackStream.sample(song,(globalThis as any).Spicetify?.Player);
  if(current)send('playback',{...current,sentAtMs:Date.now()});
}
function lyrics() {
  if (!Player.Song || Player.Song.Type === 'DJ') return;
  // Empty documents explicitly clear stale lyrics when a track has none.
  const document=Player.SongLyrics ? normalizeLyrics(Player.Song.Uri,Player.SongLyrics,Player.Song.Duration*1000)
    : {trackId:Player.Song.Uri,sync:'LINE',lines:[]};
  const identity=JSON.stringify(document);
  if(connected&&identity!==lastLyrics){send('lyrics',document);lastLyrics=identity;}
}
function synchronize() { track(); lyrics(); playback(); }
function connect() {
  if (stopped) return;
  if (timer) clearTimeout(timer);
  const current = ++generation;
  connected = false;
  socket?.close();
  const port = Number(localStorage.getItem(PORT_KEY) ?? '19584');
  if (!Number.isInteger(port) || port < 1024 || port > 65535) { notify('Invalid local port'); return; }
  const peer = socket = new WebSocket(`ws://127.0.0.1:${port}`);
  peer.onopen = () => {
    sequence = 0;
    const token = localStorage.getItem(TOKEN_KEY);
    peer.send(JSON.stringify({ v: 1, type: 'hello', seq: 0, ...(pairingCode ? { code: pairingCode } : { token: token ?? '' }) }));
  };
  peer.onmessage = event => {
    if (current !== generation) return;
    try {
      const message = JSON.parse(event.data);
      if (message.v !== 1 || message.type !== 'welcome' || !/^[0-9a-f]{64}$/.test(message.token)) return;
      localStorage.setItem(TOKEN_KEY, message.token);
      pairingCode = undefined; connected = true; retry = 0;
      lastLyrics='';notify('Connected to Minecraft'); synchronize();
    } catch { /* Ignore malformed server responses. */ }
  };
  peer.onclose = event => {
    if (current !== generation || stopped) return;
    connected = false;
    if (event.code === 1008) {
      pairingCode = undefined;
      notify('Open Sky Lyrics settings in Minecraft, then choose Pair Sky Lyrics in Spotify.');
      return;
    }
    timer = setTimeout(connect, Math.min(15000, 500 * 2 ** Math.min(retry++, 5)) + Math.random() * 250);
  };
  peer.onerror = () => { /* onclose owns reconnect scheduling. */ };
}
function showPairing() {
  const api = (globalThis as any).Spicetify;
  const container = document.createElement('div');
  const label = document.createElement('p');
  label.textContent = 'In Minecraft, open Sky Lyrics settings and click Pair. Enter the six-digit code shown there.';
  const input = document.createElement('input');
  input.placeholder = 'Pairing code'; input.inputMode = 'numeric'; input.maxLength = 6; input.autocomplete = 'off';
  input.style.cssText = 'padding:12px;font-size:24px;letter-spacing:8px;width:100%;color:black;background:white';
  const button = document.createElement('button'); button.textContent = 'Connect to Minecraft';
  button.style.cssText = 'margin-top:16px;padding:12px;border-radius:24px;color:black;background:#a5f3cd';
  const submit = () => {
    if (!/^\d{6}$/.test(input.value)) { notify('Enter six digits'); return; }
    pairingCode = input.value; retry = 0; connect(); api.PopupModal.hide();
  };
  button.onclick = submit; input.onkeydown = event => { if (event.key === 'Enter') submit(); };
  container.append(label, input, button);
  api.PopupModal.display({ title: 'Pair Sky Lyrics', content: container, isLarge: false });
  input.focus();
}

OnSpotifyReady.then(() => {
  if(stopped)return;
  const api = (globalThis as any).Spicetify;
  const menu = new api.Menu.Item('Pair Sky Lyrics…', false, showPairing);
  menu.register();
  bridgeMaid.Give(() => menu.deregister());
  bridgeMaid.Give(Player.SongChanged.Connect(synchronize));
  bridgeMaid.Give(Player.SongDetailsLoaded.Connect(track));
  bridgeMaid.Give(Player.SongLyricsLoaded.Connect(lyrics));
  bridgeMaid.Give(Player.IsPlayingChanged.Connect(playback));
  const playbackTimer = setInterval(playback, 250);
  const heartbeat = setInterval(() => send('ping'), 2000);
  bridgeMaid.Give(() => {
    stopped = true; ++generation; if (timer) clearTimeout(timer); clearInterval(heartbeat); clearInterval(playbackTimer); socket?.close();
  });
  connect();
});


import test from 'node:test';
import assert from 'node:assert/strict';
import { readSpotifyPlayback, SpotifyPlaybackStream } from '../src/playback.mjs';
import fs from 'node:fs';
import vm from 'node:vm';

test('bridge uses public progress and duration, detects seek/repeat and rejects mismatched track samples',()=>{
  let at=0,position=95504,playing=true,uri='spotify:track:a';
  const song={Uri:uri,Duration:231.546};
  const player={getProgress:()=>position,isPlaying:()=>playing,getDuration:()=>229823,data:{item:{get uri(){return uri;}}}};
  const stream=new SpotifyPlaybackStream(()=>at);
  assert.equal(stream.sample(song,player).durationMs,229823);
  for(let i=0;i<20;i++){at+=250;position+=250;const sample=stream.sample(song,player);assert.equal(sample.positionMs,position);assert.equal(sample.discontinuity,false);}
  playing=false;assert.equal(stream.sample(song,player).playing,false);at+=2000;playing=true;assert.equal(stream.sample(song,player).discontinuity,false);
  position=180000;assert.equal(stream.sample(song,player).discontinuity,true);
  position=0;assert.equal(stream.sample(song,player).discontinuity,true);
  uri='spotify:track:b';assert.equal(stream.sample(song,player),null,'Do not tag a new track position with an old track id');
});

test('a failed public progress sample is skipped instead of sending stale upstream time',()=>{
  const stream=new SpotifyPlaybackStream(()=>0);
  assert.equal(stream.sample({Uri:'a',Duration:30},{isPlaying:()=>true,getProgress(){throw new Error('Unavailable');}}),null);
});

test('public playback tracks pause, resume, seeking and repeat', () => {
  let position = 1000, playing = true;
  const player = { getProgress: () => position, isPlaying: () => playing, data: { isPaused: true } };
  assert.deepEqual(readSpotifyPlayback(player), { positionMs: 1000, playing: true });
  playing = false; assert.equal(readSpotifyPlayback(player).playing, false);
  position = 18000; assert.equal(readSpotifyPlayback(player).positionMs, 18000);
  playing = true; position = 0; assert.deepEqual(readSpotifyPlayback(player), { positionMs: 0, playing: true });
});
test('failed initial progress read retains playback state and later recovers', () => {
  let ready = false;
  const player = { isPlaying: () => true, getProgress: () => { if (!ready) throw new Error('Not ready'); return 2500; } };
  assert.deepEqual(readSpotifyPlayback(player), { positionMs: undefined, playing: true });
  ready = true; assert.deepEqual(readSpotifyPlayback(player), { positionMs: 2500, playing: true });
});
test('unavailable and malformed progress do not poison the lyric clock', () => {
  for (const value of [NaN, Infinity, -1, '1200', undefined]) {
    assert.equal(readSpotifyPlayback({ getProgress: () => value, data: { isPaused: false } }).positionMs, undefined);
  }
  assert.deepEqual(readSpotifyPlayback(undefined), { positionMs: undefined, playing: undefined });
});

test('patched upstream starts its animation after a failed first read and keeps polling', async () => {
  const source = fs.readFileSync(new URL('../../upstream/beautiful-lyrics/Extension/Spices/Spicetify/Services/Player/mod.ts', import.meta.url), 'utf8');
  const marker = source.indexOf('// Sky Lyrics public playback clock');
  assert.ok(marker >= 0, 'Run tools/build-bridge.ps1 to apply the reproducible upstream hook');
  const start = source.indexOf('const RequestPositionSync =', marker);
  const end = source.indexOf('// Handle frame updating', start);
  let ready = false, next;
  const state = {
    readSpotifyPlayback,
    SpotifyPlayer: { isPlaying: () => true, getProgress: () => { if (!ready) throw new Error('Not ready'); return 2500; } },
    IsPlaying: false, syncedPosition: undefined,
    IsPlayingChangedSignal: { Fire() {} }, performance: { now: () => 100 },
    Timeout: (_delay, callback) => { next = callback; return {}; }, PlayerMaid: { Give() {} },
  };
  await vm.runInNewContext(source.slice(start, end) + '\nRequestPositionSync()', state);
  assert.equal(state.IsPlaying, true); assert.equal(state.syncedPosition, undefined);
  assert.equal(typeof next, 'function', 'Polling must survive the first failure');
  ready = true; await next(); assert.equal(state.syncedPosition.Position, 2500);
});

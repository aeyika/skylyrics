import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeLyrics } from '../src/normalize.mjs';
import { fromSpotifyLyrics } from '../src/spotify-lyrics.mjs';
test('syllables retain word boundaries, opposite alignment and background vocals', () => {
  const syllables = [{ Text: 'sky', StartTime: 1, EndTime: 3, IsPartOfWord: false }];
  const doc = normalizeLyrics('track', { Type: 'Syllable', NaturalAlignment: 'Left', Content: [{
    Type: 'Vocal', OppositeAligned: true,
    Lead: { StartTime: 1, EndTime: 3, Syllables: syllables },
    Background: [{ StartTime: 2, EndTime: 3, Syllables: [{ ...syllables[0], StartTime: 2 }] }],
  }] });
  assert.equal(doc.lines[0].alignment, 'RIGHT');
  assert.equal(doc.lines[0].segments[0].endMs, 3000);
  assert.equal(doc.lines[1].role, 'BACKGROUND');
});
test('line, static and interlude documents use one protocol', () => {
  const doc = normalizeLyrics('track', { Type: 'Line', NaturalAlignment: 'Right', Content: [
    { Type: 'Interlude', StartTime: 0, EndTime: 2 },
    { Type: 'Vocal', Text: 'café 雲 ✨', StartTime: 2, EndTime: 4, OppositeAligned: false },
  ] });
  assert.equal(doc.lines[0].role, 'INTERLUDE'); assert.equal(doc.lines[1].alignment, 'RIGHT');
  assert.equal(doc.lines[1].segments[0].text, 'café 雲 ✨');
  const plain = normalizeLyrics('track', { Type: 'Static', Lines: [{ Text: 'Original demo' }] }, 5000);
  assert.equal(plain.sync, 'STATIC'); assert.equal(plain.lines[0].endMs, 5000);
});
test('Spotify-native fallback preserves line timing and does not invent syllables', () => {
  const source = { lyrics: { syncType: 'LINE_SYNCED', lines: [
    { startTimeMs: '1000', endTimeMs: '0', words: 'First original line' },
    { startTimeMs: '3500', endTimeMs: '0', words: 'Second original line' },
  ] } };
  const provider = fromSpotifyLyrics(source, 8000);
  assert.equal(provider.Type, 'Line');
  assert.equal(provider.Content[0].EndTime, 3.5);
  assert.equal(provider.Content[1].EndTime, 8);
  assert.equal(normalizeLyrics('track', provider).sync, 'LINE');
  assert.equal(fromSpotifyLyrics({}, 8000), undefined);
});

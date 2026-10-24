/** Convert Spotify's lyric response into Beautiful Lyrics' original provider schema. */
export function fromSpotifyLyrics(response, durationMs) {
  const source = response?.lyrics;
  if (!source || !Array.isArray(source.lines) || source.lines.length === 0) return undefined;
  if (source.syncType !== 'LINE_SYNCED') {
    return { Type: 'Static', Lines: source.lines.filter(l => l.words?.trim()).map(l => ({ Text: l.words })) };
  }
  const valid = source.lines.filter(l => Number.isFinite(Number(l.startTimeMs)) && l.words?.trim());
  const content = valid.map((line, i) => {
    const start = Number(line.startTimeMs);
    const declared = Number(line.endTimeMs);
    const next = i + 1 < valid.length ? Number(valid[i + 1].startTimeMs) : durationMs;
    return { Type: 'Vocal', Text: line.words, StartTime: start / 1000,
      EndTime: Math.max(start + 1, declared > start ? Math.min(declared, next) : next) / 1000,
      OppositeAligned: false };
  }).filter(l => l.StartTime >= 0 && l.EndTime > l.StartTime);
  return content.length ? { Type: 'Line', StartTime: content[0].StartTime,
    EndTime: content.at(-1).EndTime, Content: content } : undefined;
}

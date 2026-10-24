/** Translate Beautiful Lyrics' public transformed document, preserving roles and alignment. */
export function normalizeLyrics(trackId, lyrics, durationMs = 86400000) {
  const lines = [];
  const ms = n => Number(n) * 1000;
  const alignment = opposite => (lyrics.NaturalAlignment === 'Right') !== !!opposite ? 'RIGHT' : 'LEFT';
  const add = (vocal, role, opposite, text) => {
    const startMs = ms(vocal.StartTime ?? 0);
    const endMs = ms(vocal.EndTime ?? durationMs / 1000);
    if (!(endMs > startMs)) return;
    const segments = vocal.Syllables ? vocal.Syllables.map(s => ({
      text: s.Text, startMs: ms(s.StartTime), endMs: ms(s.EndTime), partOfWord: !!s.IsPartOfWord,
    })) : [{ text: text ?? vocal.Text ?? '•••', startMs, endMs, partOfWord: false }];
    if (segments.length) lines.push({ startMs, endMs, alignment: alignment(opposite), role, segments });
  };
  if (lyrics.Type === 'Static') {
    for (const line of lyrics.Lines ?? []) add({ StartTime: 0, EndTime: durationMs / 1000 }, 'LEAD', false, line.Text);
  } else {
    for (const item of lyrics.Content ?? []) {
      if (item.Type === 'Interlude') add(item, 'INTERLUDE', false, '•••');
      else if (lyrics.Type === 'Syllable') {
        add(item.Lead, 'LEAD', item.OppositeAligned);
        for (const background of item.Background ?? []) add(background, 'BACKGROUND', item.OppositeAligned);
      } else add(item, 'LEAD', item.OppositeAligned);
    }
  }
  lines.sort((a, b) => a.startMs - b.startMs || (a.role === 'BACKGROUND' ? 1 : -1));
  return { trackId, sync: lyrics.Type.toUpperCase(), lines };
}


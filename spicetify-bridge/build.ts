// Run from upstream/beautiful-lyrics/Extension, using its pinned Deno import map.
import Bundle from '../upstream/beautiful-lyrics/Extension/Spices/Build/Bundle.ts';
const code = await Bundle({ Type: 'Offline' });
await Deno.mkdir('../../../dist', { recursive: true });
await Deno.writeTextFile('../../../dist/beautiful-lyrics-skylyrics.mjs',
  '/* Beautiful Lyrics by surfbryce. Sky Lyrics bridge adaptation. See ATTRIBUTION.md. */\n' + code);
Deno.exit(0);

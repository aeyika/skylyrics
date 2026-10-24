import { fromSpotifyLyrics } from './spotify-lyrics.mjs';

/** Keep the original provider first; fall back to lyrics Spotify already exposes to this account. */
export async function loadProviderLyrics(trackId: string, durationMs: number, token: string) {
  try {
    const original = await fetch(`https://beautiful-lyrics.socalifornian.live/lyrics/${encodeURIComponent(trackId)}`, {
      headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(4000),
    });
    if (original.ok) {
      const text = await original.text();
      if (text.length) return JSON.parse(text);
    }
  } catch { /* Original service can be unavailable; presentation still works. */ }
  try {
    const api = (globalThis as any).Spicetify;
    const response = await api.CosmosAsync.get(`https://spclient.wg.spotify.com/color-lyrics/v2/track/${encodeURIComponent(trackId)}?format=json&vocalRemoval=false`);
    return fromSpotifyLyrics(response, durationMs);
  } catch {
    // Do not invent timings or fabricate syllable data when a provider has no lyrics.
    return undefined;
  }
}

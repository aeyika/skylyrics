/** Read public playback methods independently; temporary progress failures must not stop animation. */
export function readSpotifyPlayback(player) {
  let playing, positionMs, durationMs, trackId;
  try {
    const value = player?.isPlaying?.();
    if (typeof value === 'boolean') playing = value;
  } catch { /* Keep the last known state until Spotify is ready again. */ }
  if (playing === undefined && typeof player?.data?.isPaused === 'boolean') playing = !player.data.isPaused;
  try {
    const value = player?.getProgress?.();
    if (typeof value === 'number' && Number.isFinite(value) && value >= 0) positionMs = value;
  } catch { /* The lyric clock can continue from its previous position. */ }
  try{const duration=player?.getDuration?.();if(Number.isFinite(duration)&&duration>0)durationMs=duration;}catch{}
  const uri=player?.data?.item?.uri??player?.data?.track?.uri;
  if(typeof uri==='string'&&uri.length<=256&&uri)trackId=uri;
  return { playing, positionMs, ...(durationMs===undefined?{}:{durationMs}), ...(trackId?{trackId}:{}) };
}

/** One coherent public Spotify sample; never substitute the stale upstream clock. */
export class SpotifyPlaybackStream {
  constructor(now=()=>performance.now()){this.now=now;this.previous=undefined;}
  sample(song,player){
    const current=readSpotifyPlayback(player),now=this.now();
    if(current.positionMs===undefined||current.playing===undefined||(current.trackId&&current.trackId!==song.Uri))return null;
    const duration=current.durationMs??song.Duration*1000;
    if(!Number.isFinite(duration)||duration<=0)return null;
    const position=Math.max(0,Math.min(duration,current.positionMs)),previous=this.previous;
    const discontinuity=!!previous&&previous.trackId===song.Uri&&Math.abs(position-(previous.positionMs+(previous.playing?Math.max(0,now-previous.at):0)))>=1000;
    this.previous={trackId:song.Uri,positionMs:position,playing:current.playing,at:now};
    return {trackId:song.Uri,positionMs:position,durationMs:duration,playing:current.playing,discontinuity};
  }
}

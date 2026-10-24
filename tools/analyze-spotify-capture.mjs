import fs from 'node:fs';
const file=process.argv[2]??'test-results/live-spotify/capture.jsonl';
const rows=fs.readFileSync(file,'utf8').trim().split('\n').filter(Boolean).map(JSON.parse);
const wire=rows.filter(r=>r.kind==='wire');
const samples=rows.filter(r=>r.kind==='sample');
const playback=wire.filter(r=>r.message.type==='playback');
let lastSample,backsteps=0,maxDifference=0,lastPlayback;
for(const row of rows){
  if(row.kind==='sample')lastSample=row;
  if(row.kind==='wire'&&row.message.type==='playback'){
    if(lastSample&&lastSample.track===row.message.track)maxDifference=Math.max(maxDifference,Math.abs(lastSample.positionMs-row.message.positionMs));
    if(lastPlayback&&lastPlayback.message.track===row.message.track&&row.message.playing&&lastPlayback.message.playing&&row.message.positionMs<lastPlayback.message.positionMs-1)backsteps++;
    lastPlayback=row;
  }
}
const result={samples:samples.length,playbackMessages:playback.length,backsteps,maxPublicVsWireDifferenceMs:maxDifference,
  sockets:[...new Set(wire.map(r=>r.socketId))].length,helloMessages:wire.filter(r=>r.message.type==='hello').length,
  lyricsMessages:wire.filter(r=>r.message.type==='lyrics').length,lyricDocuments:[...new Set(wire.filter(r=>r.message.type==='lyrics').map(r=>r.message.document))].length,
  playingSamples:samples.filter(r=>r.playing).length,pausedSamples:samples.filter(r=>!r.playing).length,
  commands:rows.filter(r=>r.kind==='command').map(r=>r.action)};
console.log(JSON.stringify(result,null,2));
fs.writeFileSync(file.replace(/\.jsonl$/,'.summary.json'),JSON.stringify(result,null,2));

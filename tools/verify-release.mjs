import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const jdk=fs.readdirSync(path.join(root,'tools/jdk25'))[0];
const jar=path.join(root,'tools/jdk25',jdk,'bin/jar.exe');
for(const target of ['1.21.11','26.2']){
  const file=path.join(root,'dist',`skylyrics-0.1.0+mc${target}.jar`);
  const entries=execFileSync(jar,['tf',file],{encoding:'utf8'});
  for(const entry of ['fabric.mod.json','dev/skylyrics/fabric/SkyLyricsClient.class','dev/skylyrics/fabric/SkyRenderer.class','dev/skylyrics/fabric/SettingsScreen.class','dev/skylyrics/presentation/PanelRasterizer.class']){
    if(!entries.split(/\r?\n/).includes(entry))throw new Error(`${file} is missing ${entry}`);
  }
  if(!/META-INF\/jars\/.*core.*\.jar/.test(entries))throw new Error(`Missing nested shared core in ${target}`);
  if(!/META-INF\/jars\/Java-WebSocket/.test(entries))throw new Error(`Missing nested WebSocket dependency in ${target}`);
  console.log(`Verified packaged entrypoints, renderer, settings, core and WebSocket dependency: ${path.basename(file)}`);
}
const bridge=fs.readFileSync(path.join(root,'dist/beautiful-lyrics-skylyrics.mjs'),'utf8');
for(const expected of ['Pair Sky Lyrics','19584','color-lyrics/v2/track','skylyrics.bridge.v1.token','getProgress','isPlaying']){
  if(!bridge.includes(expected))throw new Error(`Missing bridge feature: ${expected}`);
}
console.log('Verified bridge-enabled Beautiful Lyrics bundle features');

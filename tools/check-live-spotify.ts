const directory='test-results/live-spotify';
const key=(await Deno.readTextFile(`${directory}/control-key`)).trim();
const observations:any[]=[];
let originalRepeat:number|undefined;
const delay=(ms:number)=>new Promise(resolve=>setTimeout(resolve,ms));
async function status(){return (await (await fetch('http://127.0.0.1:19585/status')).json()).lastSample;}
async function command(action:string,values={}){
  const response=await fetch('http://127.0.0.1:19585/control',{method:'POST',headers:{'content-type':'application/json','x-skylyrics-key':key},body:JSON.stringify({action,...values})});
  if(!response.ok)throw new Error(`Control failed: ${response.status}`);
}
async function observe(phase:string){const sample=await status();observations.push({phase,...sample});console.log(JSON.stringify({phase,positionMs:sample.positionMs,playing:sample.playing}));return sample;}
function require(value:boolean,message:string){if(!value)throw new Error(message);}
try{
  await command('play');await delay(1000);const first=await observe('playing-start');originalRepeat=first.repeat;
  await delay(8000);const advanced=await observe('playing-advanced');require(advanced.positionMs-first.positionMs>6500,'Spotify did not advance');
  await command('pause');await delay(1000);const paused=await observe('paused');await delay(2500);const frozen=await observe('paused-later');require(!frozen.playing&&Math.abs(frozen.positionMs-paused.positionMs)<100,'Pause did not freeze');
  await command('play');await delay(3000);const resumed=await observe('resumed');require(resumed.playing&&resumed.positionMs-frozen.positionMs>2000,'Resume did not advance');
  await command('seek',{positionMs:18000});await delay(3000);const sought=await observe('seek');require(Math.abs(sought.positionMs-21000)<1200,'Seek did not land');
  await command('repeat',{mode:2});await delay(500);await command('seek',{positionMs:Math.max(1000,sought.durationMs-3000)});await delay(7000);const repeated=await observe('repeat');require(repeated.track===sought.track&&repeated.positionMs<10000&&repeated.playing,'Natural repeat did not return to track start');
  await Deno.writeTextFile(`${directory}/playback-check.json`,JSON.stringify({status:'pass',observations},null,2));
}finally{if(originalRepeat!==undefined)await command('repeat',{mode:originalRepeat});await command('restore');}

// Local probe server; records no tokens, titles or lyric text.
const directory='test-results/live-spotify';
await Deno.mkdir(directory,{recursive:true});
const key=crypto.randomUUID();await Deno.writeTextFile(`${directory}/control-key`,key);
const prelude=(await Deno.readTextFile('tools/spotify-diagnostics.js')).replace('__DIAGNOSTIC_KEY__',key);
await Deno.writeTextFile(`${directory}/prelude.js`,prelude);
const log=await Deno.open(`${directory}/capture.jsonl`,{create:true,write:true,truncate:true});
const encoder=new TextEncoder();let peer:WebSocket|undefined,lastSample:unknown,events=0;
const server=Deno.serve({hostname:'127.0.0.1',port:19585,onListen(){console.log('Spotify probe ready on loopback port 19585');}},async request=>{
  const url=new URL(request.url);
  if(url.pathname==='/status')return Response.json({connected:peer?.readyState===WebSocket.OPEN,events,lastSample});
  if(url.pathname==='/probe'&&request.headers.get('upgrade')==='websocket'){
    const origin=request.headers.get('origin');
    if(url.searchParams.get('key')!==key||(origin&&origin!=='https://xpui.app.spotify.com'))return new Response('Forbidden',{status:403});
    const {socket,response}=Deno.upgradeWebSocket(request);peer=socket;
    socket.onmessage=event=>{try{const value=JSON.parse(event.data);if(value.kind==='sample')lastSample=value;events++;log.writeSync(encoder.encode(JSON.stringify({receivedAt:Date.now(),...value})+'\n'));}catch{}};
    return response;
  }
  if(url.pathname==='/control'&&request.method==='POST'&&request.headers.get('x-skylyrics-key')===key){
    if(peer?.readyState!==WebSocket.OPEN)return new Response('Spotify probe not connected',{status:409});
    const command=await request.json();if(!['pause','play','seek','repeat','restore','stop'].includes(command.action))return new Response('Invalid action',{status:400});
    peer.send(JSON.stringify(command));return Response.json({sent:command.action});
  }
  return new Response('Not found',{status:404});
});
await server.finished;

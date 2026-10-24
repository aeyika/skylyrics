/* Temporary local-only Spicetify probe. Not part of the release extension. */
(() => {
  globalThis.__skylyricsProbe?.stop();
  const rawSend=WebSocket.prototype.send, socketIds=new WeakMap();
  let nextSocket=0, stopped=false, sampleTimer, originalRepeat, originalPlaying;
  const probe=new WebSocket('ws://127.0.0.1:19585/probe?key=__DIAGNOSTIC_KEY__');
  const hash=value=>{let n=2166136261;for(const ch of String(value)){n=Math.imul(n^ch.charCodeAt(0),16777619);}return (n>>>0).toString(16);};
  const emit=value=>{if(probe.readyState===WebSocket.OPEN)rawSend.call(probe,JSON.stringify({...value,at:performance.now()}));};
  WebSocket.prototype.send=function(value){
    if(!stopped&&/^ws:\/\/127\.0\.0\.1:19584\/?$/.test(this.url)&&typeof value==='string'){
      try{
        const message=JSON.parse(value);let socketId=socketIds.get(this);
        if(!socketId){socketId=++nextSocket;socketIds.set(this,socketId);emit({kind:'connection',socketId});}
        const safe={v:message.v,type:message.type,seq:message.seq,track:message.trackId?hash(message.trackId):undefined};
        if(message.type==='playback')Object.assign(safe,{positionMs:message.positionMs,durationMs:message.durationMs,playing:message.playing,sentAtMs:message.sentAtMs});
        if(message.type==='lyrics')Object.assign(safe,{lines:message.lines?.length,document:hash(JSON.stringify(message.lines)),sync:message.sync});
        emit({kind:'wire',socketId,message:safe});
      }catch{}
    }
    return rawSend.call(this,value);
  };
  const sample=()=>{
    const p=globalThis.Spicetify?.Player;if(!p)return;
    try{const repeat=p.getRepeat(),playing=p.isPlaying();if(originalRepeat===undefined)originalRepeat=repeat;if(originalPlaying===undefined)originalPlaying=playing;emit({kind:'sample',positionMs:p.getProgress(),playing,durationMs:p.getDuration(),track:hash(p.data?.item?.uri??p.data?.track?.uri??''),paused:p.data?.isPaused,repeat});}catch(e){emit({kind:'sample-error',error:String(e)});}
  };
  probe.onopen=()=>{
    const p=globalThis.Spicetify?.Player;try{originalRepeat=p?.getRepeat?.();originalPlaying=p?.isPlaying?.();}catch{}
    emit({kind:'ready'});sample();sampleTimer=setInterval(sample,250);
  };
  probe.onmessage=async event=>{
    const p=globalThis.Spicetify?.Player;
    try{
      const command=JSON.parse(event.data);
      switch(command.action){
        case 'pause':await p.pause();break;
        case 'play':await p.play();break;
        case 'seek':if(Number.isFinite(command.positionMs)&&command.positionMs>=0)await p.seek(command.positionMs);break;
        case 'repeat':if([0,1,2].includes(command.mode))await p.setRepeat(command.mode);break;
        case 'restore':if([0,1,2].includes(originalRepeat))await p.setRepeat(originalRepeat);if(originalPlaying)await p.play();break;
        case 'stop':globalThis.__skylyricsProbe.stop();break;
        default:return;
      }
      emit({kind:'command',action:command.action});sample();
    }catch(e){emit({kind:'command-error',error:String(e)});}
  };
  globalThis.__skylyricsProbe={stop(){stopped=true;clearInterval(sampleTimer);WebSocket.prototype.send=rawSend;probe.close();}};
})();

package dev.skylyrics.core;

import org.junit.jupiter.api.Test;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BridgeTest {
    static final class Listener implements WebSocket.Listener {
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        final CompletableFuture<Integer> closed=new CompletableFuture<>();
        public void onOpen(WebSocket socket){socket.request(1);}
        public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){messages.add(data.toString());socket.request(1);return null;}
        public CompletionStage<?> onClose(WebSocket socket,int code,String reason){closed.complete(code);return null;}
    }
    private static WebSocket connect(int port,Listener listener){return HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port),listener).join();}
    @Test void pairsStreamsRejectsReplayAndReconnectsWithSameToken()throws Exception{
        int port;try(var free=new java.net.ServerSocket(0)){port=free.getLocalPort();}
        var d=Preferences.defaults();var p=new Preferences(1,true,0,12,440,1,1,true,true,true,port,d.bridgeToken());
        var ready=new CountDownLatch(1);var session=new PlaybackSession();
        try(var server=new BridgeServer(p,session,s->{if(s.startsWith("Listening"))ready.countDown();})){
            server.start();assertTrue(ready.await(10,TimeUnit.SECONDS));
            var l=new Listener();var ws=connect(port,l);
            ws.sendText("{\"v\":1,\"type\":\"hello\",\"seq\":0,\"code\":\""+server.beginPairing()+"\"}",true).join();
            String welcome=l.messages.poll(5,TimeUnit.SECONDS);assertNotNull(welcome);assertTrue(welcome.contains(p.bridgeToken()));
            String track="{\"v\":1,\"type\":\"track\",\"seq\":1,\"trackId\":\"skylyrics:test\",\"title\":\"Fixture\",\"durationMs\":32000}";
            ws.sendText(track,true).join();ws.sendText("{\"v\":1,\"type\":\"ping\",\"seq\":2}",true).join();
            assertNotNull(l.messages.poll(5,TimeUnit.SECONDS));assertEquals("skylyrics:test",session.snapshot(0).trackId());
            ws.sendText(track,true).join();assertEquals(1008,l.closed.get(5,TimeUnit.SECONDS));
            var retry=new Listener();var reconnected=connect(port,retry);
            reconnected.sendText("{\"v\":1,\"type\":\"hello\",\"seq\":0,\"token\":\""+p.bridgeToken()+"\"}",true).join();
            assertNotNull(retry.messages.poll(5,TimeUnit.SECONDS));assertTrue(server.connected());
            reconnected.sendClose(1000,"done").join();
        }
    }
    @Test void unauthenticatedClientCannotChangeTrack()throws Exception{
        int port;try(var free=new java.net.ServerSocket(0)){port=free.getLocalPort();}
        var d=Preferences.defaults();var p=new Preferences(1,true,0,12,440,1,1,true,true,true,port,d.bridgeToken());
        var ready=new CountDownLatch(1);var session=new PlaybackSession();
        try(var server=new BridgeServer(p,session,s->{if(s.startsWith("Listening"))ready.countDown();})){
            server.start();assertTrue(ready.await(10,TimeUnit.SECONDS));var l=new Listener();var ws=connect(port,l);
            ws.sendText("{\"v\":1,\"type\":\"track\",\"seq\":1,\"trackId\":\"x\",\"title\":\"x\",\"durationMs\":1}",true).join();
            assertEquals(1008,l.closed.get(5,TimeUnit.SECONDS));assertEquals("",session.snapshot(0).trackId());
        }
    }
}

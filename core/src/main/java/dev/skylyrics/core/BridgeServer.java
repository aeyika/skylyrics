package dev.skylyrics.core;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Local-only authenticated bridge. Pairing codes expire and can be used only once. */
public final class BridgeServer extends WebSocketServer implements AutoCloseable {
    private final Preferences preferences;
    private final PlaybackSession session;
    private final Consumer<String> status;
    private final Map<WebSocket,Long> sequences=new ConcurrentHashMap<>();
    private volatile WebSocket peer;
    private String code="";
    private long expiry;
    private int attempts;
    public BridgeServer(Preferences preferences,PlaybackSession session,Consumer<String> status) {
        super(new InetSocketAddress("127.0.0.1",preferences.port()),1);
        this.preferences=preferences;this.session=session;this.status=status;
        setReuseAddr(true);setConnectionLostTimeout(10);setMaxPendingConnections(4);
    }
    public synchronized String beginPairing(){code="%06d".formatted(new SecureRandom().nextInt(1_000_000));expiry=System.nanoTime()+120_000_000_000L;attempts=0;return code;}
    @Override public void onOpen(WebSocket socket,ClientHandshake handshake){
        String origin=handshake.getFieldValue("Origin");
        if(!origin.isEmpty()&&!origin.equals("https://xpui.app.spotify.com")) {socket.close(1008,"Spotify origin required");return;}
        if(getConnections().size()>4){socket.close(1013,"Too many connections");return;}
        sequences.put(socket,-1L);
    }
    private static boolean equal(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private synchronized boolean pair(String provided){
        if(code.isEmpty()||System.nanoTime()>expiry||++attempts>5||!equal(provided,code))return false;
        code="";return true;
    }
    @Override public void onMessage(WebSocket socket,String text){
        try {
            JsonObject o=Protocol.parse(text);String type=Protocol.string(o,"type",32);long seq=(long)Protocol.number(o,"seq");
            Long previous=sequences.get(socket);if(previous==null||seq<=previous)throw new IllegalArgumentException("Stale sequence");
            if(type.equals("hello")){
                if(socket==peer)throw new IllegalArgumentException("Already authenticated");
                boolean paired=o.has("code")&&pair(Protocol.string(o,"code",6));
                boolean trusted=o.has("token")&&equal(Protocol.string(o,"token",64),preferences.bridgeToken());
                if(!paired&&!trusted){socket.close(1008,"Pairing required");return;}
                WebSocket old=peer;peer=socket;if(old!=null&&old!=socket)old.close(1000,"Replaced by bridge");
                sequences.put(socket,seq);
                JsonObject reply=new JsonObject();reply.addProperty("v",1);reply.addProperty("type","welcome");reply.addProperty("token",preferences.bridgeToken());
                socket.send(reply.toString());status.accept("Connected to Beautiful Lyrics");return;
            }
            if(peer!=socket)throw new IllegalArgumentException("Authenticate first");
            switch(type){
                case "track" -> {double d=Protocol.number(o,"durationMs");if(d<0||d>86_400_000)throw new IllegalArgumentException("Invalid duration");session.track(Protocol.string(o,"trackId",256),Protocol.string(o,"title",1024),d);}
                case "playback" -> session.playback(Protocol.string(o,"trackId",256),Protocol.number(o,"positionMs"),Protocol.number(o,"durationMs"),Protocol.bool(o,"playing"),System.currentTimeMillis()-Protocol.number(o,"sentAtMs"),o.has("discontinuity")&&Protocol.bool(o,"discontinuity"));
                case "lyrics" -> session.lyrics(Protocol.lyrics(o));
                case "ping" -> socket.send("{\"v\":1,\"type\":\"pong\",\"seq\":"+seq+"}");
                default -> throw new IllegalArgumentException("Unsupported message");
            }
            sequences.put(socket,seq);
        } catch(RuntimeException e){socket.close(1008,"Invalid protocol message");status.accept("Bridge message rejected: "+e.getClass().getSimpleName());}
    }
    @Override public void onClose(WebSocket socket,int code,String reason,boolean remote){sequences.remove(socket);if(peer==socket){peer=null;session.disconnected();status.accept("Bridge disconnected — waiting for reconnection");}}
    @Override public void onError(WebSocket socket,Exception exception){status.accept("Bridge error: "+exception.getMessage());}
    @Override public void onStart(){status.accept("Listening on 127.0.0.1:"+getPort());}
    public boolean connected(){WebSocket p=peer;return p!=null&&p.isOpen();}
    @Override public void close(){try{stop(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}

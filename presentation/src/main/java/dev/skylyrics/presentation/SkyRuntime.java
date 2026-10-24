package dev.skylyrics.presentation;

import dev.skylyrics.core.*;
import java.awt.image.BufferedImage;
import java.io.*;

/** Shared client runtime. Minecraft adapters supply only events, camera data and GPU submission. */
public final class SkyRuntime implements AutoCloseable {
    public final PlaybackSession session=new PlaybackSession();
    private final AnimationEngine animation=new AnimationEngine();
    private final PanelRasterizer rasterizer=new PanelRasterizer();
    public final BridgeServer bridge;
    public volatile Preferences preferences;
    public volatile String status="Starting bridge";
    public volatile String pairingCode="";
    private long lastNanos=System.nanoTime(),rasterNanos;
    private BufferedImage raster;
    public SkyRuntime()throws IOException{
        preferences=Preferences.load(Preferences.sharedPath());
        bridge=new BridgeServer(preferences,session,s->{status=s;System.getLogger("SkyLyrics").log(System.Logger.Level.INFO,s);});
        bridge.start();
        if(Boolean.getBoolean("skylyrics.demo"))toggleDemo();
    }
    public Prepared extract(){
        var p=preferences;var state=session.snapshot(p.offsetMs());long now=System.nanoTime();
        Frame frame=animation.extract(state.lyrics(),state.clock().positionMs(),(now-lastNanos)/1e9,p,(text,role)->rasterizer.width(text,role));lastNanos=now;
        if(frame.lines().isEmpty())return null;
        if(raster==null||now-rasterNanos>=33_333_333L){raster=rasterizer.rasterize(frame);rasterNanos=now;}
        return new Prepared(frame,raster);
    }
    public void update(Preferences p){
        try{p.save(Preferences.sharedPath());preferences=p;raster=null;}
        catch(IOException e){status="Unable to save settings: "+e.getMessage();}
    }
    public void pair(){pairingCode=bridge.beginPairing();status="Enter this code in Spotify: "+pairingCode;}
    public void toggleDemo(){if(!session.snapshot(0).demo())session.demo(Demo.lyrics());else session.stopDemo();raster=null;}
    public boolean demo(){return session.snapshot(0).demo();}
    public void invalidate(){raster=null;}
    @Override public void close(){bridge.close();raster=null;}
    /** Pixels belong to this snapshot; they are never mutated after publication. */
    public record Prepared(Frame frame,BufferedImage pixels){}
}

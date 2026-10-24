package dev.skylyrics.core;

/** Serializes bridge updates and extraction so lyrics, track and clock are captured together. */
public final class PlaybackSession {
    private final PlaybackClock clock;
    private String trackId="", title="Waiting for Spotify";
    private Lyrics lyrics;
    private boolean demo;
    private long demoEpoch;
    public PlaybackSession(){this(new PlaybackClock());}
    public PlaybackSession(PlaybackClock clock){this.clock=clock;}
    public synchronized void track(String id,String name,double duration) {
        demo=false;
        if(!id.equals(trackId)){trackId=id;lyrics=null;clock.update(0,duration,false,0);}
        title=name;
    }
    public synchronized void playback(String id,double position,double duration,boolean playing,double age) {
        playback(id,position,duration,playing,age,false);
    }
    public synchronized void playback(String id,double position,double duration,boolean playing,double age,boolean discontinuity) {
        if(!id.equals(trackId))throw new IllegalArgumentException("Playback track does not match");
        demo=false;clock.update(position,duration,playing,age,discontinuity);
    }
    public synchronized void lyrics(Lyrics document) {
        if(!document.trackId().equals(trackId))throw new IllegalArgumentException("Stale lyrics for another track");
        // Repeated provider notifications must not restart animation springs or scrolling.
        if(!document.equals(lyrics))lyrics=document;
    }
    public synchronized Snapshot snapshot(double offsetMs) {
        if(demo){double t=((System.nanoTime()-demoEpoch)/1e6)%32000;clock.update(t,32000,true,0);}
        return new Snapshot(trackId,title,lyrics,clock.snapshot(offsetMs),demo);
    }
    public synchronized void disconnected(){if(!demo)clock.pause();}
    public synchronized void demo(Lyrics document){trackId=document.trackId();title="Skyward — original Sky Lyrics demo";lyrics=document;demo=true;demoEpoch=System.nanoTime();}
    public synchronized void stopDemo(){demo=false;trackId="";title="Waiting for Spotify";lyrics=null;clock.update(0,0,false,0);}
    public record Snapshot(String trackId,String title,Lyrics lyrics,PlaybackClock.State clock,boolean demo){}
}

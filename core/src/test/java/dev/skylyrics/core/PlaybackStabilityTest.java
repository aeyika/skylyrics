package dev.skylyrics.core;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackStabilityTest {
    @Test void repeatedStaleSpotifyPositionsCannotRestartPlayingClock(){
        var now=new AtomicLong();var clock=new PlaybackClock(now::get);clock.update(95504,231546,true,0);
        double previous=clock.snapshot(0).positionMs();
        for(int i=1;i<=120;i++){
            now.set(i*250_000_000L);
            clock.update(95504+(i%3)*65,231546,true,0);
            double position=clock.snapshot(0).positionMs();
            assertTrue(position>previous+200,"Stale 250ms polls must not pull time backward");previous=position;
        }
        assertEquals(125504,previous,100);
    }
    @Test void coarsePublicProgressRemainsSmoothBetweenSamplesAndRespectsTransitions(){
        var now=new AtomicLong();var clock=new PlaybackClock(now::get);clock.update(1000,32000,true,0);
        for(int i=1;i<=40;i++){now.set(i*250_000_000L);clock.update(1000+(i/4)*1000,32000,true,0);assertEquals(1000+i*250,clock.snapshot(0).positionMs(),1);}
        clock.update(11000,32000,false,0);now.addAndGet(2_000_000_000L);assertEquals(11000,clock.snapshot(0).positionMs());
        clock.update(11000,32000,true,0);now.addAndGet(500_000_000L);assertEquals(11500,clock.snapshot(0).positionMs());
        clock.update(18000,32000,true,0,true);assertEquals(18000,clock.snapshot(0).positionMs());
        clock.update(17900,32000,true,0,true);assertEquals(17900,clock.snapshot(0).positionMs(),"Explicit small backward seek");
        clock.update(0,32000,true,0,true);assertEquals(0,clock.snapshot(0).positionMs(),"Repeat must reset to start");
    }
    @Test void identicalLyricNotificationsPreserveDocumentIdentity(){
        var session=new PlaybackSession();var first=Demo.lyrics();session.track(first.trackId(),"Demo",32000);session.lyrics(first);
        session.lyrics(Demo.lyrics());assertSame(first,session.snapshot(0).lyrics(),"Equal documents must not reset animation springs");
    }
}

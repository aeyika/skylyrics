package dev.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @Test void monotonicClockPauseSeekRepeatAndOffset(){
        var now=new AtomicLong();var c=new PlaybackClock(now::get);
        c.update(1000,32000,true,50);now.set(500_000_000);assertEquals(1550,c.snapshot(0).positionMs());
        assertEquals(1650,c.snapshot(100).positionMs());
        c.update(2000,32000,false,100);now.set(2_000_000_000);assertEquals(2000,c.snapshot(0).positionMs());
        c.update(18000,32000,true,0);assertEquals(18000,c.snapshot(0).positionMs());
        c.update(0,32000,true,0);assertEquals(0,c.snapshot(0).positionMs());
        now.set(90_000_000_000L);assertEquals(32000,c.snapshot(0).positionMs());
        assertThrows(IllegalArgumentException.class,()->c.update(Double.NaN,32000,true,0));
    }
    @Test void preferencesSurviveVersionChanges(@TempDir Path dir)throws Exception{
        Path file=dir.resolve("preferences.json");var original=Preferences.load(file).withOffset(-125).withEffects(false,true,true);
        original.save(file);assertEquals(original,Preferences.load(file));
        assertThrows(IllegalArgumentException.class,()->original.withOffset(Double.NaN));
    }
    @Test void trackChangeClearsLyricsAndRejectsStaleMessages(){
        var s=new PlaybackSession();s.track("skylyrics:demo","Demo",32000);s.lyrics(Demo.lyrics());
        s.track("second","Second",10000);assertNull(s.snapshot(0).lyrics());
        assertThrows(IllegalArgumentException.class,()->s.lyrics(Demo.lyrics()));
        assertThrows(IllegalArgumentException.class,()->s.playback("skylyrics:demo",0,32000,true,0));
    }
    @Test void malformedMessagesAreRejected(){
        for(String message:List.of("{}","{\"v\":2,\"type\":\"ping\",\"seq\":1}","{\"v\":1,\"type\":\"ping\",\"seq\":-1}","{\"v\":1,\"type\":\"ping\",\"seq\":0.5}","{\"v\":1,\"type\":\"execute\",\"seq\":1}"))
            assertThrows(RuntimeException.class,()->Protocol.parse(message));
        assertThrows(IllegalArgumentException.class,()->new Lyrics.Segment("bad",100,0,false));
    }
    @Test void immutableFramesWrapUnicodeAndAgreeAcrossFreshEngines(){
        var lyrics=Demo.lyrics();var p=Preferences.defaults().withPlacement(12,120,1,1);
        var a=new AnimationEngine();var b=new AnimationEngine();
        for(double time:new double[]{0,1000,1500,3000,9000,15500,21000,29000,0}){
            var f=a.extract(lyrics,time,1d/60,p,t->t.codePointCount(0,t.length())*6);
            assertEquals(f,b.extract(lyrics,time,1d/60,p,t->t.codePointCount(0,t.length())*6));
            assertThrows(UnsupportedOperationException.class,()->f.lines().clear());
            for(var l:f.lines())for(var s:l.segments()){
                assertTrue(s.x()>=0&&s.x()+s.width()<=120.000001);
                assertTrue(Double.isFinite(s.glow()));assertTrue(s.scale()>0);
            }
        }
    }
    @Test void naturalSplinePassesKnotsAndHasNaturalBoundaries(){
        var spline=new NaturalSpline(new double[]{0,.7,1},new double[]{.95,1.025,1});
        assertEquals(.95,spline.at(0),1e-12);assertEquals(1.025,spline.at(.7),1e-12);assertEquals(1,spline.at(1),1e-12);
        assertEquals(.95,spline.at(-1),1e-12);assertEquals(1,spline.at(2),1e-12);
    }
    @Test void springsConvergeAndImmediateSeekSnaps(){
        for(double damping:new double[]{.4,.6,1,1.4}){
            var s=new BeautifulSpring(0,damping,1.25);s.target=1;
            double v=0;for(int i=0;i<1200;i++)v=s.update(1d/60);
            assertEquals(1,v,1e-8);s.set(.3);assertEquals(.3,s.update(0),1e-12);
        }
    }
}

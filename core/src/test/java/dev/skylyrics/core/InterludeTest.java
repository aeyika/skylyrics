package dev.skylyrics.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InterludeTest {
    @Test void matchesOriginalCurveInterpolatorIntersectionsAndArcLength()throws Exception{
        JsonArray samples;try(var in=getClass().getResourceAsStream("/expected-interlude.json")){assertNotNull(in);samples=JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("samples");}
        var lift=new Curve2D(new double[][]{{0,.01},{.9,-1d/60},{1,0}});
        for(var raw:samples){var e=raw.getAsJsonObject();double duration=e.get("duration").getAsDouble(),t=e.get("t").getAsDouble();
            List<double[]> points=new ArrayList<>();points.add(new double[]{0,0});points.add(new double[]{.2/duration,1.05});
            for(int i=1;i<=Math.floor((duration-.075-.2)/2.25);i++)points.add(new double[]{(.2+i*2.25)/duration,i%2==0?1.05:.95});
            points.add(new double[]{(duration-.075)/duration,1.15});points.add(new double[]{1,0});
            var scale=new Curve2D(points.toArray(double[][]::new));var alpha=new Curve2D(new double[][]{{0,0},{.5/duration,1},{(duration-.075)/duration,1},{1,0}});
            assertEquals(e.get("scale").getAsDouble(),scale.lastYAtX(t),1e-8,"scale at "+duration+" / "+t);
            assertEquals(e.get("opacity").getAsDouble(),alpha.lastYAtX(t),1e-8,"opacity at "+duration+" / "+t);
            assertEquals(e.get("lift").getAsDouble(),lift.yAtDistance(t),1e-5,"arc-length lift");
        }
    }
    @Test void dotsHaveIndependentTimingAndDisappearOutsideInterlude(){
        var line=new Lyrics.Line(1000,6000,Lyrics.Alignment.LEFT,Lyrics.Role.INTERLUDE,List.of(new Lyrics.Segment("•••",1000,6000,false)));
        var lyrics=new Lyrics("interlude",Lyrics.Sync.SYLLABLE,List.of(line));var engine=new AnimationEngine();var p=Preferences.defaults();
        assertTrue(engine.extract(lyrics,0,0,p,s->6).lines().isEmpty());
        var frame=engine.extract(lyrics,3000,0,p,s->6);assertEquals(3,frame.lines().getFirst().segments().size());
        assertNotEquals(frame.lines().getFirst().segments().getFirst().scale(),frame.lines().getFirst().segments().getLast().scale());
        assertTrue(engine.extract(lyrics,7000,0,p,s->6).lines().isEmpty());
    }
}

package dev.skylyrics.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class UpstreamAnimationTest {
    @Test void matchesRecordedUnmodifiedBeautifulLyricsSpringAndSpline()throws Exception{
        JsonArray expected;
        try(var in=getClass().getResourceAsStream("/expected-animation.json")){
            assertNotNull(in);expected=JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("samples");
        }
        var scale=new NaturalSpline(new double[]{0,.7,1},new double[]{.95,1.025,1});
        var lift=new NaturalSpline(new double[]{0,.9,1},new double[]{.01,-1d/60,0});
        var glow=new NaturalSpline(new double[]{0,.15,.6,1},new double[]{0,1,1,0});
        var s=new BeautifulSpring(.95,.6,.7);var y=new BeautifulSpring(.01,.4,1.25);var g=new BeautifulSpring(0,.5,1);
        int index=0;
        for(int tick=0;tick<=120;tick++){
            double t=Math.min(1,tick/60d);s.target=scale.at(t);y.target=lift.at(t);g.target=glow.at(t);
            double sv=s.update(1d/60),yv=y.update(1d/60),gv=g.update(1d/60);
            if(tick%10==0){var e=expected.get(index++).getAsJsonObject();assertEquals(e.get("scale").getAsDouble(),sv,1e-10);assertEquals(e.get("lift").getAsDouble(),yv,1e-10);assertEquals(e.get("glow").getAsDouble(),gv,1e-10);}
        }
    }
}

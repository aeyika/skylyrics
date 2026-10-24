package dev.skylyrics.presentation;
import dev.skylyrics.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PresentationTest {
    @Test void snapshotGlowRadiusControlsShadowSpread(){
        var rasterizer=new PanelRasterizer();
        assertTrue(glowPixels(rasterizer,12)>glowPixels(rasterizer,2),"A larger glow radius must reach more surrounding pixels");
    }
    private int glowPixels(PanelRasterizer rasterizer,double radius){
        var segment=new Frame.Segment("M",100,20,35,1,1,0,1,1,radius,0,1000,0,1,false);
        var line=new Frame.Line(0,20,50,1,0,Lyrics.Alignment.LEFT,Lyrics.Role.LEAD,java.util.List.of(segment));
        var frame=new Frame(500,"test",java.util.List.of(line),new Frame.Panel(12,440,1,1,true,false,true,40));
        var image=rasterizer.rasterize(frame);int count=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>0)count++;
        return count;
    }
    @Test void preparedTextMasksContainTransparentAndVisiblePixels(){
        var rasterizer=new PanelRasterizer();var engine=new AnimationEngine();
        var frame=engine.extract(Demo.lyrics(),3000,1d/60,Preferences.defaults(),(text,role)->rasterizer.width(text,role));
        var image=rasterizer.rasterize(frame);int visible=0,transparent=0,partial=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int a=image.getRGB(x,y)>>>24;if(a==0)transparent++;else visible++;if(a>0&&a<200)partial++;
        }
        assertTrue(transparent>visible,"Panel background must remain transparent");
        assertTrue(visible>1000,"Actual glyph masks must be present");assertTrue(partial>1000,"Antialiasing and effects must retain fractional alpha");
    }
    @Test void turningEffectsOffChangesPreparedPixels(){
        var rasterizer=new PanelRasterizer();var p=Preferences.defaults();var lyrics=Demo.lyrics();
        var a=rasterizer.rasterize(new AnimationEngine().extract(lyrics,3000,0,p,(text,role)->rasterizer.width(text,role)));
        var b=rasterizer.rasterize(new AnimationEngine().extract(lyrics,3000,0,p.withEffects(true,false,false),(text,role)->rasterizer.width(text,role)));
        int changed=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)if(a.getRGB(x,y)!=b.getRGB(x,y))changed++;
        assertTrue(changed>1000,"Blur and glow settings must affect text masks");
    }
}

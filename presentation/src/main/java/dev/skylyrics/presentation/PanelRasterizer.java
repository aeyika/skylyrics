package dev.skylyrics.presentation;

import dev.skylyrics.core.*;
import dev.skylyrics.core.Frame;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/** Backend neutral text mask preparation; adapters upload pixels using Blaze3D DynamicTexture. */
public final class PanelRasterizer {
    public static final int DENSITY=2, PADDING=30, HEIGHT=360;
    private static final FontRenderContext METRICS=new FontRenderContext(null,true,true);
    private final Font face;
    private final Font medium;
    private final Font fallback=new Font("Dialog",Font.BOLD,18);
    public PanelRasterizer(){
        try(InputStream in=getClass().getResourceAsStream("/assets/skylyrics/fonts/beautiful-lyrics-bold.ttf")){
            face=in==null?fallback:Font.createFont(Font.TRUETYPE_FONT,in);
        }catch(IOException|FontFormatException e){throw new IllegalStateException("Unable to load Beautiful Lyrics font",e);}
        try(InputStream in=getClass().getResourceAsStream("/assets/skylyrics/fonts/beautiful-lyrics-medium.ttf")){
            medium=in==null?fallback:Font.createFont(Font.TRUETYPE_FONT,in);
        }catch(IOException|FontFormatException e){throw new IllegalStateException("Unable to load Beautiful Lyrics medium font",e);}
    }
    private Font font(String text,float size){return (face.canDisplayUpTo(text)<0?face:fallback).deriveFont(size);}
    public double width(String text){return font(text,9).getStringBounds(text,METRICS).getWidth();}
    public double width(String text,Lyrics.Role role){Font chosen=role==Lyrics.Role.BACKGROUND?medium:face;return (chosen.canDisplayUpTo(text)<0?chosen:fallback).deriveFont(9f).getStringBounds(text,METRICS).getWidth();}
    public BufferedImage rasterize(Frame frame){
        int w=(int)Math.ceil(frame.panel().width()+PADDING*2)*DENSITY,h=HEIGHT*DENSITY;
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D out=result.createGraphics();quality(out);
        for(Frame.Line line:frame.lines()){
            BufferedImage ink=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g=ink.createGraphics();quality(g);
            g.scale(DENSITY,DENSITY);
            for(Frame.Segment segment:line.segments()){
                float size=(float)(frame.panel().fontSize()*(line.role()==Lyrics.Role.BACKGROUND?.75:1));
                Font chosen=line.role()==Lyrics.Role.BACKGROUND?medium:face;
                Font font=(chosen.canDisplayUpTo(segment.text())<0?chosen:fallback).deriveFont(size);
                boolean dot=line.role()==Lyrics.Role.INTERLUDE;
                Shape glyph=dot?new Ellipse2D.Double(0,-segment.width(),segment.width(),segment.width()):font.createGlyphVector(METRICS,segment.text()).getOutline();
                double x=segment.x()+PADDING,y=segment.y()+110+segment.displacement()+(dot?segment.width():size);
                AffineTransform transform=new AffineTransform();transform.translate(x+segment.width()/2,y);transform.scale(segment.scale(),segment.scale());transform.translate(-segment.width()/2,0);
                Shape shape=transform.createTransformedShape(glyph);
                float alpha=(float)Math.max(0,Math.min(1,segment.opacity()*frame.panel().opacity()));
                // Beautiful Lyrics' white-to-dim CSS gradient across the text mask.
                double extent=Math.max(.0001,segment.gradientExtent());
                double local=(segment.highlight()-segment.gradientOffset())/extent;
                float start=(float)((segment.verticalGradient()?y-size:x)+local*(segment.verticalGradient()?size:segment.width()));
                float end=(float)(start+.2/extent*(segment.verticalGradient()?size:segment.width()));
                Color bright=new Color(1,1,1,alpha*.85f),dim=new Color(1,1,1,alpha*(segment.verticalGradient()?.4f:.5f));
                if(segment.verticalGradient())g.setPaint(new LinearGradientPaint(0,start,0,Math.max(start+.001f,end),new float[]{0,1},new Color[]{bright,dim}));
                else g.setPaint(new LinearGradientPaint(start,0,Math.max(start+.001f,end),0,new float[]{0,1},new Color[]{bright,dim}));
                g.fill(shape);
                if(frame.panel().glow()){
                    drawGlow(out,shape,alpha*segment.glow(),segment.glowRadius());
                }
            }
            g.dispose();
            double radius=frame.panel().blur()?line.blur():0;
            out.drawImage(radius>.1?blur(ink,radius*DENSITY):ink,0,0,null);
        }
        out.dispose();
        // Soft vertical clipping keeps entering and leaving lines from popping.
        int[] pixels=((DataBufferInt)result.getRaster().getDataBuffer()).getData();
        for(int y=0;y<h;y++){
            double fade=Math.min(1,Math.min(y/(20d*DENSITY),(h-1-y)/(32d*DENSITY)));
            if(fade>=1)continue;
            for(int x=0;x<w;x++){int at=y*w+x,c=pixels[at];int a=(int)((c>>>24)*fade),r=(int)(((c>>>16)&255)*fade),b=(int)((c&255)*fade),gg=(int)(((c>>>8)&255)*fade);pixels[at]=(a<<24)|(r<<16)|(gg<<8)|b;}
        }
        return result;
    }
    private static void drawGlow(Graphics2D out,Shape shape,double alpha,double radius){
        if(alpha<=.001)return;
        var bounds=shape.getBounds2D();
        int padding=(int)Math.ceil(radius*DENSITY)+2;
        int x=(int)Math.floor(bounds.getMinX()*DENSITY)-padding,y=(int)Math.floor(bounds.getMinY()*DENSITY)-padding;
        int width=(int)Math.ceil(bounds.getMaxX()*DENSITY)-x+padding,height=(int)Math.ceil(bounds.getMaxY()*DENSITY)-y+padding;
        var mask=new BufferedImage(Math.max(1,width),Math.max(1,height),BufferedImage.TYPE_INT_ARGB_PRE);
        var graphics=mask.createGraphics();quality(graphics);graphics.translate(-x,-y);graphics.scale(DENSITY,DENSITY);
        graphics.setColor(new Color(1,1,1,(float)Math.max(0,Math.min(1,alpha))));graphics.fill(shape);graphics.dispose();
        out.drawImage(blur(mask,radius*DENSITY),x,y,null);
    }
    private static void quality(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
    private static BufferedImage blur(BufferedImage source,double radius){
        int r=Math.max(1,(int)Math.ceil(radius));float[] weights=new float[r*2+1];double sigma=Math.max(.5,r/2.5),sum=0;
        for(int i=-r;i<=r;i++){double v=Math.exp(-(i*i)/(2*sigma*sigma));weights[i+r]=(float)v;sum+=v;}
        for(int i=0;i<weights.length;i++)weights[i]/=(float)sum;
        var horizontal=new ConvolveOp(new Kernel(weights.length,1,weights),ConvolveOp.EDGE_NO_OP,null);
        var vertical=new ConvolveOp(new Kernel(1,weights.length,weights),ConvolveOp.EDGE_NO_OP,null);
        return vertical.filter(horizontal.filter(source,null),null);
    }
}

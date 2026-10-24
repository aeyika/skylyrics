package dev.skylyrics.core;

import java.util.*;
import java.util.function.ToDoubleFunction;

/** Beautiful Lyrics animation is calculated here once, never in a Minecraft adapter. */
public final class AnimationEngine {
    private static final NaturalSpline SCALE = new NaturalSpline(new double[]{0,.7,1},new double[]{.95,1.025,1});
    private static final NaturalSpline LIFT = new NaturalSpline(new double[]{0,.9,1},new double[]{.01,-1d/60,0});
    private static final NaturalSpline GLOW = new NaturalSpline(new double[]{0,.15,.6,1},new double[]{0,1,1,0});
    private static final NaturalSpline LINE_GLOW = new NaturalSpline(new double[]{0,.5,.925,1},new double[]{0,1,1,0});
    private final Map<String, Motion> motions = new HashMap<>();
    private final Map<Integer,InterludeAnimation> interludes=new HashMap<>();
    private Lyrics document;
    private double lastTime = Double.NaN, scroll;
    private static final class Motion {
        final BeautifulSpring scale = new BeautifulSpring(.95,.6,.7);
        final BeautifulSpring lift = new BeautifulSpring(.01,.4,1.25);
        final BeautifulSpring glow = new BeautifulSpring(0,.5,1);
        double[] sample(double t,double glowT,double dt,boolean snap,boolean line) {
            scale.target=line?1:SCALE.at(t);lift.target=line?0:LIFT.at(t);glow.target=(line?LINE_GLOW:GLOW).at(glowT);
            if(snap){scale.set(scale.target);lift.set(lift.target);glow.set(glow.target);}
            return new double[]{scale.update(dt),lift.update(dt),glow.update(dt)};
        }
    }
    public static double clamp(double n) { return Math.max(0,Math.min(1,n)); }
    /** Font metrics in Minecraft font pixels, provided during extraction. */
    public synchronized Frame extract(Lyrics lyrics, double timeMs, double dt, Preferences p, ToDoubleFunction<String> width) {
        return extract(lyrics,timeMs,dt,p,(text,role)->width.applyAsDouble(text));
    }
    public synchronized Frame extract(Lyrics lyrics, double timeMs, double dt, Preferences p, TextMetrics width) {
        boolean snap = document != lyrics || Double.isNaN(lastTime) || Math.abs(timeMs-lastTime-dt*1000)>250;
        if(document!=lyrics){document=lyrics;motions.clear();interludes.clear();}
        lastTime=timeMs;dt=Math.max(0,Math.min(.1,dt));
        double font=Math.max(29.6,Math.min(56,p.width()*.07))*p.scale();
        var panel = new Frame.Panel(p.distance(),p.width(),p.scale(),p.opacity(),p.occlusion(),p.blur(),p.glow(),font,p.skyboxAnchor());
        if(lyrics==null||!p.enabled())return new Frame(timeMs,"",List.of(),panel);
        List<Lyrics.Line> lines=lyrics.lines(); int active=0;
        for(int i=0;i<lines.size();i++)if(lines.get(i).startMs()<=timeMs)active=i;
        // Background vocals share the lead's scrolling anchor.
        while(active>0&&lines.get(active).role()==Lyrics.Role.BACKGROUND)active--;
        double gap=p.width()*.02, y=0, anchor=0;
        List<List<Raw>> laidOut=new ArrayList<>();double[] ys=new double[lines.size()],heights=new double[lines.size()];
        for(int i=0;i<lines.size();i++) {
            Lyrics.Line line=lines.get(i);ys[i]=y;if(i==active)anchor=y;
            if(line.role()==Lyrics.Role.INTERLUDE){
                boolean showing=timeMs>=line.startMs()&&timeMs<=line.endMs();
                laidOut.add(List.of());heights[i]=showing?Math.max(4.8,Math.min(12.8,p.width()*.017))*2:0;
                if(showing)y+=heights[i]+gap;continue;
            }
            double size=font*(line.role()==Lyrics.Role.BACKGROUND?.75:1), x=0,row=0;
            List<Raw> raw=new ArrayList<>();
            for(int si=0;si<line.segments().size();si++) {
                Lyrics.Segment segment=line.segments().get(si);
                int[] cps=segment.text().codePoints().toArray();boolean emphasis=lyrics.sync()==Lyrics.Sync.SYLLABLE && segment.endMs()-segment.startMs()>=1000 && segment.text().length()<=12;
                if(lyrics.sync()==Lyrics.Sync.SYLLABLE&&(si==0||!line.segments().get(si-1).partOfWord())){
                    double wordWidth=0;
                    for(int wi=si;wi<line.segments().size();wi++){
                        var wordPart=line.segments().get(wi);
                        wordWidth+=width.width(wordPart.text(),line.role())*size/9;
                        if(!wordPart.partOfWord())break;
                    }
                    if(wordWidth<=p.width()&&x>0&&x+wordWidth>p.width()){x=0;row+=size*1.1818181818;}
                }
                double syllableT=clamp((timeMs-segment.startMs())/(segment.endMs()-segment.startMs()));
                double alpha=-(Math.cos(Math.PI*syllableT)-1)/2;
                double segmentWidth=0,advance=0;
                for(int cp:cps)segmentWidth+=width.width(new String(Character.toChars(cp)),line.role())*size/9;
                // Use code points for emphasis, matching the upstream for-of loop.
                for(int ci=0;ci<cps.length;ci++) {
                    String text=new String(Character.toChars(cps[ci]));double w=width.width(text,line.role())*size/9;
                    if(!Character.isWhitespace(cps[ci])&&(ci==0||Character.isWhitespace(cps[ci-1]))){
                        double wordWidth=0;for(int wi=ci;wi<cps.length&&!Character.isWhitespace(cps[wi]);wi++)wordWidth+=width.width(new String(Character.toChars(cps[wi])),line.role())*size/9;
                        if(wordWidth<=p.width()&&x>0&&x+wordWidth>p.width()){x=0;row+=size*1.1818181818;}
                    }
                    if(x+w>p.width()&&x>0){x=0;row+=size*1.1818181818;}
                    double start=(double)ci/Math.max(1,cps.length);
                    double t=emphasis?clamp((alpha-start)*cps.length):syllableT;
                    double glowT=emphasis?clamp((alpha-start)/(1-start)):syllableT;
                    String key=i+":"+si+":"+(emphasis?ci:-1);
                    raw.add(new Raw(text,x,row,w,t,glowT,key,emphasis,segment,emphasis?0:advance/Math.max(.001,segmentWidth),emphasis?1:w/Math.max(.001,segmentWidth),syllableT));x+=w;advance+=w;
                }
                if(!segment.partOfWord()&&si<line.segments().size()-1&&!segment.text().endsWith(" "))x+=width.width(" ",line.role())*size/9;
            }
            // Align wrapped rows independently.
            if(line.alignment()==Lyrics.Alignment.RIGHT) {
                Map<Double,Double> ends=new HashMap<>();for(Raw r:raw)ends.merge(r.y,r.x+r.width,Math::max);
                for(int r=0;r<raw.size();r++){Raw a=raw.get(r);raw.set(r,a.at(a.x+p.width()-ends.get(a.y)));}
            }
            laidOut.add(raw);heights[i]=row+size*1.1818181818;y+=heights[i]+gap;
        }
        scroll=snap?anchor:scroll+(anchor-scroll)*(1-Math.exp(-dt*12));
        List<Frame.Line> visible=new ArrayList<>();
        for(int i=0;i<lines.size();i++) {
            Lyrics.Line line=lines.get(i);double yy=ys[i]-scroll;
            if(yy+heights[i]<-font*5||yy>font*10)continue;
            boolean singing=timeMs>=line.startMs()&&timeMs<=line.endMs();
            if(line.role()==Lyrics.Role.INTERLUDE){
                if(singing){var animation=interludes.computeIfAbsent(i,key->new InterludeAnimation(line.endMs()-line.startMs()));visible.add(new Frame.Line(i,yy,heights[i],1,0,line.alignment(),line.role(),animation.extract(line,timeMs,yy,dt,snap,panel)));}
                continue;
            }
            double opacity=singing?1:timeMs>line.endMs()?.497:.51;
            if(line.role()==Lyrics.Role.BACKGROUND)opacity*=singing?.8:timeMs>line.endMs()?.6690140845:.53921568627;
            double blur=Math.min(4,Math.abs(i-active))*1.25;
            List<Frame.Segment> segments=new ArrayList<>();Map<String,double[]> sampled=new HashMap<>();
            for(Raw r:laidOut.get(i)) {
                boolean lineSync=lyrics.sync()!=Lyrics.Sync.SYLLABLE;
                double[] values=sampled.get(r.key);
                if(values==null){values=motions.computeIfAbsent(r.key,k->new Motion()).sample(r.t,r.glowT,dt,snap,lineSync);sampled.put(r.key,values);}
                double scale=values[0],lift=values[1]*font*(r.emphasis?2:1);
                if(r.emphasis){
                    String parentKey=r.key.substring(0,r.key.lastIndexOf(':'))+":parent";
                    double[] parent=sampled.get(parentKey);
                    if(parent==null){parent=motions.computeIfAbsent(parentKey,k->new Motion()).sample(r.syllableT,r.syllableT,dt,snap,false);sampled.put(parentKey,parent);}
                    scale*=parent[0];lift+=parent[1]*font;
                }
                if(lineSync&&singing)scale*=1.05;
                double highlight=lyrics.sync()==Lyrics.Sync.STATIC?1:lineSync?1.2*r.t:-.2+1.2*r.t;
                // Smooth 20%-wide CSS gradient, evaluated by the renderer at glyph mask pixels.
                segments.add(new Frame.Segment(r.text,r.x,yy+r.y,r.width,highlight,scale,lift,opacity,
                        values[2]*(lineSync?.5:r.emphasis?1:.35),lineSync?4+8*values[2]:4+2*values[2]*(r.emphasis?3:1),r.source.startMs(),r.source.endMs(),
                        lineSync?r.y/heights[i]:r.gradientOffset,lineSync?font/heights[i]:r.gradientExtent,lineSync));
            }
            visible.add(new Frame.Line(i,yy,heights[i],opacity,blur,line.alignment(),line.role(),segments));
        }
        return new Frame(timeMs,lyrics.trackId(),visible,panel);
    }
    private record Raw(String text,double x,double y,double width,double t,double glowT,String key,boolean emphasis,Lyrics.Segment source,double gradientOffset,double gradientExtent,double syllableT) {
        Raw at(double newX){return new Raw(text,newX,y,width,t,glowT,key,emphasis,source,gradientOffset,gradientExtent,syllableT);}
    }
}

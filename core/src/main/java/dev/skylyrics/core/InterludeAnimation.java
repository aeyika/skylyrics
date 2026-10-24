package dev.skylyrics.core;

import java.util.*;

/** Port of Beautiful Lyrics Interlude.ts, including pulse curves and independent dot springs. */
final class InterludeAnimation {
    private static final NaturalSpline SCALE=new NaturalSpline(new double[]{0,.7,1},new double[]{.75,1.05,1});
    private static final NaturalSpline LIFT=new NaturalSpline(new double[]{0,.9,1},new double[]{.125,-.2,0});
    private static final NaturalSpline GLOW=new NaturalSpline(new double[]{0,.6,1},new double[]{0,1,1});
    private static final NaturalSpline OPACITY=new NaturalSpline(new double[]{0,.6,1},new double[]{.35,1,1});
    private static final Curve2D MAIN_LIFT=new Curve2D(new double[][]{{0,.01},{.9,-1d/60},{1,0}});
    private final Curve2D mainScale,mainOpacity;
    private final BeautifulSpring ms=new BeautifulSpring(0,.7,5),my=new BeautifulSpring(0,.4,1.25),ma=new BeautifulSpring(0,.4,1.25);
    private final BeautifulSpring[][] dots=new BeautifulSpring[3][4];
    InterludeAnimation(double durationMs){
        double duration=durationMs/1000;
        List<double[]> scale=new ArrayList<>();scale.add(new double[]{0,0});scale.add(new double[]{.2/duration,1.05});
        int pulses=(int)Math.floor((duration-.075-.2)/2.25);
        for(int i=1;i<=pulses;i++)scale.add(new double[]{(.2+i*2.25)/duration,i%2==0?1.05:.95});
        scale.add(new double[]{(duration-.075)/duration,1.15});scale.add(new double[]{1,0});mainScale=new Curve2D(scale.toArray(double[][]::new));
        mainOpacity=new Curve2D(new double[][]{{0,0},{.5/duration,1},{(duration-.075)/duration,1},{1,0}});
        for(int i=0;i<3;i++)dots[i]=new BeautifulSpring[]{new BeautifulSpring(.75,.6,.7),new BeautifulSpring(.125,.4,1.25),new BeautifulSpring(0,.5,1),new BeautifulSpring(.35,.5,1)};
    }
    private static double sample(BeautifulSpring spring,double target,double dt,boolean snap){spring.target=target;if(snap)spring.set(target);return spring.update(dt);}
    List<Frame.Segment> extract(Lyrics.Line line,double time,double y,double dt,boolean snap,Frame.Panel panel){
        double t=AnimationEngine.clamp((time-line.startMs())/(line.endMs()-line.startMs()));
        double scale=sample(ms,mainScale.lastYAtX(t),dt,snap),lift=sample(my,MAIN_LIFT.yAtDistance(t),dt,snap),opacity=sample(ma,mainOpacity.lastYAtX(t),dt,snap);
        opacity=-(Math.cos(Math.PI*opacity)-1)/2;
        double gap=Math.max(4.8,Math.min(12.8,panel.width()*.017)),size=gap*2,step=gap*2.75,center=step+size/2;
        List<Frame.Segment> output=new ArrayList<>();
        for(int i=0;i<3;i++){
            double dotT=AnimationEngine.clamp((t-i*.925/3)/(.925/3));
            double ds=sample(dots[i][0],SCALE.at(dotT),dt,snap),dy=sample(dots[i][1],LIFT.at(dotT),dt,snap),dg=sample(dots[i][2],GLOW.at(dotT),dt,snap),da=sample(dots[i][3],OPACITY.at(dotT),dt,snap);
            double x=gap*1.1+center+(i*step+size/2-center)*scale-size/2;
            if(line.alignment()==Lyrics.Alignment.RIGHT)x+=panel.width()-(2*step+size+gap*1.1);
            output.add(new Frame.Segment("●",x,y,size,1,scale*ds,(lift+dy)*size,AnimationEngine.clamp(opacity*da),dg*.9,4+6*dg,line.startMs(),line.endMs(),0,1,false));
        }
        return output;
    }
}

package dev.skylyrics.core;

import java.util.*;

/** Default open, alpha=0, tension=.5 curve-interpolator 3.3.1 behavior used by Beautiful Lyrics. */
public final class Curve2D {
    private final double[][][] coefficients;
    private double[] lengths;
    public Curve2D(double[][] points){
        if(points.length<2)throw new IllegalArgumentException("At least two points required");
        coefficients=new double[points.length-1][2][4];
        for(int i=0;i<coefficients.length;i++)for(int axis=0;axis<2;axis++){
            double p1=points[i][axis],p2=points[i+1][axis],p0=i>0?points[i-1][axis]:2*p1-p2,p3=i+2<points.length?points[i+2][axis]:2*p2-p1;
            double u=.25*(p2-p0),v=.25*(p3-p1);
            coefficients[i][axis]=new double[]{2*p1-2*p2+u+v,-3*p1+3*p2-2*u-v,u,p1};
        }
    }
    private static double value(double[] c,double t){return ((c[0]*t+c[1])*t+c[2])*t+c[3];}
    private double point(double time,int axis){double scaled=Math.max(0,Math.min(1,time))*coefficients.length;int i=Math.min(coefficients.length-1,(int)scaled);return value(coefficients[i][axis],scaled-i);}
    /** Last intersection in curve order, matching upstream getIntersects(x).at(-1). */
    public double lastYAtX(double x){
        double result=1;
        for(double[][] segment:coefficients){
            double[] c=segment[0];List<Double> cuts=new ArrayList<>(List.of(0d,1d));
            double a=3*c[0],b=2*c[1],d=c[2],disc=b*b-4*a*d;
            if(Math.abs(a)<1e-14){if(Math.abs(b)>1e-14){double t=-d/b;if(t>0&&t<1)cuts.add(t);}}
            else if(disc>=0){for(double t:new double[]{(-b-Math.sqrt(disc))/(2*a),(-b+Math.sqrt(disc))/(2*a)})if(t>0&&t<1)cuts.add(t);}
            Collections.sort(cuts);
            for(int i=0;i<cuts.size()-1;i++){
                double lo=cuts.get(i),hi=cuts.get(i+1),fl=value(c,lo)-x,fh=value(c,hi)-x;
                if(Math.abs(fl)<1e-12)result=value(segment[1],lo);
                if(fl*fh<0){for(int n=0;n<48;n++){double mid=(lo+hi)/2,f=value(c,mid)-x;if(fl*f<=0){hi=mid;fh=f;}else{lo=mid;fl=f;}}result=value(segment[1],(lo+hi)/2);}
                if(Math.abs(fh)<1e-12)result=value(segment[1],hi);
            }
        }
        return result;
    }
    /** Arc-length parameterization, numerically bounded by the recorded upstream oracle tests. */
    public double yAtDistance(double fraction){
        final int samples=8192;
        if(lengths==null){lengths=new double[samples+1];double px=point(0,0),py=point(0,1);for(int i=1;i<=samples;i++){double x=point(i/(double)samples,0),y=point(i/(double)samples,1);lengths[i]=lengths[i-1]+Math.hypot(x-px,y-py);px=x;py=y;}}
        double target=AnimationEngine.clamp(fraction)*lengths[samples];int low=0,high=samples;
        while(high-low>1){int mid=(low+high)/2;if(lengths[mid]<target)low=mid;else high=mid;}
        double t=(low+(target-lengths[low])/Math.max(1e-15,lengths[high]-lengths[low]))/samples;
        return point(t,1);
    }
}

package dev.skylyrics.core;

/** Natural cubic interpolation, equivalent to Beautiful Lyrics' cubic-spline dependency. */
public final class NaturalSpline {
    private final double[] x, y, second;
    public NaturalSpline(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) throw new IllegalArgumentException("Invalid spline");
        this.x=x.clone(); this.y=y.clone(); int n=x.length; second=new double[n]; double[] u=new double[n];
        for(int i=1;i<n;i++) if(x[i]<=x[i-1]) throw new IllegalArgumentException("Unsorted spline");
        for(int i=1;i<n-1;i++) {
            double s=(x[i]-x[i-1])/(x[i+1]-x[i-1]), p=s*second[i-1]+2;
            second[i]=(s-1)/p;
            u[i]=(6*((y[i+1]-y[i])/(x[i+1]-x[i])-(y[i]-y[i-1])/(x[i]-x[i-1]))/(x[i+1]-x[i-1])-s*u[i-1])/p;
        }
        for(int i=n-2;i>=0;i--) second[i]=second[i]*second[i+1]+u[i];
    }
    public double at(double t) {
        t=Math.max(x[0],Math.min(x[x.length-1],t)); int lo=0,hi=x.length-1;
        while(hi-lo>1) { int m=(hi+lo)/2; if(x[m]>t)hi=m;else lo=m; }
        double h=x[hi]-x[lo],a=(x[hi]-t)/h,b=(t-x[lo])/h;
        return a*y[lo]+b*y[hi]+((a*a*a-a)*second[lo]+(b*b*b-b)*second[hi])*h*h/6;
    }
}

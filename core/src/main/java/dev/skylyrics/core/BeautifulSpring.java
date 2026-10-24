package dev.skylyrics.core;

/** Port of surfbryce's Universal/Modules/LegacySpring.ts, revision recorded in ATTRIBUTION.md. */
public final class BeautifulSpring {
    private double position, velocity;
    private final double damping, frequency;
    public double target;
    public BeautifulSpring(double initial,double damping,double frequency) {
        if(damping<0||frequency<0)throw new IllegalArgumentException("Spring does not converge");
        this.position=this.target=initial;this.damping=damping;this.frequency=frequency;
    }
    public void set(double value) { position=target=value;velocity=0; }
    public double update(double dt) {
        double w=frequency*2*Math.PI, o=position-target, v=velocity, decay=Math.exp(-damping*w*dt), p,nv;
        if(damping==1) {
            p=(o*(1+w*dt)+v*dt)*decay+target;
            nv=(v*(1-w*dt)-o*w*w*dt)*decay;
        } else if(damping<1) {
            double c=Math.sqrt(1-damping*damping),i=Math.cos(w*c*dt),j=Math.sin(w*c*dt);
            double a=dt*w,z=c>1e-4?j/c:a+((a*a*c*c*c*c/20-c*c)*a*a*a)/6;
            double b=w*c,y=b>1e-4?j/b:dt+((dt*dt*b*b*b*b/20-b*b)*dt*dt*dt)/6;
            p=(o*(i+damping*z)+v*y)*decay+target;
            nv=(v*(i-z*damping)-o*z*w)*decay;
        } else {
            double c=Math.sqrt(damping*damping-1),r1=-w*(damping-c),r2=-w*(damping+c);
            double co2=(v-o*r1)/(2*w*c),co1=o-co2,e1=co1*Math.exp(r1*dt),e2=co2*Math.exp(r2*dt);
            p=e1+e2+target;nv=e1*r1+e2*r2;
        }
        position=p;velocity=nv;return p;
    }
}

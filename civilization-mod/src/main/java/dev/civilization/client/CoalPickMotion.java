package dev.civilization.client;

/** Bounded camera-relative inertia. Advance once per rendered frame, never per hand. */
public final class CoalPickMotion {
    private double yaw,pitch,yawSpeed,pitchSpeed;
    public double yaw(){return yaw;}
    public double pitch(){return pitch;}
    public void reset(){yaw=pitch=yawSpeed=pitchSpeed=0;}
    public void advance(double cameraYaw,double cameraPitch,double seconds){
        if(!Double.isFinite(cameraYaw)||!Double.isFinite(cameraPitch)||!Double.isFinite(seconds)){reset();return;}
        // Keep the tool's previous world direction when the camera moves past it.
        yaw=Math.clamp(yaw-cameraYaw,-55,55);pitch=Math.clamp(pitch-cameraPitch,-45,45);
        double dt=Math.clamp(seconds,0,.1),omega=6,decay=Math.exp(-omega*dt);
        double y=yawSpeed+omega*yaw,p=pitchSpeed+omega*pitch;
        yaw=(yaw+y*dt)*decay;pitch=(pitch+p*dt)*decay;
        yawSpeed=(yawSpeed-omega*y*dt)*decay;pitchSpeed=(pitchSpeed-omega*p*dt)*decay;
        yaw=Math.clamp(yaw,-55,55);pitch=Math.clamp(pitch,-45,45);
    }
    public record Pose(double pitch,double yaw,double roll,double x,double y,double z){}
    /** Raise the grip and draw the head behind the shoulder, then strike in one plane. */
    public static Pose swing(double age,int contact,int duration){
        if(age<0||age>=duration)return new Pose(0,0,0,0,0,0);
        double wind=smooth(age/(contact-4.0));
        double hit=smooth((age-(contact-4))/4);
        double follow=smooth((age-contact)/3);
        double rest=1-smooth((age-contact-3)/(duration-contact-3.0));
        // Positive pitch brings the upright head back toward the shoulder;
        // negative pitch drives it forward/down. No scripted sideways spin.
        return new Pose((75*wind-95*hit-25*follow)*rest,0,0,
                (-.1*wind-.3*hit)*rest,(.48*wind+.1*hit-.22*follow)*rest,
                (-.22*wind-.06*hit-.04*follow)*rest);
    }
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
}

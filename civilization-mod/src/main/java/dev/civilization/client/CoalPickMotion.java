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
    /** Grip-centered backswing, accelerating contact, continued travel, then recovery. */
    public static Pose swing(double age,int contact,int duration){
        if(age<0||age>=duration)return new Pose(0,0,0,0,0,0);
        double wind=smooth(age/(contact-2.0));
        double hit=smooth((age-(contact-2))/2);
        double follow=smooth((age-contact)/3);
        double rest=1-smooth((age-contact-3)/(duration-contact-3.0));
        return new Pose((-95*wind+150*hit+30*follow)*rest,
                (38*wind-63*hit-12*follow)*rest,
                (-32*wind+74*hit+12*follow)*rest,
                (.12*wind-.27*hit)*rest,(.8*wind-.9*hit)*rest,
                (.18*wind-.34*hit-.04*follow)*rest);
    }
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
}

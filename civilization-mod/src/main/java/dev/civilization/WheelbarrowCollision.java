package dev.civilization;

import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;

/**
 * Where a wheelbarrow is solid, in the model frame (x right, y up, z rear): one outline from its ground to
 * the tray rim, drawn tight around the tray, frame and tyre. The same body meets terrain, other carriers,
 * entities and the mouse, and shows as the debug hitbox. The handles take mouse hits but never block.
 */
public final class WheelbarrowCollision {
    public static final double TRAY_TOP=15.525/16;
    /** Straight sides along the flared tray rim, tapering past the axle brackets to the tyre's tread. */
    public static final Solid BODY=new Solid(new double[]{-.531,.531,.531,.4185,.0616,-.0616,-.4185,-.531},new double[]{.82,.82,-.4556,-.9028,-1.1813,-1.1813,-.9028,-.4556},0,TRAY_TOP);
    // The wide box makes the handles easy to grab.
    private static final Solid HANDLES=Solid.of(new AABB(-.48,.45,12.69/16,.48,1.05,1.472));
    private WheelbarrowCollision(){}
    /** World point of a model-frame point on a carrier at `at` facing `yaw`. */
    public static Vec3 place(Vec3 at,float yaw,Vec3 model){return at.add(model.yRot((float)Math.toRadians(180d-yaw)));}
    /** World bounds of a model-frame box on a carrier at `at` facing `yaw`. */
    public static AABB place(Vec3 at,float yaw,AABB model){return Solid.of(model).place(at,yaw).bounds();}
    public static AABB bounds(Vec3 at,float yaw){return BODY.place(at,yaw).bounds();}
    public static AABB pickBounds(Vec3 at,float yaw){return bounds(at,yaw).minmax(HANDLES.place(at,yaw).bounds());}
    /** Nearest point where a ray meets the body or handles. */
    public static Optional<Vec3> pick(Vec3 at,float yaw,Vec3 from,Vec3 to){
        var body=BODY.place(at,yaw).clip(from,to);var handles=HANDLES.place(at,yaw).clip(from,to);
        if(body.isEmpty())return handles;
        return handles.filter(h->from.distanceToSqr(h)<from.distanceToSqr(body.get())).or(()->body);
    }
    /** An entity's horizontal move slid along the bodies it would walk into; it loses only the velocity that pressed into them. */
    public static Vec3 slide(Entity mover,Vec3 motion){
        var bodies=near(mover,motion);if(bodies.isEmpty())return motion;
        // Native order resolves height first: an entity rising past a rim this tick passes over it.
        var self=Solid.of(mover.getBoundingBox()).rise(motion.y);Vec3 slid=motion;
        for(var body:bodies)slid=self.slide(body,slid);
        double px=slid.x-motion.x,pz=slid.z-motion.z,length=Math.hypot(px,pz);
        if(length>0){
            var v=mover.getDeltaMovement();double into=(v.x*px+v.z*pz)/length;
            if(into<0)mover.setDeltaMovement(v.x-into*px/length,v.y,v.z-into*pz/length);
        }
        return slid;
    }
    /** An entity's move cut short where it comes down onto a tray, or up against a body overhead. */
    public static Vec3 land(Entity mover,Vec3 motion){
        if(motion.y==0)return motion;
        var self=Solid.of(mover.getBoundingBox());double dy=motion.y;
        for(var body:near(mover,motion))dy=self.reach(body,dy);
        return dy==motion.y?motion:new Vec3(motion.x,dy,motion.z);
    }
    private static List<Solid> near(Entity mover,Vec3 motion){
        return mover.level().getEntitiesOfClass(WheelbarrowEntity.class,mover.getBoundingBox().expandTowards(motion),e->!mover.isPassengerOfSameVehicle(e)).stream().map(WheelbarrowEntity::body).toList();
    }
}

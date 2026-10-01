package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Ghosts occlude other ghost outlines, while their textures remain transparent.
 * Uses only the small selected structure, never terrain scans or changes to world depth. */
final class GuideOutline {
    static void draw(PoseStack pose, VertexConsumer lines, AABB box, List<AABB> blockers,
                     Vec3 camera, int steps, float red, float green, float blue, float alpha) {
        // Every sight ray to this box stays inside this envelope. Discard distant
        // guide parts once, rather than testing them for every outline segment.
        var envelope=new AABB(Math.min(camera.x,box.minX),Math.min(camera.y,box.minY),Math.min(camera.z,box.minZ),
                Math.max(camera.x,box.maxX),Math.max(camera.y,box.maxY),Math.max(camera.z,box.maxZ)).inflate(.004);
        var nearby=new ArrayList<AABB>();
        var target = box.getCenter();
        var ray = target.subtract(camera);
        double rayLengthSqr = ray.lengthSqr();
        for(var blocker:blockers) {
            if (!blocker.intersects(envelope)) continue;
            // The whole target box subtends at most one block around its center ray.
            // Parts outside this conservative corridor cannot hide any of its edges.
            var center = blocker.getCenter();
            double t = rayLengthSqr == 0 ? 0 : Math.max(0, Math.min(1, center.subtract(camera).dot(ray) / rayLengthSqr));
            if (center.distanceToSqr(camera.add(ray.scale(t))) <= 3.25) nearby.add(blocker);
        }
        for(int axis=0;axis<3;axis++)for(int a=0;a<2;a++)for(int b=0;b<2;b++) {
            double[] start={box.minX,box.minY,box.minZ},end={box.minX,box.minY,box.minZ};
            double[] high={box.maxX,box.maxY,box.maxZ};
            int u=(axis+1)%3,v=(axis+2)%3;
            start[u]=end[u]=a==0?start[u]:high[u];start[v]=end[v]=b==0?start[v]:high[v];end[axis]=high[axis];
            var from=new Vec3(start[0],start[1],start[2]);var to=new Vec3(end[0],end[1],end[2]);
            int run=-1;
            for(int step=0;step<=steps;step++) {
                boolean visible=step<steps && visible(camera,from.lerp(to,(step+.5)/steps),nearby);
                if(visible && run<0)run=step;
                if(!visible && run>=0){segment(pose,lines,from.lerp(to,(double)run/steps),from.lerp(to,(double)step/steps),red,green,blue,alpha);run=-1;}
            }
        }
    }
    private static boolean visible(Vec3 eye,Vec3 point,List<AABB> blockers) {
        for(var blocker:blockers) {
            if(blocker.contains(eye))continue;
            double enter=0,exit=1;
            for(int axis=0;axis<3;axis++) {
                double origin=axis==0?eye.x:axis==1?eye.y:eye.z;
                double delta=(axis==0?point.x:axis==1?point.y:point.z)-origin;
                double low=axis==0?blocker.minX:axis==1?blocker.minY:blocker.minZ;
                double high=axis==0?blocker.maxX:axis==1?blocker.maxY:blocker.maxZ;
                if(Math.abs(delta)<1e-10){if(origin<low || origin>high){exit=-1;break;}}
                else {double a=(low-origin)/delta,b=(high-origin)/delta;enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));}
                if(enter>exit)break;
            }
            if(enter<=exit && enter<.999999)return false;
        }
        return true;
    }
    private static void segment(PoseStack pose,VertexConsumer lines,Vec3 a,Vec3 b,float r,float g,float blue,float alpha) {
        var normal=b.subtract(a).normalize();var entry=pose.last();
        lines.addVertex(entry,(float)a.x,(float)a.y,(float)a.z).setColor(r,g,blue,alpha).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
        lines.addVertex(entry,(float)b.x,(float)b.y,(float)b.z).setColor(r,g,blue,alpha).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
    }
}

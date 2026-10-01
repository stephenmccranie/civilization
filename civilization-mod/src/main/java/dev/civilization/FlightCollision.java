package dev.civilization;

/** Conservative continuous translation of a hull box against a static collision box. */
public final class FlightCollision {
    public record Box(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        public double sweep(Box obstacle,double dx,double dy,double dz){
            double enter=0,exit=1;
            double[] low={obstacle.minX-maxX,obstacle.minY-maxY,obstacle.minZ-maxZ};
            double[] high={obstacle.maxX-minX,obstacle.maxY-minY,obstacle.maxZ-minZ};
            double[] motion={dx,dy,dz};
            for(int axis=0;axis<3;axis++){
                double v=motion[axis];
                if(Math.abs(v)<1e-12){if(low[axis]>=0||high[axis]<=0)return 1;continue;}
                double a=low[axis]/v,b=high[axis]/v;
                enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));
                if(enter>=exit)return 1;
            }
            // Existing overlaps belong to Sable's contact solver; allow separation without trapping a resting hull.
            if(low[0]<0&&high[0]>0&&low[1]<0&&high[1]>0&&low[2]<0&&high[2]>0)return 1;
            return enter;
        }
    }
    private FlightCollision(){}
}

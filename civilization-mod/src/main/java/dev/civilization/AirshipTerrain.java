package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

/** Server-thread-only, expiring FULL (non-ticking) corridor tickets; no synchronous getChunk or future joins. */
final class AirshipTerrain {
    private static final TicketType<UUID> TICKET=TicketType.create("civilization_airship_corridor",UUID::compareTo,40);
    static final int WORLD_BUDGET=4096;
    static int update(AirshipSystem.Drive d,ServerLevel level,UUID id,Set<Long> cells,boolean request,int budget){
        if(d.ticketLevel!=level)release(d);
        var next=new LongLinkedOpenHashSet();
        int added=0;
        if(request)for(long cell:cells){
            if(next.size()>=budget)break;
            if(!d.tickets.contains(cell)&&added>=128)continue;
            if(!d.tickets.contains(cell)){added++;d.preparing.put(cell,System.nanoTime());}
            next.add(cell);
        }
        if(d.ticketLevel!=null)for(long cell:d.tickets)if(!next.contains(cell))level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(cell),0,id);
        boolean refresh=level.getGameTime()%20==0;
        for(long cell:next)if(refresh||!d.tickets.contains(cell))level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(cell),0,id);
        d.ticketLevel=level;d.ticketId=id;d.tickets=LongSets.unmodifiable(next);
        d.terrain=FlightTerrain.capture(cells,cell->level.getChunkSource().getChunkNow(FlightTerrain.x(cell),FlightTerrain.z(cell))!=null);
        long now=System.nanoTime();double delay=Math.max(.15,d.preparationSeconds-.01);
        var pending=d.preparing.long2LongEntrySet().fastIterator();
        while(pending.hasNext()){
            var entry=pending.next();long cell=entry.getLongKey();
            if(!next.contains(cell)){pending.remove();continue;}
            delay=Math.max(delay,Math.clamp((now-entry.getLongValue())/1e9,.15,.6));
            if(d.terrain.loaded().contains(cell))pending.remove();
        }
        d.preparationSeconds=delay;
        return next.size();
    }
    static void release(AirshipSystem.Drive d){
        if(d.ticketLevel!=null)for(long cell:d.tickets)d.ticketLevel.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(cell),0,d.ticketId);
        d.tickets=LongSets.EMPTY_SET;d.preparing.clear();d.preparationSeconds=.3;d.ticketLevel=null;d.ticketId=null;
    }
    /** Upload the swept terrain and conservatively shorten translations that would cross a solid shape.
     * Sable 2.0.5's custom terrain dispatcher does not implement CCD shape casts. */
    static boolean prepareCollision(ServerSubLevel ship,FlightTerrain.Snapshot terrain,Vec3 origin,Vector3d velocity,double seconds){
        var level=ship.getLevel();var system=SubLevelPhysicsSystem.get(level);if(system==null)return false;
        double endX=origin.x+velocity.x*seconds,endY=origin.y+velocity.y*seconds,endZ=origin.z+velocity.z*seconds;
        int minX=(int)Math.floor((Math.min(origin.x,endX)-24)/16),maxX=(int)Math.floor((Math.max(origin.x,endX)+24)/16);
        int minZ=(int)Math.floor((Math.min(origin.z,endZ)-24)/16),maxZ=(int)Math.floor((Math.max(origin.z,endZ)+24)/16);
        int minY=Math.max(level.getMinSection(),(int)Math.floor((Math.min(origin.y,endY)-24)/16));
        int maxY=Math.min(level.getMaxSection()-1,(int)Math.floor((Math.max(origin.y,endY)+24)/16));
        if(maxY-minY>16)return false;
        var bounds=ship.boundingBox();
        var hull=new FlightCollision.Box(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ());
        double dx=velocity.x*seconds,dy=velocity.y*seconds,dz=velocity.z*seconds,fraction=1;
        var swept=new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()).expandTowards(dx,dy,dz);
        int solid=0,examined=0;var pos=new BlockPos.MutableBlockPos();
        for(long cell:terrain.loaded()){
            int x=FlightTerrain.x(cell),z=FlightTerrain.z(cell);
            if(x<minX||x>maxX||z<minZ||z>maxZ)continue;
            var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)return false;
            for(int y=minY;y<=maxY;y++){
                var section=chunk.getSection(level.getSectionIndexFromSectionY(y));
                if(section.hasOnlyAir())continue;
                if(++solid>128)return false;
                // Sable's neighborhood baker can consult adjacent chunks; never make it synchronously load one.
                for(int a=x-1;a<=x+1;a++)for(int b=z-1;b<=z+1;b++)if(level.getChunkSource().getChunkNow(a,b)==null)return false;
                system.getTicketManager().addSectionIfNotTracked(level,section,SectionPos.of(x,y,z),system.getPipeline());
                // Empty sections cost no block scans. Cap dense/diagonal work rather than stall the server.
                int x0=Math.max(x*16,(int)Math.floor(swept.minX)-1),x1=Math.min(x*16+15,(int)Math.floor(swept.maxX)+1);
                int y0=Math.max(y*16,(int)Math.floor(swept.minY)-1),y1=Math.min(y*16+15,(int)Math.floor(swept.maxY)+1);
                int z0=Math.max(z*16,(int)Math.floor(swept.minZ)-1),z1=Math.min(z*16+15,(int)Math.floor(swept.maxZ)+1);
                for(int bx=x0;bx<=x1;bx++)for(int by=y0;by<=y1;by++)for(int bz=z0;bz<=z1;bz++){
                    if(++examined>32768)return false;
                    var state=section.getBlockState(bx&15,by&15,bz&15);if(state.isAir())continue;
                    pos.set(bx,by,bz);
                    for(var box:state.getCollisionShape(level,pos).toAabbs()){
                        var obstacle=new FlightCollision.Box(box.minX+bx,box.minY+by,box.minZ+bz,box.maxX+bx,box.maxY+by,box.maxZ+bz);
                        fraction=Math.min(fraction,hull.sweep(obstacle,dx,dy,dz));
                    }
                }
            }
        }
        if(fraction<1){
            double length=velocity.length()*seconds;
            velocity.mul(Math.max(0,fraction-.01/Math.max(.01,length)));
        }
        return true;
    }
}

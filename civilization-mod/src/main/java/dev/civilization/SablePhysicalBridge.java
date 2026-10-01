package dev.civilization;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;

/** Read-only pinned-Sable boundary. Does not rewrite live vessel mass or physics policies. */
public final class SablePhysicalBridge {
    private SablePhysicalBridge() {}
    public record Address(String dimension,UUID vessel,BlockPos local,Vec3 world) {}
    public static Address address(ServerLevel level,BlockPos local){
        var container=SubLevelContainer.getContainer(level);
        if(container!=null&&container.inBounds(local)){
            var plot=container.getPlot(new ChunkPos(local));
            if(plot==null||plot.getSubLevel()==null)throw new IllegalArgumentException("Unassigned Sable plot");
            var ship=plot.getSubLevel();var point=ship.logicalPose().transformPosition(new org.joml.Vector3d(local.getX()+.5,local.getY()+.5,local.getZ()+.5),new org.joml.Vector3d());
            return new Address(level.dimension().location().toString(),ship.getUniqueId(),local.immutable(),new Vec3(point.x,point.y,point.z));
        }
        return new Address(level.dimension().location().toString(),null,local.immutable(),local.getCenter());
    }
    public static double mass(ServerLevel level,BlockPos local){return PhysicsBlockPropertyHelper.getMass(level,local,level.getBlockState(local));}
    /** Same material data can feed a Sable static full-block property definition. No runtime mutation. */
    public static com.google.gson.JsonObject definition(String block,PhysicalMaterials.Profile material){
        var root=new com.google.gson.JsonObject();root.addProperty("selector",block);
        var props=new com.google.gson.JsonObject();props.addProperty("sable:mass",material.density());root.add("properties",props);return root;
    }
}

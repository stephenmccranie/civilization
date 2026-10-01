package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CivicAccess {
    /** World-space center for ordinary blocks and blocks carried by a Sable vessel. */
    public static Vec3 world(Level level, BlockPos pos) {
        if(level instanceof ServerLevel server){
            var airship=AirshipSystem.at(server,pos);if(airship!=null)return AirshipSystem.world(airship,pos);
            var boat=BoatSystem.at(server,pos);if(boat!=null)return BoatSystem.world(boat,pos);
        }
        return pos.getCenter();
    }
    public static CivicData.Claim claim(Level level, BlockPos pos) {
        pos=BlockPos.containing(world(level,pos));
        return level instanceof ServerLevel server ? CivicData.get(server.getServer()).activeAt(level.dimension().location().toString(), pos, System.currentTimeMillis()) : null;
    }
    public static boolean allowed(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)) return true;
        var airship=AirshipSystem.at(server,pos);
        if(airship!=null)return AirshipSystem.owner(airship,player);
        var boat=BoatSystem.at(server,pos);
        if(boat!=null){return BoatSystem.owner(boat,player);}
        var c = claim(level, pos);
        return c == null || player != null && (player.hasPermissions(2) || CivicData.get(server.getServer()).canUse(c, player.getUUID()));
    }
    /** Automation may cross boundaries only when both ends are unclaimed or belong to the same owner. */
    public static boolean boundary(Level level, BlockPos source, BlockPos target) {
        var a = claim(level, source); var b = claim(level, target);
        return a == null && b == null || a != null && b != null && a.owner.equals(b.owner);
    }
    public static boolean container(net.minecraft.world.Container container, Player player) {
        if (container instanceof net.minecraft.world.level.block.entity.BlockEntity entity && entity.getLevel() != null)
            return allowed(entity.getLevel(), entity.getBlockPos(), player);
        if (container instanceof dev.civilization.mixin.CompoundContainerAccess compound)
            return container(compound.civilization$first(), player) && container(compound.civilization$second(), player);
        return true;
    }
}

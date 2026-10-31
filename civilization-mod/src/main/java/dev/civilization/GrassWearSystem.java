package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=Civilization.MOD_ID)
public final class GrassWearSystem {
    private static final Map<ServerPlayer,Stride> STRIDES=new WeakHashMap<>();
    private static final Map<LevelChunk,Long2ByteOpenHashMap> CHANGED=new HashMap<>();
    private static final class Stride {ServerLevel level;Vec3 last;double distance;int airTicks=21;double groundY;}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p)step(p);}
    public static void step(ServerPlayer player){
        var stride=STRIDES.computeIfAbsent(player,p->new Stride());var level=player.serverLevel();var now=player.position();var last=stride.last;
        boolean same=stride.level==level;stride.level=level;stride.last=now;
        if(last==null||!same){stride.distance=0;stride.airTicks=player.onGround()&&!player.getAbilities().flying?0:21;stride.groundY=now.y;return;}
        if(player.isSpectator()||player.getAbilities().flying||player.isFallFlying()||player.isPassenger()||player.isInWater()||player.onClimbable()){
            stride.distance=0;stride.airTicks=21;return;
        }
        if(player.onGround()){stride.airTicks=0;stride.groundY=now.y;}
        else if(++stride.airTicks>20||now.y-stride.groundY>1.6){stride.distance=0;return;}
        double distance=now.subtract(last).horizontalDistance();
        if(distance>2||Math.abs(now.y-last.y)>1.1){stride.distance=0;stride.airTicks=21;return;}
        if(distance<1e-6)return;
        // Sample distance along the route, including ordinary hops, rather than counting landings.
        double along=.8-stride.distance;
        for(;along<=distance+1e-8;along+=.8){
            var foot=grassBelow(level,last.lerp(now,Math.min(1,along/distance)));
            if(foot==null){stride.distance=0;return;}
            record(level,foot);
        }
        stride.distance=.8-(along-distance);
    }
    private static net.minecraft.core.BlockPos grassBelow(ServerLevel level,Vec3 position){
        var top=net.minecraft.core.BlockPos.containing(position.x,position.y-1e-4,position.z);
        // Normal jumps rise about 1.25 blocks. Stop at the first physical surface, never
        // project through a roof/bridge or down a cliff, and never request unloaded terrain.
        for(int dy=0;dy<3;dy++){
            var at=top.below(dy);if(!level.hasChunkAt(at))return null;
            var state=level.getBlockState(at);
            if(!state.getFluidState().isEmpty())return null;
            if(!state.getCollisionShape(level,at).isEmpty())
                return state.is(Blocks.GRASS_BLOCK)&&position.y-(at.getY()+1)<=1.6?at:null;
        }
        return null;
    }
    public static boolean record(ServerLevel level,net.minecraft.core.BlockPos foot){
        if(!level.hasChunkAt(foot)||!level.getBlockState(foot).is(Blocks.GRASS_BLOCK))return false;
        var chunk=level.getChunkAt(foot);var wear=chunk.getData(GrassWearChunk.TYPE).wear;long key=GrassWear.key(foot.getX(),foot.getY(),foot.getZ());int before=GrassWear.stage(wear.steps(key));
        if(!wear.step(key))return false;
        chunk.setUnsaved(true);int after=GrassWear.stage(wear.steps(key));
        if(after!=before)CHANGED.computeIfAbsent(chunk,c->new Long2ByteOpenHashMap()).put(key,(byte)after);
        return true;
    }
    public static GrassWearPayload snapshot(LevelChunk chunk){
        return new GrassWearPayload(chunk.getLevel().dimension().location(),chunk.getPos().x,chunk.getPos().z,true,
                chunk.hasData(GrassWearChunk.TYPE)?chunk.getData(GrassWearChunk.TYPE).wear.visible():new long[0]);
    }
    @SubscribeEvent public static void sent(ChunkWatchEvent.Sent event){
        if(event.getPlayer().serverLevel()==event.getLevel()&&event.getChunk().hasData(GrassWearChunk.TYPE))PacketDistributor.sendToPlayer(event.getPlayer(),snapshot(event.getChunk()));
    }
    @SubscribeEvent public static void flush(ServerTickEvent.Post event){
        for(var entry:CHANGED.entrySet()){
            var chunk=entry.getKey();var level=(ServerLevel)chunk.getLevel();var pos=chunk.getPos();
            if(level.getChunkSource().getChunkNow(pos.x,pos.z)!=chunk)continue;
            var cells=new long[entry.getValue().size()];int i=0;for(var cell:entry.getValue().long2ByteEntrySet())cells[i++]=(cell.getLongKey()<<4)|cell.getByteValue();
            PacketDistributor.sendToPlayersTrackingChunk(level,pos,new GrassWearPayload(level.dimension().location(),pos.x,pos.z,false,cells));
        }
        CHANGED.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){STRIDES.clear();CHANGED.clear();}
    @SubscribeEvent public static void teleported(net.neoforged.neoforge.event.entity.EntityTeleportEvent event){if(event.getEntity() instanceof ServerPlayer p)STRIDES.remove(p);}
}

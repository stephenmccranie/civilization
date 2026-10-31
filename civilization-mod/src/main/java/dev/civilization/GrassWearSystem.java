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
    private static final class Stride {ServerLevel level;Vec3 last;double distance;}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p)step(p);}
    public static void step(ServerPlayer player){
        var stride=STRIDES.computeIfAbsent(player,p->new Stride());var level=player.serverLevel();var now=player.position();var last=stride.last;
        boolean same=stride.level==level;stride.level=level;stride.last=now;
        if(last==null||!same||!player.onGround()||player.isSpectator()||player.getAbilities().flying||player.isFallFlying()||player.isPassenger()||player.isInWater()||player.onClimbable()){
            stride.distance=0;return;
        }
        double distance=now.subtract(last).horizontalDistance();
        if(distance>2||Math.abs(now.y-last.y)>1.1){stride.distance=0;return;}
        if(distance<1e-6)return;
        var foot=player.getOnPos();
        if(!level.hasChunkAt(foot)||!level.getBlockState(foot).is(Blocks.GRASS_BLOCK)){stride.distance=0;return;}
        stride.distance+=distance;
        if(stride.distance+1e-8<.8)return;
        stride.distance=Math.max(0,stride.distance-.8);
        record(level,foot);
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

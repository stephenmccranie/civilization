package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Loaded-world source index. Rebuilt from chunks, never persisted or used to load terrain. */
@EventBusSubscriber(modid="civilization")
public final class BlockHeatSources {
    private static final class Sources {
        final LongOpenCustomHashSet lava=new LongOpenCustomHashSet(HeatMaps.POSITIONS),flames=new LongOpenCustomHashSet(HeatMaps.POSITIONS);
        final Long2ObjectOpenCustomHashMap<LongOpenCustomHashSet> chunks=new Long2ObjectOpenCustomHashMap<>(HeatMaps.POSITIONS);
        final LongLinkedOpenHashSet discovery=new LongLinkedOpenHashSet();
        final HeatSourceQueue emissions=new HeatSourceQueue();
        long scanning;int section,offset;boolean active;
    }
    private static final Direction[] NEIGHBORS=Direction.values();
    private static final Map<ServerLevel,Sources> SOURCES=new WeakHashMap<>();
    public static boolean torch(net.minecraft.world.level.block.state.BlockState state){
        return state.is(net.minecraft.world.level.block.Blocks.TORCH)||state.is(net.minecraft.world.level.block.Blocks.WALL_TORCH)
            ||state.is(net.minecraft.world.level.block.Blocks.SOUL_TORCH)||state.is(net.minecraft.world.level.block.Blocks.SOUL_WALL_TORCH);
    }
    public static boolean campfire(net.minecraft.world.level.block.state.BlockState state){
        return state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
    }
    public static boolean flame(net.minecraft.world.level.block.state.BlockState state){return torch(state)||campfire(state);}
    public static boolean source(net.minecraft.world.level.block.state.BlockState state){return flame(state)||state.getFluidState().is(FluidTags.LAVA);}
    private static final int LIMIT=4096;
    public static final double TEMPERATURE=1000; // Celsius internally; displays use Fahrenheit.
    public static void changed(ServerLevel level,BlockPos pos) {
        if(!ThermalConfig.enabled())return;
        if(!level.dimension().equals(Level.OVERWORLD))return;
        if(!level.getServer().isSameThread()){var copy=pos.immutable();level.getServer().execute(()->changed(level,copy));return;}
        var sources=SOURCES.computeIfAbsent(level,k->new Sources());
        long key=pos.asLong();
        var state=level.getBlockState(pos);
        if(!state.getFluidState().is(FluidTags.LAVA))sources.lava.remove(key);
        else if(sources.lava.size()<LIMIT)sources.lava.add(key);
        if(!flame(state))sources.flames.remove(key);
        else if(sources.flames.size()<LIMIT)sources.flames.add(key);
        long chunk=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);
        if(sources.lava.contains(key)||sources.flames.contains(key))
            sources.chunks.computeIfAbsent(chunk,k->new LongOpenCustomHashSet(HeatMaps.POSITIONS)).add(key);
        else {
            var index=sources.chunks.get(chunk);
            if(index!=null){index.remove(key);if(index.isEmpty())sources.chunks.remove(chunk);}
        }
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(!ThermalConfig.enabled())return;
        if(!(event.getChunk() instanceof LevelChunk chunk)||!(chunk.getLevel() instanceof ServerLevel level)
                ||!level.dimension().equals(Level.OVERWORLD))return;
        // Queue coordinates only: never retain a chunk or scan its blocks in the load callback.
        long key=chunk.getPos().toLong();
        level.getServer().execute(()->enqueue(level,key));
    }
    static void enqueue(ServerLevel level,long key){SOURCES.computeIfAbsent(level,k->new Sources()).discovery.add(key);}
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        long key=event.getChunk().getPos().toLong();
        level.getServer().execute(()->forget(level,key));
    }
    static void forget(ServerLevel level,long key){
        var sources=SOURCES.get(level);if(sources==null)return;
        var positions=sources.chunks.remove(key);
        if(positions!=null){sources.lava.removeAll(positions);sources.flames.removeAll(positions);}
        sources.discovery.remove(key);
        if(sources.active&&sources.scanning==key)sources.active=false;
    }
    static void discover(ServerLevel level){
        var sources=SOURCES.get(level);if(sources==null||!ThermalConfig.enabled())return;
        long deadline=System.nanoTime()+500_000;
        for(int work=0;work<512&&System.nanoTime()<deadline;work++){
            if(!sources.active){
                if(sources.discovery.isEmpty())return;
                sources.scanning=sources.discovery.removeFirstLong();sources.section=0;sources.offset=0;sources.active=true;
            }
            var pos=new net.minecraft.world.level.ChunkPos(sources.scanning);
            var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);
            if(chunk==null||sources.section==chunk.getSectionsCount()){sources.active=false;continue;}
            var section=chunk.getSection(sources.section);
            if(sources.offset==0&&!section.maybeHas(BlockHeatSources::source)){sources.section++;continue;}
            int cell=sources.offset++,x=cell&15,z=(cell>>4)&15,y=cell>>8;
            if(source(section.getBlockState(x,y,z)))changed(level,new BlockPos(pos.getMinBlockX()+x,chunk.getSectionYFromSectionIndex(sources.section)*16+y,pos.getMinBlockZ()+z));
            if(sources.offset==4096){sources.offset=0;sources.section++;}
        }
    }
    static void scheduledEmit(ServerLevel level,ThermalField field){
        if(!ThermalConfig.enabled())return;
        var sources=SOURCES.get(level);if(sources==null)return;
        // The chunk index is also the unique source registry; no per-tick copy of every source.
        var keys=new it.unimi.dsi.fastutil.longs.AbstractLongCollection(){
            public int size(){return sources.lava.size()+sources.flames.size();}
            public LongIterator iterator(){return LongIterators.concat(sources.lava.iterator(),sources.flames.iterator());}
        };
        sources.emissions.tick(level.getGameTime(),keys,key->emitSource(level,field,sources,key));
    }
    static void emit(ServerLevel level,ThermalField field) {
        if(!ThermalConfig.enabled())return;
        var sources=SOURCES.get(level);if(sources==null)return;
        // Explicit whole-second path for deterministic checks; production uses scheduledEmit.
        for(long key:sources.lava.toLongArray())emitSource(level,field,sources,key);
        for(long key:sources.flames.toLongArray())emitSource(level,field,sources,key);
    }
    private static void emitSource(ServerLevel level,ThermalField field,Sources sources,long key){
        var p=BlockPos.of(key);
        if(!ThermalField.supported(level,p))return;
        var state=level.getBlockState(p);
        var lava=state.getFluidState();
        if(lava.is(FluidTags.LAVA)&&sources.lava.contains(key)){
            double fraction=lava.isSource()?1:lava.getAmount()/8d;
            for(var direction:NEIGHBORS) {
                var q=p.relative(direction);
                if(!ThermalField.supported(level,q)||level.getFluidState(q).is(FluidTags.LAVA))continue;
                double temperature=field.localTemperature(level,q);
                var neighbor=level.getBlockState(q);
                double rate=neighbor.isAir()?1:ThermalField.conductance(neighbor);
                double heat=Math.min(ThermalRules.LAVA_FACE_HEAT*rate,Math.max(0,TEMPERATURE-temperature)*.08)*fraction;
                if(heat>0)field.add(level,q,heat);
            }
        }else if(flame(state)&&sources.flames.contains(key)){
            var air=new ArrayList<BlockPos>(7);air.add(p);
            for(var direction:NEIGHBORS){
                var q=p.relative(direction);
                if(ThermalField.supported(level,q)&&level.getBlockState(q).isAir())air.add(q);
            }
            double heat=(campfire(state)?ThermalRules.CAMPFIRE_HEAT:ThermalRules.TORCH_HEAT)/air.size();
            for(var q:air)field.add(level,q,heat);
        }else changed(level,p);
    }
}

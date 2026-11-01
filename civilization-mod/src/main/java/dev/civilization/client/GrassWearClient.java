package dev.civilization.client;

import dev.civilization.*;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.*;

/** Immutable published chunk maps are safe for Grassier Grass's asynchronous mesh builder. */
@EventBusSubscriber(modid=Civilization.MOD_ID,value=Dist.CLIENT)
public final class GrassWearClient {
    private static volatile Level world;
    private static final ConcurrentHashMap<Long,Long2ByteOpenHashMap> CHUNKS=new ConcurrentHashMap<>();
    private static boolean cacheHookFailed;
    private static java.lang.reflect.Method dirtySection;
    public static float heightScale(int x,int y,int z){
        var map=CHUNKS.get(ChunkPos.asLong(x>>4,z>>4));
        return GrassWear.heightScale(map==null?0:map.get(GrassWear.key(x,y,z)));
    }
    public static void accept(Level level,GrassWearPayload payload){
        if(!(level instanceof ClientLevel client)||!level.dimension().location().equals(payload.dimension())||!client.getChunkSource().hasChunk(payload.chunkX(),payload.chunkZ()))return;
        if(world!=level){CHUNKS.clear();world=level;}
        long chunk=ChunkPos.asLong(payload.chunkX(),payload.chunkZ());var old=CHUNKS.get(chunk);
        var next=payload.replace()||old==null?new Long2ByteOpenHashMap():new Long2ByteOpenHashMap(old);
        for(long entry:payload.cells()){
            long key=entry>>4;int stage=(int)(entry&15);
            if(GrassWear.y(key)<level.getMinBuildHeight()||GrassWear.y(key)>=level.getMaxBuildHeight())continue;
            if(next.containsKey(key)||next.size()<GrassWear.MAX_CELLS)next.put(key,(byte)Math.max(next.get(key),stage));
        }
        var dirty=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
        if(old!=null)for(var entry:old.long2ByteEntrySet())if(next.get(entry.getLongKey())!=entry.getByteValue())dirty.add(GrassWear.y(entry.getLongKey())>>4);
        for(var entry:next.long2ByteEntrySet())if(old==null||old.get(entry.getLongKey())!=entry.getByteValue())dirty.add(GrassWear.y(entry.getLongKey())>>4);
        if(next.isEmpty())CHUNKS.remove(chunk);else CHUNKS.put(chunk,next);
        if(!supported()||cacheHookFailed)return;
        try{for(int y:dirty)invalidateSection(SectionPos.asLong(payload.chunkX(),y,payload.chunkZ()));}
        catch(LinkageError|ReflectiveOperationException|RuntimeException ex){cacheHookFailed=true;org.slf4j.LoggerFactory.getLogger("Civilization/GrassierGrass").warn("Grass wear cache hook unavailable; keeping saved wear but leaving this renderer unchanged",ex);}
    }
    /** Package-private optional renderer class cannot be called through a public static invoker.
     * Resolve once; reflection is only used per changed section, never per frame/blade. */
    public static void invalidateSection(long section) throws ReflectiveOperationException {
        if(dirtySection==null){
            dirtySection=Class.forName("com.leonardoinc22.shortgrass.client.render.GrassSectionCache").getDeclaredMethod("markSectionDirty",long.class);
            dirtySection.setAccessible(true);
        }
        dirtySection.invoke(null,section);
    }
    private static boolean supported(){return net.neoforged.fml.ModList.get().getModContainerById("grassiergrass").map(m->m.getModInfo().getVersion().toString().equals("1.4.5")).orElse(false);}
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event){if(event.getLevel()==world)CHUNKS.remove(event.getChunk().getPos().toLong());}
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){if(event.getLevel()==world){CHUNKS.clear();world=null;}}
}

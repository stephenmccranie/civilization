package dev.civilization;

import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Creates the canonical flat chunk using Minecraft, before offline corridor replication. */
@EventBusSubscriber(modid="civilization")
public final class FlightBenchmarkSeed {
    private static int ticks;private static Path world;
    @SubscribeEvent public static void tick(ServerTickEvent.Post e)throws Exception{
        if(!Boolean.getBoolean("civilization.flightBenchmarkSeed"))return;
        var l=e.getServer().overworld();
        if(!(l.getChunkSource().getGenerator() instanceof FlatLevelSource))throw new IllegalStateException("Benchmark requires flat generator");
        if(++ticks==1){
            l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,e.getServer());
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,e.getServer());
            l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,e.getServer());
            l.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,e.getServer());
            l.getGameRules().getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0,e.getServer());
            l.setDayTime(6000);l.setWeatherParameters(1000000,0,false,false);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.getChunk(x,z);
        }
        if(ticks==80){
            world=e.getServer().getWorldPath(LevelResource.ROOT);
            var tag=ChunkSerializer.write(l,l.getChunk(0,0));
            if(!tag.getString("Status").equals("minecraft:full")||!tag.getList("block_entities",10).isEmpty())throw new IllegalStateException("Invalid flat template");
            NbtIo.writeCompressed(tag,world.resolve("flat-template.nbt"));
            System.out.println("FLIGHT_BENCHMARK_TEMPLATE_PASS");e.getServer().halt(false);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e)throws Exception{
        if(world==null)return;
        var p=world.resolve("level.dat");var tag=NbtIo.readCompressed(p,NbtAccounter.unlimitedHeap());var d=tag.getCompound("Data");
        d.putString("LevelName","Civilization Flight Benchmark");d.putBoolean("allowCommands",true);d.putInt("GameType",1);d.putInt("SpawnX",0);d.putInt("SpawnY",100);d.putInt("SpawnZ",0);
        var dims=d.getCompound("WorldGenSettings").getCompound("dimensions");for(var key:java.util.List.copyOf(dims.getAllKeys()))if(key.startsWith("civilization:test_"))dims.remove(key);
        NbtIo.writeCompressed(tag,p);
    }
}

package dev.civilization;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Immutable server-thread publication; C2ME workers never inspect ships or players. */
public final class FlightChunkHints {
    public record Hint(int chunkX,int chunkZ,int heading){}
    private record Snapshot(Map<Object,List<Hint>> worlds,long time){}
    private static volatile Snapshot snapshot=new Snapshot(Map.of(),0);
    public static final AtomicLong reordered=new AtomicLong();
    private FlightChunkHints(){}
    public static void publish(Map<Object,List<Hint>> worlds){
        Map<Object,List<Hint>> copy=new IdentityHashMap<>();
        worlds.forEach((world,hints)->copy.put(world,List.copyOf(hints)));
        snapshot=new Snapshot(Collections.unmodifiableMap(copy),System.nanoTime());
    }
    public static void clear(){snapshot=new Snapshot(Map.of(),0);}
    public static int heading(Object chunkMap,long source){
        var state=snapshot;if(System.nanoTime()-state.time()>1_000_000_000L)return -1;
        var hints=state.worlds().get(chunkMap);if(hints==null)return -1;
        int best=4,heading=-1;
        for(var hint:hints){
            int distance=Math.max(Math.abs(FlightChunkOrder.x(source)-hint.chunkX()),Math.abs(FlightChunkOrder.z(source)-hint.chunkZ()));
            if(distance<best){best=distance;heading=hint.heading();}
        }
        return heading;
    }
    public interface OrderedIterator { void civilization$setHeading(int heading); }
}

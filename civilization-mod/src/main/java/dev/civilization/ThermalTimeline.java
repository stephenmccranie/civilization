package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.nbt.*;

/** Bounded quarter-second input history. Work scheduling cannot reorder heat releases. */
final class ThermalTimeline {
    private final NavigableMap<Long,Long2DoubleOpenCustomHashMap> frames=new TreeMap<>();
    private int entries;
    private final int capacity;
    ThermalTimeline(){this(ThermalRules.MAX_CELLS);}
    ThermalTimeline(int capacity){this.capacity=capacity;}
    long committed=Long.MIN_VALUE,observed=Long.MIN_VALUE;
    void resume(long now){
        if(observed==Long.MIN_VALUE){committed=observed=now;return;}
        long shift=now-observed;
        if(shift!=0){
            var moved=new TreeMap<Long,Long2DoubleOpenCustomHashMap>();
            frames.forEach((tick,values)->moved.put(tick+shift,values));frames.clear();frames.putAll(moved);
            committed+=shift;
        }
        observed=now;
    }
    void observe(long now){observed=now;}
    int headroom(){return capacity-entries;}
    int entries(){return entries;}
    boolean empty(){return frames.isEmpty();}
    boolean add(long key,double amount,long closedThrough){
        long end=committed+Math.max(1,(Math.max(0,observed-committed)+4)/5)*5;
        end=Math.max(end,closedThrough+5);
        var frame=frames.get(end);
        if((frame==null||!frame.containsKey(key))&&headroom()==0)return false;
        if(frame==null){frame=new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);frames.put(end,frame);}
        if(!frame.containsKey(key))entries++;
        frame.addTo(key,amount);return true;
    }
    List<Long2DoubleOpenCustomHashMap> through(long end){return List.copyOf(frames.headMap(end,true).values());}
    void commit(long end){
        var iterator=frames.headMap(end,true).values().iterator();
        while(iterator.hasNext()){entries-=iterator.next().size();iterator.remove();}
        committed=end;
    }
    double heat(){double sum=0;for(var f:frames.values())sum+=f.values().doubleStream().sum();return sum;}
    CompoundTag save(){
        var tag=new CompoundTag();tag.putLong("committed",committed);tag.putLong("observed",observed);
        var list=new ListTag();frames.forEach((tick,values)->values.forEach((position,energy)->{
            var entry=new CompoundTag();entry.putLong("tick",tick);entry.putLong("pos",position);entry.putDouble("energy",energy);list.add(entry);
        }));tag.put("releases",list);return tag;
    }
    void load(CompoundTag tag){
        if(!tag.contains("committed")||!tag.contains("observed"))return;
        committed=tag.getLong("committed");observed=tag.getLong("observed");
        for(var item:tag.getList("releases",10)){
            var e=(CompoundTag)item;long tick=e.getLong("tick"),key=e.getLong("pos");double heat=e.getDouble("energy");
            if(entries>=capacity||tick<=committed||!Double.isFinite(heat))continue;
            var frame=frames.computeIfAbsent(tick,k->new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS));
            if(!frame.containsKey(key))entries++;frame.addTo(key,heat);
        }
    }
}

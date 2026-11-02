package dev.civilization;

import java.util.*;

/** Bounded RTT samples, timestamped with a monotonic clock. Network replies may arrive off-thread. */
public final class LatencyWindow {
    public static final long WINDOW_MS=5_000,PROBE_INTERVAL_MS=1_000;
    private static final long PENDING_MS=30_000;
    private static final int LIMIT=32;
    public record Reading(int milliseconds,boolean overdue){
        public String label(){return milliseconds<0?"-- ms":(overdue?"\u2265 ":"")+milliseconds+" ms";}
    }
    private record Sample(long received,int elapsed){}
    private final LinkedHashMap<Integer,Long> pending=new LinkedHashMap<>();
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private int nextId;
    public LatencyWindow(int firstId){nextId=firstId;}
    public synchronized int probe(long now){
        expire(now);while(pending.size()>=LIMIT)pending.remove(pending.keySet().iterator().next());
        int id=nextId++;pending.put(id,now);return id;
    }
    public synchronized boolean reply(int id,long now){
        var sent=pending.remove(id);
        if(sent==null||now<sent||now-sent>=PENDING_MS)return false;
        expire(now);samples.addLast(new Sample(now,(int)(now-sent)));
        while(samples.size()>LIMIT)samples.removeFirst();return true;
    }
    public synchronized Reading reading(long now){
        expire(now);
        if(!pending.isEmpty()){
            long elapsed=now-pending.values().iterator().next();
            if(elapsed>=WINDOW_MS)return new Reading((int)Math.min(Integer.MAX_VALUE,elapsed),true);
        }
        return samples.isEmpty()?new Reading(-1,false):new Reading((int)Math.round(samples.stream().mapToInt(Sample::elapsed).average().orElseThrow()),false);
    }
    private void expire(long now){
        while(!samples.isEmpty()&&now-samples.peekFirst().received>=WINDOW_MS)samples.removeFirst();
        pending.entrySet().removeIf(entry->now-entry.getValue()>=PENDING_MS);
    }
}

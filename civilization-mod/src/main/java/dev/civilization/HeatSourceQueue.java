package dev.civilization;

import it.unimi.dsi.fastutil.longs.LongCollection;
import java.util.function.LongConsumer;

/** Fair, bounded recurring work. Overload delays a source, never accumulates catch-up bursts. */
final class HeatSourceQueue {
    private long[] keys=new long[0];
    private int cursor;
    private long next;
    void tick(long time,LongCollection sources,LongConsumer action){
        if(cursor==keys.length){
            if(time<next)return;
            keys=sources.toLongArray();cursor=0;next=time+20;
        }
        long deadline=System.nanoTime()+1_000_000;
        int count=Math.min(256,Math.max(1,(keys.length+19)/20));
        do {if(cursor==keys.length)return;action.accept(keys[cursor++]);}
        while(--count>0&&System.nanoTime()<deadline);
    }
}

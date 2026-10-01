package dev.civilization.client;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Shared performance policy for every multiblock guide, including custom models. */
public final class GuidePerformance {
    public static final int CHECK_INTERVAL_TICKS=10;
    public static final int MAX_OUTLINES=64;
    private GuidePerformance() {}
    /** Call once per client tick. Selection/build keys invalidate immediately. */
    public static final class Refresh {
        private Object key;
        private int remaining;
        public boolean due(Object next){
            if(!Objects.equals(key,next)||remaining--<=0){key=next;remaining=CHECK_INTERVAL_TICKS-1;return true;}
            return false;
        }
        public void reset(){key=null;remaining=0;}
    }
    public static int outlineSteps(int parts){return parts>128?6:16;}
    /** Cull first, cap even small guides, and retain the aimed part within the cap. */
    public static <T> List<T> outlines(List<T> candidates,Predicate<T> visible,ToDoubleFunction<T> distance,T aimed){
        var result=new ArrayList<T>();for(var part:candidates)if(visible.test(part))result.add(part);
        if(result.size()>MAX_OUTLINES){
            boolean keep=aimed!=null&&result.contains(aimed);
            result.sort(Comparator.comparingDouble(distance));result.subList(MAX_OUTLINES,result.size()).clear();
            if(keep&&!result.contains(aimed))result.set(MAX_OUTLINES-1,aimed);
        }
        return result;
    }
}

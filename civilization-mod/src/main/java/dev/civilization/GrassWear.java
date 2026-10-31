package dev.civilization;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

/** Persistent footfall counts, independent of clocks. Height changes only at eight-step thresholds. */
public final class GrassWear {
    public static final int STEPS_PER_STAGE=8,STAGES=8,MAX_STEPS=STEPS_PER_STAGE*STAGES,MAX_CELLS=16384;
    private final Long2ByteOpenHashMap cells=new Long2ByteOpenHashMap();
    // Chunk-local X/Z, full signed Y: do not alias tall Terrain Diffusion surfaces.
    public static long key(int x,int y,int z){return ((long)y<<8)|((z&15)<<4)|(x&15);}
    public static int y(long key){return (int)(key>>8);}
    public static int stage(int steps){return Math.clamp(steps/STEPS_PER_STAGE,0,STAGES);}
    public static float heightScale(int stage){return 1-.8f*Math.clamp(stage,0,STAGES)/STAGES;}
    public int steps(long key){return cells.get(key);}
    public int size(){return cells.size();}
    public boolean step(long key){
        int old=steps(key);if(old==MAX_STEPS||old==0&&size()>=MAX_CELLS)return false;
        cells.put(key,(byte)(old+1));return true;
    }
    public long[] save(){
        var result=new long[cells.size()];int i=0;
        for(var e:cells.long2ByteEntrySet())result[i++]=(e.getLongKey()<<7)|e.getByteValue();
        java.util.Arrays.sort(result);return result;
    }
    public void load(long[] saved){
        cells.clear();
        for(long entry:saved){
            long key=entry>>7;int steps=(int)(entry&127);
            if(steps<1||steps>MAX_STEPS||key>>8!=(long)y(key))continue;
            if(size()>=MAX_CELLS)break;
            cells.put(key,(byte)Math.max(steps,steps(key)));
        }
    }
    public long[] visible(){
        var result=new it.unimi.dsi.fastutil.longs.LongArrayList();
        for(var e:cells.long2ByteEntrySet()){int stage=stage(e.getByteValue());if(stage>0)result.add((e.getLongKey()<<4)|stage);}
        return result.toLongArray();
    }
}

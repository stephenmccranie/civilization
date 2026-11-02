package dev.civilization;

/** Permanent climate and deterministic district opportunities; these are not stock quotas. */
public final class DepositDistribution {
    static final int CELL=256, REGION=1024, FAVORED_RARITY=8, TEMPERATE_RARITY=16;
    public enum Climate { COLD, TEMPERATE, HOT }
    private DepositDistribution(){}
    private static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    /** Biome temperature index, independent of altitude, weather and player heat. */
    public static Climate climate(float temperature){return temperature<.3f?Climate.COLD:temperature>=.9f?Climate.HOT:Climate.TEMPERATE;}
    static long district(long seed,int x,int z,boolean coal){
        return mix(seed ^ ((long)x*0x632be59bd9b4e019L) ^ ((long)z*0x9e3779b97f4a7c15L) ^ (coal?0x2a969f4cb37d518dL:0));
    }
    private static long roll(long district){return mix(district^0x6d487bb7581a72c9L);}
    /** Thin whole oil districts independently, preserving the search order and every retained site. */
    static boolean selected(long district,boolean coal){
        return Math.floorMod(roll(district),FAVORED_RARITY)==0 && (coal || (mix(district^0x3c79ac492ba7b653L)&1)==0);
    }
    static boolean allows(long district,boolean coal,Climate climate){
        if(!selected(district,coal))return false;
        if(coal&&climate==Climate.HOT || !coal&&climate==Climate.COLD)return false;
        return Math.floorMod(roll(district),climate==Climate.TEMPERATE?TEMPERATE_RARITY:FAVORED_RARITY)==0;
    }
    /** A seed-stable permutation visits every cell in the district, including for negative coordinates. */
    static <T> T search(long district,java.util.function.IntFunction<T> sample){
        int start=(int)Math.floorMod(district,16),step=((int)(district>>>8)&7)*2+1;
        for(int n=0;n<16;n++){var result=sample.apply((start+n*step)&15);if(result!=null)return result;}
        return null;
    }
}

package dev.civilization;

/** Candidate opportunities average pi/2 per kind in a 2,048-block town radius; geology still gates placement. */
public final class DepositDistribution {
    static final int COAL_CELL=256, COAL_RARITY=128;
    static final int OIL_REGION=1024, OIL_RARITY=8;
    static final int URANIUM_DISTRICT=512, URANIUM_RARITY=32;
    private DepositDistribution(){}
    private static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    static boolean coal(long siteHash){return Math.floorMod(siteHash,COAL_RARITY)==0;}
    static boolean oil(long regionHash){return Math.floorMod(mix(regionHash^0x6d487bb7581a72c9L),OIL_RARITY)==0;}
    public static boolean uranium(long seed,int blockX,int blockZ){
        int x=Math.floorDiv(blockX,URANIUM_DISTRICT),z=Math.floorDiv(blockZ,URANIUM_DISTRICT);
        long hash=mix(seed^((long)x*341873128712L)^((long)z*132897987541L)^0x1c69b3f74ac4ae35L);
        return Math.floorMod(hash,URANIUM_RARITY)==0;
    }
}

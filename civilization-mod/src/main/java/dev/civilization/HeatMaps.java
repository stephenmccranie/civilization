package dev.civilization;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongHash;

/** Packed block-coordinate grids need all three axes avalanched before table indexing. */
final class HeatMaps {
    private HeatMaps(){}
    static final LongHash.Strategy POSITIONS=new LongHash.Strategy(){
        public int hashCode(long key){long mixed=HashCommon.murmurHash3(key);return (int)(mixed^(mixed>>>32));}
        public boolean equals(long a,long b){return a==b;}
    };
}

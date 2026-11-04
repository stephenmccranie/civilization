package dev.civilization;

import java.util.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a store's goods lie in its grid: one block state per eighth of a cell, filling a cell at a time row by row from
 * the grid's first row, then the next layer up, in the order the goods arrived. Slabs and saw-cut pieces fill eighths
 * by volume; any other block, such as stairs or a machine, fills a whole cell. Stock beyond the grid is left out.
 * Equal layouts draw alike, so their drawing is shared.
 */
public final class CargoLayout {
    public final CargoGrid grid;
    public final List<BlockState> eighths;
    private final int hash;
    private CargoLayout(CargoGrid grid,List<BlockState> eighths){this.grid=grid;this.eighths=eighths;hash=grid.hashCode()*31+eighths.hashCode();}
    static CargoLayout of(List<CargoLedger.Entry> stock,CargoGrid grid){
        int room=grid.capacity()*8;var eighths=new ArrayList<BlockState>();
        for(var entry:stock){
            double volume=CargoMass.volume(entry.item());
            if(!(volume>0))continue; // Unknown legacy goods still count against capacity but have no shape to draw.
            var state=CargoMass.material(entry.item());int each=(int)Math.round(volume*8);
            for(long n=0;n<entry.count()&&eighths.size()<room;n++)for(int k=0;k<each&&eighths.size()<room;k++)eighths.add(state);
        }
        return new CargoLayout(grid,List.copyOf(eighths));
    }
    public int cells(){return (eighths.size()+7)/8;}
    /** The block filling all of `cell`, or null when the cell holds pieces, is not full or lies outside the goods. */
    public BlockState whole(int cell){
        int first=cell*8;
        if(cell<0||first+8>eighths.size())return null;
        for(int k=1;k<8;k++)if(eighths.get(first+k)!=eighths.get(first))return null;
        return eighths.get(first);
    }
    @Override public boolean equals(Object o){return o instanceof CargoLayout l&&hash==l.hash&&grid.equals(l.grid)&&eighths.equals(l.eighths);}
    @Override public int hashCode(){return hash;}
}

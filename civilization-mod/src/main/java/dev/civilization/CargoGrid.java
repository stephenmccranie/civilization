package dev.civilization;

/**
 * A cargo store's interior as a grid of quarter-size cells, one stored block each, so goods pack 64 to a
 * block of space. Its capacity and its drawing both come from this grid. A layer may be shorter than a cell
 * is wide so the stack fits its store, such as a pallet's stack above the timber.
 */
public record CargoGrid(int across,int along,int layers,float height){
    /** The width of one stored block's cell. */
    public static final float CELL=.25f;
    public int layer(){return across*along;}
    public int capacity(){return layer()*layers;}
}

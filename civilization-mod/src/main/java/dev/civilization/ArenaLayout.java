package dev.civilization;

import java.util.*;

/** Immutable controller-local geometry. Cell (0,0) is exactly the oval's center. */
public final class ArenaLayout {
    public record Cell(int x, int z) {}
    public record Piece(int x, int y, int z, boolean slab, boolean gate) {}
    public static final Set<Cell> OVAL;
    public static final Set<Cell> FLOOR;
    public static final List<Piece> PIECES;
    public static final List<Piece> GATES;
    public static final Set<Cell> FIGHTING;
    static {
        var oval = new LinkedHashSet<Cell>();
        for (int x=-14;x<=14;x++) for (int z=-9;z<=9;z++)
            if (x*x/(14.5*14.5)+z*z/(9.5*9.5)<=1) oval.add(new Cell(x,z));
        var wall = new HashSet<Cell>();
        for (var c:oval) if (!oval.contains(new Cell(c.x+1,c.z)) || !oval.contains(new Cell(c.x-1,c.z))
                || !oval.contains(new Cell(c.x,c.z+1)) || !oval.contains(new Cell(c.x,c.z-1))) wall.add(c);
        var pieces = new ArrayList<Piece>();
        for (var c:oval) if(wall.contains(c)) {
            if(Math.abs(c.x)==14 && Math.abs(c.z)<=1) pieces.add(new Piece(c.x,1,c.z,false,true));
            else { pieces.add(new Piece(c.x,1,c.z,false,false)); pieces.add(new Piece(c.x,2,c.z,false,false)); pieces.add(new Piece(c.x,3,c.z,true,false)); }
        }
        var floor = new LinkedHashSet<>(oval);
        for(int side:new int[]{-1,1}) {
            for(int d=14;d<=20;d++) for(int z=-3;z<=3;z++) floor.add(new Cell(side*d,z));
            for(int d=14;d<=20;d++) for(int z:new int[]{-3,3}) for(int y=1;y<=3;y++) pieces.add(new Piece(side*d,y,z,false,false));
            for(int z=-2;z<=2;z++) if(z!=0 && z!=1) for(int y=1;y<=3;y++) pieces.add(new Piece(side*20,y,z,false,false));
            for(int d=14;d<=20;d++) for(int z=-3;z<=3;z++) pieces.add(new Piece(side*d,4,z,true,false));
        }
        var fighting=new LinkedHashSet<>(oval); fighting.removeAll(wall);
        OVAL=Collections.unmodifiableSet(oval); FLOOR=Collections.unmodifiableSet(floor);
        FIGHTING=Collections.unmodifiableSet(fighting); PIECES=List.copyOf(pieces);GATES=PIECES.stream().filter(Piece::gate).toList();
    }
    public static boolean prep(int side,int x,int z) { return x*(side==0?-1:1)>=15 && x*(side==0?-1:1)<=19 && Math.abs(z)<=2; }
    private ArenaLayout() {}
}

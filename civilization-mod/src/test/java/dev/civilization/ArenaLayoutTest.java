package dev.civilization;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaLayoutTest {
    @Test void ovalAndControllerAreExactlyCentered(){
        for(var c:ArenaLayout.OVAL){assertTrue(ArenaLayout.OVAL.contains(new ArenaLayout.Cell(-c.x(),c.z())));assertTrue(ArenaLayout.OVAL.contains(new ArenaLayout.Cell(c.x(),-c.z())));}
        assertEquals(-14,ArenaLayout.OVAL.stream().mapToInt(ArenaLayout.Cell::x).min().orElseThrow());assertEquals(14,ArenaLayout.OVAL.stream().mapToInt(ArenaLayout.Cell::x).max().orElseThrow());assertEquals(-9,ArenaLayout.OVAL.stream().mapToInt(ArenaLayout.Cell::z).min().orElseThrow());assertEquals(9,ArenaLayout.OVAL.stream().mapToInt(ArenaLayout.Cell::z).max().orElseThrow());assertTrue(ArenaLayout.FIGHTING.contains(new ArenaLayout.Cell(0,0)));
    }
    @Test void piecesHaveNoOverlapsAndRoomsAreClear(){
        var occupied=new HashSet<List<Integer>>();for(var p:ArenaLayout.PIECES)assertTrue(occupied.add(List.of(p.x(),p.y(),p.z())),"Duplicate structural cell");
        for(int side=0;side<2;side++)for(int x=-20;x<=20;x++)for(int z=-3;z<=3;z++)if(ArenaLayout.prep(side,x,z))for(int y=1;y<=3;y++)assertFalse(occupied.contains(List.of(x,y,z)),"Blocked prep room");
        assertEquals(232,ArenaLayout.PIECES.stream().filter(p->!p.slab()&&!p.gate()).count());assertEquals(160,ArenaLayout.PIECES.stream().filter(ArenaLayout.Piece::slab).count());assertEquals(6,ArenaLayout.PIECES.stream().filter(ArenaLayout.Piece::gate).count());
        for(int side:new int[]{-1,1})for(int z=-2;z<=2;z++)for(int y=1;y<=3;y++)assertEquals(z!=0,occupied.contains(List.of(side*20,y,z)),"Only the centered one-block rear doorway is open");
    }
}

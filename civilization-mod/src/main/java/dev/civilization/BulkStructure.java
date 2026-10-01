package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** Small local shells; each controller owns only its own stock. */
public final class BulkStructure {
    private static final List<MachineStructure.Part> BUNKER=build(false), TANK=build(true);
    public static List<MachineStructure.Part> parts(boolean liquid){return liquid?TANK:BUNKER;}
    private static List<MachineStructure.Part> build(boolean tank){
        var out=new ArrayList<MachineStructure.Part>();
        for(int y=0;y<3;y++)for(int z=0;z<5;z++)for(int x=-1;x<=1;x++){
            if(x==0&&y==0&&z==0)continue;
            String material="casing";int units=4;Direction side=Direction.DOWN;int corner=0;
            if(!tank){
                if(y==0)units=x==0&&z>0&&z<4?2:4;
                else if(x==0&&z>0&&z<4)material="air";
                else if(y==2){
                    if(x!=0&&(z==0||z==4))units=2;
                    else {units=1;corner=x!=0?(x>0?1:0):(z==4?3:2);}
                }
                else if(x!=0&&z>0&&z<4){units=2;side=x<0?Direction.WEST:Direction.EAST;}
                else if(x==0){units=2;side=z==0?Direction.NORTH:Direction.SOUTH;}
            }else{
                if(y==1){
                    if(x==0&&z>0&&z<4)material="air";
                    else if(x==0&&z==0)material="glass";
                }else if(x==0){units=2;side=y==0?Direction.UP:Direction.DOWN;}
                else {units=1;side=y==0?Direction.UP:Direction.DOWN;corner=x<0?1:0;}
                // Grounded end saddles meet the clipped vessel, without floating lower corners.
                if(y==0&&(z==1||z==3)){units=4;side=Direction.DOWN;}
            }
            out.add(new MachineStructure.Part(x,y,z,material,units,side,corner));
        }
        return List.copyOf(out);
    }
    public static boolean complete(Level level,BlockPos at,Direction front,boolean tank){
        for(var part:parts(tank)){var pos=MachineStructure.position(at,front,part);
            if(!level.hasChunkAt(pos)||!MachineStructure.matches(level,pos,part,front))return false;
        }return true;
    }
}

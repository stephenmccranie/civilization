package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
public final class OilEngineStructure {
 public static final List<MachineStructure.Part> PARTS=parts();
 /** Check only the four possible controller anchors for this component's build position. */
 public static Direction placementFacing(Level level, BlockPos target, net.minecraft.world.level.block.Block component, Direction preferred) {
  for(var part:PARTS) {
   if(!part.material().startsWith("engine_") || !MachineStructure.materialState(part.material()).is(component))continue;
   for(var front:List.of(preferred,preferred.getClockWise(),preferred.getOpposite(),preferred.getCounterClockWise())) {
    var controller=target.subtract(MachineStructure.position(BlockPos.ZERO,front,part));
    if(!level.hasChunkAt(controller))continue;
    var state=level.getBlockState(controller);
    if(state.getBlock() instanceof OilEngineBlock && state.getValue(CivicBlock.FACING)==front)return front;
   }
  }
  return preferred;
 }
 private static List<MachineStructure.Part> parts(){var a=new ArrayList<MachineStructure.Part>();
  for(int z=0;z<3;z++)for(int x=-1;x<=1;x++)if(z!=0||x!=0)a.add(new MachineStructure.Part(x,0,z,"casing",2,Direction.DOWN));
  a.add(new MachineStructure.Part(0,1,1,"engine_cylinder",4,Direction.NORTH));
  a.add(new MachineStructure.Part(-1,1,1,"engine_flywheel",4,Direction.NORTH));
  // The cylinder's modeled cast bed occupies this space; no copper support slab.
  a.add(new MachineStructure.Part(0,1,2,"air",4,Direction.DOWN));
  a.add(new MachineStructure.Part(0,2,2,"air",4,Direction.DOWN));
  for(int x=-1;x<=0;x++)for(int y=1;y<=2;y++)a.add(new MachineStructure.Part(x,y,0,"air",4,Direction.DOWN));
  // Protect the headroom needed by the visible flywheel and cylinder.
  for(int x=-1;x<=0;x++)a.add(new MachineStructure.Part(x,2,1,"air",4,Direction.DOWN));
  return List.copyOf(a);
 }
 public static boolean complete(Level l,BlockPos at,Direction front){for(var part:PARTS){var p=MachineStructure.position(at,front,part);if(!l.hasChunkAt(p)||!MachineStructure.matches(l,p,part,front))return false;
  if(part.material().startsWith("engine_")&&l.getBlockState(p).getValue(CivicBlock.FACING)!=front)return false;
 }return true;}
}

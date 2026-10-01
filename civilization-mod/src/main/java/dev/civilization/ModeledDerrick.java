package dev.civilization;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** All-or-nothing controller construction; geometry-derived collision is shared by both sides. */
public final class ModeledDerrick {
    public static final int BUILD_BUTTON=20;
    public static final String BILL="149 planks, 8 iron blocks, 7 stone/cobblestone, 1 Refinery Port";
    public static final MachineStructure.Part PORT=new MachineStructure.Part(1,0,0,"output_port",4,Direction.EAST);
    public record Cell(int x,int y,int z,VoxelShape[] shapes){public VoxelShape shape(Direction front){return shapes[front.get2DDataValue()];}}
    public static final List<Cell> CELLS=load();
    private ModeledDerrick(){}
    private static List<Cell> load(){
        try(var in=ModeledDerrick.class.getResourceAsStream("/data/civilization/derrick_collision.json")){
            if(in==null)throw new IllegalStateException("Missing derrick collision");
            var result=new ArrayList<Cell>();
            for(var e:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var o=e.getAsJsonObject();var offset=o.getAsJsonArray("offset");var shapes=new VoxelShape[4];
                for(var front:Direction.Plane.HORIZONTAL){
                    var boxes=new ArrayList<VoxelShape>();
                    for(var b:o.getAsJsonArray("boxes")){
                        var a=b.getAsJsonArray();double x=a.get(0).getAsDouble(),y=a.get(1).getAsDouble(),z=a.get(2).getAsDouble(),X=a.get(3).getAsDouble(),Y=a.get(4).getAsDouble(),Z=a.get(5).getAsDouble();
                        boxes.add(switch(front){case EAST->Shapes.box(1-Z,y,x,1-z,Y,X);case SOUTH->Shapes.box(1-X,y,1-Z,1-x,Y,1-z);case WEST->Shapes.box(z,y,1-X,Z,Y,1-x);default->Shapes.box(x,y,z,X,Y,Z);});
                    }
                    shapes[front.get2DDataValue()]=Shapes.or(Shapes.empty(),boxes.toArray(VoxelShape[]::new));
                }
                result.add(new Cell(offset.get(0).getAsInt(),offset.get(1).getAsInt(),offset.get(2).getAsInt(),shapes));
            }return List.copyOf(result);
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static BlockPos position(BlockPos at,Direction front,Cell c){return at.relative(front.getClockWise(),c.x()).relative(front.getOpposite(),c.z()).above(c.y());}
    public static BlockState state(int index,Direction front){return IndustrialContent.DERRICK_PART.get().defaultBlockState().setValue(CivicBlock.FACING,front).setValue(DerrickPartBlock.CELL,index);}
    public static int status(Level l,BlockPos at,Direction front){
        if(!(l.getBlockEntity(at) instanceof IndustrialBlockEntity m)||!m.derrickBuilt)return MachineStructure.INCOMPLETE;
        for(int i=0;i<CELLS.size();i++){var p=position(at,front,CELLS.get(i));if(!l.hasChunkAt(p))return MachineStructure.UNLOADED;if(!l.getBlockState(p).equals(state(i,front)))return MachineStructure.INCOMPLETE;}
        return MachineStructure.COMPLETE;
    }
    private static boolean accepts(ItemStack s,int kind){return switch(kind){case 0->s.is(ItemTags.PLANKS);case 1->s.is(Items.IRON_BLOCK);case 2->s.is(Items.STONE)||s.is(Items.COBBLESTONE);default->s.is(IndustrialContent.PORT.get().asItem());};}
    private static boolean fail(Player p,String reason){p.displayClientMessage(Component.literal(reason),true);return false;}
    public static boolean build(IndustrialBlockEntity m,Player player){
        if(!(m.getLevel() instanceof net.minecraft.server.level.ServerLevel l)||m.kind!=IndustrialBlock.Kind.PUMP||m.derrickBuilt||!m.stillValid(player))return false;
        var inventory=player.getInventory();int[] take=new int[inventory.items.size()];var paid=new ArrayList<ItemStack>();
        if(!player.getAbilities().instabuild){
            int[] costs={149,8,7,1};
            for(int kind=0;kind<costs.length;kind++){
                int left=costs[kind];for(int slot=0;slot<take.length&&left>0;slot++){var stack=inventory.getItem(slot);if(accepts(stack,kind)){int n=Math.min(left,stack.getCount()-take[slot]);take[slot]+=n;left-=n;}}
                if(left>0)return fail(player,"Requires "+BILL);
            }
        }
        var positions=new ArrayList<BlockPos>();var states=new ArrayList<BlockState>();
        for(int i=0;i<CELLS.size();i++){positions.add(position(m.getBlockPos(),m.front(),CELLS.get(i)));states.add(state(i,m.front()));}
        positions.add(MachineStructure.position(m.getBlockPos(),m.front(),PORT));states.add(MachineStructure.shape(PORT,m.front()));
        for(int i=0;i<positions.size();i++){
            var p=positions.get(i);if(!l.hasChunkAt(p)||l.isOutsideBuildHeight(p)||!l.getWorldBorder().isWithinBounds(p))return fail(player,"Derrick space must be loaded and within the world.");
            if(!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty())return fail(player,"Clear derrick space: "+p.toShortString());
            if(!CivicAccess.allowed(l,p,player)||!CivicAccess.boundary(l,m.getBlockPos(),p)||!l.mayInteract(player,p)||!player.mayUseItemAt(p,Direction.UP,IndustrialContent.PUMP.toStack())||AirshipSystem.at(l,p)!=null)return fail(player,"Derrick construction is protected here.");
            if(!l.isUnobstructed(null,states.get(i).getCollisionShape(l,p).move(p.getX(),p.getY(),p.getZ())))return fail(player,"An entity is in the derrick's construction space.");
        }
        // No owner exists until every cell is placed, so rollback cannot dismantle recursively.
        int placed=0;boolean failed=false;
        for(;placed<positions.size();){
            var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(l.dimension(),l,positions.get(placed));
            if(!l.setBlock(positions.get(placed),states.get(placed),3))break;
            placed++;
            if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,Direction.UP)||!m.stillValid(player)){failed=true;break;}
        }
        if(!m.stillValid(player)||positions.stream().anyMatch(p->!l.hasChunkAt(p)))placed=Math.min(placed,positions.size()-1);
        for(int i=0;i<placed;i++)if(!l.getBlockState(positions.get(i)).equals(states.get(i)))failed=true;
        if(failed||placed!=positions.size()){
            for(int i=0;i<placed;i++)if(l.getBlockState(positions.get(i)).equals(states.get(i)))l.removeBlock(positions.get(i),false);
            return fail(player,"Derrick construction could not finish.");
        }
        for(int slot=0;slot<take.length;slot++)if(take[slot]>0)paid.add(inventory.removeItem(slot,take[slot]));
        inventory.setChanged();m.derrickMaterials.addAll(paid);m.derrickBuilt=true;m.formed=true;m.setChanged();IndustrialStructure.bind(m);
        // Internal collision cells are not hundreds of separate player placement tasks.
        if(CalorieFoodData.active(player))CalorieFoodData.of(player).spendLabor(player,CalorieConfig.PLACE.get()*164,false,"civilization:derrick_assemble");
        l.playSound(null,m.getBlockPos(),net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,.8f);
        l.sendBlockUpdated(m.getBlockPos(),m.getBlockState(),m.getBlockState(),3);
        return true;
    }
    public static void dismantle(IndustrialBlockEntity m){
        if(!m.derrickBuilt||m.getLevel()==null)return;
        m.derrickBuilt=false;m.formed=false;var l=m.getLevel();
        // Unloaded remnants remove themselves when their chunk next loads (part tick).
        for(int i=0;i<CELLS.size();i++){var p=position(m.getBlockPos(),m.front(),CELLS.get(i));if(l.hasChunkAt(p)&&l.getBlockState(p).equals(state(i,m.front())))l.removeBlock(p,false);}
        var port=MachineStructure.position(m.getBlockPos(),m.front(),PORT);
        if(l.hasChunkAt(port)&&l.getBlockEntity(port) instanceof RefineryPortEntity e&&e.boundTo(m.getBlockPos()))l.removeBlock(port,false);
        for(var stack:m.derrickMaterials)Containers.dropItemStack(l,m.getBlockPos().getX()+.5,m.getBlockPos().getY()+.5,m.getBlockPos().getZ()+.5,stack);
        m.derrickMaterials.clear();
        m.setChanged();var current=l.getBlockState(m.getBlockPos());
        if(current.is(IndustrialContent.PUMP.get()))l.sendBlockUpdated(m.getBlockPos(),current,current,3);
    }
}

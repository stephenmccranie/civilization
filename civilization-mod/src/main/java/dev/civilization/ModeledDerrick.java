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

/** Progressive, locally repairable controller construction; geometry-derived collision is shared by both sides. */
public final class ModeledDerrick {
    public static final int BUILD_BUTTON=20;
    public static final String BILL="149 planks, 8 iron blocks, 7 stone/cobblestone, 1 Refinery Port";
    public static final MachineStructure.Part PORT=new MachineStructure.Part(1,0,0,"output_port",4,Direction.EAST);
    public record Cell(int x,int y,int z,VoxelShape[] shapes,Map<Integer,VoxelShape[]> pieces){
        public VoxelShape shape(Direction front){return shapes[front.get2DDataValue()];}
        public VoxelShape shape(Direction front,int mask){var result=Shapes.empty();for(var e:pieces.entrySet())if((mask&(1<<e.getKey()))!=0)result=Shapes.or(result,e.getValue()[front.get2DDataValue()]);return result;}
    }
    public static final List<Cell> CELLS=load();
    private ModeledDerrick(){}
    private static List<Cell> load(){
        try(var in=ModeledDerrick.class.getResourceAsStream("/data/civilization/derrick_sections.json")){
            if(in==null)throw new IllegalStateException("Missing derrick collision");
            var result=new ArrayList<Cell>();
            for(var e:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var o=e.getAsJsonObject();var offset=o.getAsJsonArray("offset");var shapes=new VoxelShape[4];var pieces=new TreeMap<Integer,VoxelShape[]>();
                for(var front:Direction.Plane.HORIZONTAL){
                    var boxes=new ArrayList<VoxelShape>();
                    for(var b:o.getAsJsonArray("boxes")){
                        var a=b.getAsJsonArray();int part=a.get(0).getAsInt();double x=a.get(1).getAsDouble(),y=a.get(2).getAsDouble(),z=a.get(3).getAsDouble(),X=a.get(4).getAsDouble(),Y=a.get(5).getAsDouble(),Z=a.get(6).getAsDouble();
                        var box=switch(front){case EAST->Shapes.box(1-Z,y,x,1-z,Y,X);case SOUTH->Shapes.box(1-X,y,1-Z,1-x,Y,1-z);case WEST->Shapes.box(z,y,1-X,Z,Y,1-x);default->Shapes.box(x,y,z,X,Y,Z);};boxes.add(box);var parts=pieces.computeIfAbsent(part,k->new VoxelShape[]{Shapes.empty(),Shapes.empty(),Shapes.empty(),Shapes.empty()});parts[front.get2DDataValue()]=Shapes.or(parts[front.get2DDataValue()],box);
                    }
                    shapes[front.get2DDataValue()]=Shapes.or(Shapes.empty(),boxes.toArray(VoxelShape[]::new));
                }
                result.add(new Cell(offset.get(0).getAsInt(),offset.get(1).getAsInt(),offset.get(2).getAsInt(),shapes,pieces));
            }return List.copyOf(result);
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static BlockPos position(BlockPos at,Direction front,Cell c){return at.relative(front.getClockWise(),c.x()).relative(front.getOpposite(),c.z()).above(c.y());}
    public static BlockState state(int index,Direction front){return IndustrialContent.DERRICK_PART.get().defaultBlockState().setValue(CivicBlock.FACING,front).setValue(DerrickPartBlock.CELL,index);}
    public static int status(Level l,BlockPos at,Direction front){
        if(!(l.getBlockEntity(at) instanceof IndustrialBlockEntity m)||m.derrickSections!=ALL)return MachineStructure.INCOMPLETE;
        for(int i=0;i<CELLS.size();i++){var p=position(at,front,CELLS.get(i));if(!l.hasChunkAt(p))return MachineStructure.UNLOADED;if(!l.getBlockState(p).equals(state(i,front)))return MachineStructure.INCOMPLETE;}
        return MachineStructure.COMPLETE;
    }
    public static final int PARTS=25, ALL=(1<<PARTS)-1;
    public static int cost(int part){return part==0?7:part<=20?6:part==21?20:part==22?9:part==23?8:1;}
    public static int material(int part){return part==0?2:part<=22?0:part==23?1:3;}
    public static String label(int part){return part==0?"Footings":part<=20?"Frame panel "+part:part==21?"Middle gallery":part==22?"Crown gallery":part==23?"Pump machinery":"Output port";}
    public static boolean accepts(ItemStack s,int kind){return switch(kind){case 0->s.is(ItemTags.PLANKS);case 1->s.is(Items.IRON_BLOCK);case 2->s.is(Items.STONE)||s.is(Items.COBBLESTONE);default->s.is(IndustrialContent.PORT.get().asItem());};}
    public static boolean has(IndustrialBlockEntity m,int part){return (m.derrickSections&(1<<part))!=0;}
    private static boolean fail(Player p,String reason){p.displayClientMessage(Component.literal(reason),true);return false;}
    /** Development fixtures only; gameplay always supplies a held stack. */
    public static boolean build(IndustrialBlockEntity m,Player player){
        if(!player.getAbilities().instabuild)return false;
        for(var stack:List.of(new ItemStack(Items.COBBLESTONE,7),new ItemStack(Items.OAK_PLANKS,64),new ItemStack(Items.IRON_BLOCK,8),IndustrialContent.PORT.toStack()))build(m,player,stack);
        return m.derrickSections==ALL;
    }
    public static boolean build(IndustrialBlockEntity m,Player player,ItemStack held){
        if(held.isEmpty())return false;boolean any=false;
        for(int i=0;i<PARTS&&!held.isEmpty();i++){if(!buildOne(m,player,held))break;any=true;}
        return any;
    }
    private static boolean buildOne(IndustrialBlockEntity m,Player player,ItemStack held){
        if(!(m.getLevel() instanceof net.minecraft.server.level.ServerLevel l)||m.kind!=IndustrialBlock.Kind.PUMP||!m.stillValid(player))return false;
        migrate(m);
        for(int part=0;part<PARTS;part++){
            if(has(m,part)||!held.isEmpty()&&!accepts(held,material(part)))continue;
            if(!player.getAbilities().instabuild&&held.getCount()<cost(part))return false;
            var positions=new ArrayList<BlockPos>();var states=new ArrayList<BlockState>();
            var before=new ArrayList<BlockState>();
            for(int i=0;i<CELLS.size();i++)if(CELLS.get(i).pieces().containsKey(part)){positions.add(position(m.getBlockPos(),m.front(),CELLS.get(i)));states.add(state(i,m.front()));}
            if(part==24){positions.add(MachineStructure.position(m.getBlockPos(),m.front(),PORT));states.add(MachineStructure.shape(PORT,m.front()));}
            for(int i=0;i<positions.size();i++){
                var at=positions.get(i);var old=l.getBlockState(at);before.add(old);
                if(!l.hasChunkAt(at)||l.isOutsideBuildHeight(at)||!l.getWorldBorder().isWithinBounds(at))return fail(player,"Load the section's construction space first.");
                if(!old.isAir()&&!old.equals(states.get(i))||!l.getFluidState(at).isEmpty())return fail(player,"Clear construction space: "+at.toShortString());
                if(!CivicAccess.allowed(l,at,player)||!CivicAccess.boundary(l,m.getBlockPos(),at)||!l.mayInteract(player,at)||!player.mayUseItemAt(at,Direction.UP,IndustrialContent.PUMP.toStack())||AirshipSystem.at(l,at)!=null)return fail(player,"Construction is protected here.");
                var shape=part==24?states.get(i).getCollisionShape(l,at):CELLS.get(states.get(i).getValue(DerrickPartBlock.CELL)).pieces().get(part)[m.front().get2DDataValue()];
                if(!l.isUnobstructed(null,shape.move(at.getX(),at.getY(),at.getZ())))return fail(player,"An entity blocks this section.");
            }
            // Set ownership before placement, then roll back without refunds on cancellation.
            int prior=m.derrickSections;m.derrickSections|=1<<part;m.derrickBuilt=true;
            var snapshots=new ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();boolean failed=false;
            for(int i=0;i<positions.size();i++){
                var snap=net.neoforged.neoforge.common.util.BlockSnapshot.create(l.dimension(),l,positions.get(i));snapshots.add(snap);
                if(!l.setBlock(positions.get(i),states.get(i),3)&&!l.getBlockState(positions.get(i)).equals(states.get(i))||net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snap,Direction.UP)||!m.stillValid(player)){failed=true;break;}
            }
            for(int i=0;i<positions.size();i++)if(!l.getBlockState(positions.get(i)).equals(states.get(i)))failed=true;
            if(failed){m.derrickSections=prior;m.derrickBuilt=prior!=0;for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore();sync(m);return fail(player,"Section construction was cancelled.");}
            var paid=new ArrayList<ItemStack>();if(!player.getAbilities().instabuild)paid.add(held.split(cost(part)));
            m.derrickPaid.put(part,paid);player.getInventory().setChanged();if(part==24&&l.getBlockEntity(positions.getFirst()) instanceof RefineryPortEntity e)e.bind(m,"output_port",MachineStructure.side(PORT,m.front()));
            if(CalorieFoodData.active(player))CalorieFoodData.of(player).spendLabor(player,CalorieConfig.PLACE.get()*(part==24?0:cost(part)),false,"civilization:derrick_section");
            sync(m);l.playSound(null,m.getBlockPos(),net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,.8f);return true;
        }
        return false;
    }
    public static void reconcile(IndustrialBlockEntity m){
        int lost=0;var l=m.getLevel();
        for(int i=0;i<CELLS.size();i++){var cell=CELLS.get(i);var p=position(m.getBlockPos(),m.front(),cell);if(!l.hasChunkAt(p))continue;
            if(!l.getBlockState(p).equals(state(i,m.front())))for(int part:cell.pieces().keySet())if(has(m,part))lost|=1<<part;
        }
        var port=MachineStructure.position(m.getBlockPos(),m.front(),PORT);if(has(m,24)&&l.hasChunkAt(port)&&!MachineStructure.matches(l,port,PORT,m.front()))lost|=1<<24;
        for(int part=0;part<PARTS;part++)if((lost&(1<<part))!=0)removeSection(m,part,true);
    }
    public static void sync(IndustrialBlockEntity m){m.derrickBuilt=m.derrickSections!=0;m.formed=false;m.setChanged();if(m.getLevel()!=null)m.getLevel().sendBlockUpdated(m.getBlockPos(),m.getBlockState(),m.getBlockState(),3);}
    public static void migrate(IndustrialBlockEntity m){
        if(m.derrickSections!=0||!m.derrickBuilt)return;
        m.derrickSections=ALL;
        for(int part=0;part<PARTS;part++){var paid=new ArrayList<ItemStack>();int left=cost(part);for(var stack:m.derrickMaterials)if(left>0&&accepts(stack,material(part))){int n=Math.min(left,stack.getCount());paid.add(stack.split(n));left-=n;}m.derrickPaid.put(part,paid);}
        m.derrickMaterials.removeIf(ItemStack::isEmpty);m.setChanged();
    }
    public static void removeSection(IndustrialBlockEntity m,int part,boolean drops){
        migrate(m);if(!has(m,part)||m.getLevel()==null)return;
        m.derrickSections&=~(1<<part);m.derrickChanging=true;var l=m.getLevel();
        try{
            for(int i=0;i<CELLS.size();i++)if(CELLS.get(i).pieces().containsKey(part)){var p=position(m.getBlockPos(),m.front(),CELLS.get(i));if(!l.hasChunkAt(p))continue;var old=l.getBlockState(p);if(old.isAir()||old.equals(state(i,m.front()))){var next=CELLS.get(i).shape(m.front(),m.derrickSections);if(next.isEmpty())l.removeBlock(p,false);else l.setBlock(p,state(i,m.front()),3);}}
            if(part==24){var at=MachineStructure.position(m.getBlockPos(),m.front(),PORT);if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof RefineryPortEntity e&&e.boundTo(m.getBlockPos()))l.removeBlock(at,false);}
        }finally{m.derrickChanging=false;}
        var paid=m.derrickPaid.remove(part);if(drops&&paid!=null)for(var stack:paid)Containers.dropItemStack(l,m.getBlockPos().getX()+.5,m.getBlockPos().getY()+.5,m.getBlockPos().getZ()+.5,stack);
        sync(m);
    }
    public static void dismantle(IndustrialBlockEntity m){migrate(m);for(int part=0;part<PARTS;part++)removeSection(m,part,true);for(var stack:m.derrickMaterials)Containers.dropItemStack(m.getLevel(),m.getBlockPos().getX()+.5,m.getBlockPos().getY()+.5,m.getBlockPos().getZ()+.5,stack);m.derrickMaterials.clear();}
}

package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds, checks, repairs and dismantles any machine drawn as a native model. A held stack pays toward sections, any
 * amount at a time; a section paid in full places its invisible collision cells and any ports. Everything paid toward a
 * section is recorded exactly and refunded once when the section is broken or the controller is removed.
 */
public final class ModeledMachine {
    private ModeledMachine() {}

    /** The modeled controller behind a block entity, or null. */
    public static ModeledController of(Object entity){return entity instanceof ModeledController c&&c.model()!=null?c:null;}
    public static BlockPos position(BlockPos at,Direction front,MachineModel.Cell c){return at.relative(front.getClockWise(),c.x()).relative(front.getOpposite(),c.z()).above(c.y());}
    public static BlockState cellState(MachineModel model,int index,Direction front){return MachineParts.block(model).defaultBlockState().setValue(CivicBlock.FACING,front).setValue(model.cell,index);}
    private static BlockPos at(ModeledController c){return c.entity().getBlockPos();}
    /** A controller's model, facing and built sections as the client last received them. */
    public record Drawn(MachineModel model,Direction front,int sections){}
    /** The client's loaded controllers by position. Chunk meshing reads a cell's controller here, not from the level:
     *  Sodium's mesh threads see block entities only within two blocks of the section they mesh, and a tall machine's
     *  cells reach far beyond its controller's. */
    public static final Map<BlockPos,Drawn> DRAWN=new java.util.concurrent.ConcurrentHashMap<>();
    /** On the client, a change in built sections redraws the machine's cells, whose block states it leaves unchanged. */
    public static void redraw(ModeledController c){
        var l=c.entity().getLevel();DRAWN.put(at(c),new Drawn(c.model(),c.front(),c.build().sections));
        for(var cell:c.model().cells){var p=position(at(c),c.front(),cell);var s=l.getBlockState(p);l.sendBlockUpdated(p,s,s,net.minecraft.world.level.block.Block.UPDATE_CLIENTS);}
    }

    public static int status(ModeledController c){
        var l=c.entity().getLevel();var model=c.model();
        if(l==null||c.build().sections!=model.all)return MachineStructure.INCOMPLETE;
        for(int i=0;i<model.cells.size();i++){
            var p=position(at(c),c.front(),model.cells.get(i));
            if(!Chunks.loaded(l,p))return MachineStructure.UNLOADED;
            if(!l.getBlockState(p).equals(cellState(model,i,c.front())))return MachineStructure.INCOMPLETE;
        }
        for(var port:model.ports){
            var p=MachineStructure.position(at(c),c.front(),port.part());
            if(!Chunks.loaded(l,p))return MachineStructure.UNLOADED;
            if(!MachineStructure.matches(l,p,port.part(),c.front()))return MachineStructure.INCOMPLETE;
        }
        return MachineStructure.COMPLETE;
    }
    public static boolean accepts(ModeledController c,ItemStack held){
        for(int part=0;part<c.model().sections.size();part++)if(!c.build().has(part)&&c.model().section(part).accepts(held))return true;
        return false;
    }
    /** The stack a right-click builds this machine from; see {@link MachineConstruction#material}. */
    public static ItemStack material(ModeledController c,Player player,ItemStack held){return MachineConstruction.material(player,held,s->accepts(c,s));}
    private static boolean fail(Player p,String reason){p.displayClientMessage(Component.literal(reason),true);return false;}
    /** Development fixtures only: a creative player builds every section from the section's own sample stack. */
    public static boolean assemble(ModeledController c,Player creative){
        if(!creative.getAbilities().instabuild)return false;
        for(int part=0;part<c.model().sections.size();part++)if(!c.build().has(part))build(c,creative,c.model().section(part).sample());
        return c.build().sections==c.model().all;
    }
    /** Pays the held stack toward unbuilt sections in order, building each once it is paid in full; leftovers stay in hand. */
    public static boolean build(ModeledController c,Player player,ItemStack held){
        boolean any=false;
        while(!held.isEmpty()&&buildOne(c,player,held))any=true;
        return any;
    }
    /** Pays what the first unbuilt section taking the held stack still needs, or as much of it as the stack holds, and
     * builds the section once that completes its cost; a creative player builds it unpaid. */
    private static boolean buildOne(ModeledController c,Player player,ItemStack held){
        var model=c.model();var front=c.front();var build=c.build();
        if(!(c.entity().getLevel() instanceof ServerLevel l)||!c.stillValid(player))return false;
        for(int part=0;part<model.sections.size();part++){
            var section=model.section(part);
            if(build.has(part)||!section.accepts(held))continue;
            boolean creative=player.getAbilities().instabuild;
            int take=creative?0:Math.min(held.getCount(),section.count()-build.paidCount(part));
            boolean completes=creative||build.paidCount(part)+take==section.count();
            var positions=new ArrayList<BlockPos>();var states=new ArrayList<BlockState>();var shapes=new ArrayList<net.minecraft.world.phys.shapes.VoxelShape>();
            for(int i=0;i<model.cells.size();i++){
                var cell=model.cells.get(i);if(!cell.pieces().containsKey(part))continue;
                positions.add(position(at(c),front,cell));states.add(cellState(model,i,front));shapes.add(cell.pieces().get(part)[front.get2DDataValue()]);
            }
            for(var port:model.ports)if(port.section()==part){
                var p=MachineStructure.position(at(c),front,port.part());var state=MachineStructure.shape(port.part(),front);
                positions.add(p);states.add(state);shapes.add(state.getCollisionShape(l,p));
            }
            for(int i=0;i<positions.size();i++){
                var p=positions.get(i);var old=l.getBlockState(p);
                if(!Chunks.loaded(l,p)||l.isOutsideBuildHeight(p)||!l.getWorldBorder().isWithinBounds(p))return fail(player,"Load the section's construction space first.");
                // A cell other sections already fill may take another piece; anything else must be cleared.
                if(!old.isAir()&&!old.equals(states.get(i))||!l.getFluidState(p).isEmpty())return fail(player,"Clear construction space: "+p.toShortString());
                if(!CivicAccess.allowed(l,p,player)||!CivicAccess.boundary(l,at(c),p)||!l.mayInteract(player,p)||!player.mayUseItemAt(p,Direction.UP,new ItemStack(c.entity().getBlockState().getBlock()))||AirshipSystem.at(l,p)!=null)
                    return fail(player,"Construction is protected here.");
                if(completes&&!l.isUnobstructed(null,shapes.get(i).move(p.getX(),p.getY(),p.getZ())))return fail(player,"An entity blocks this section.");
            }
            var sound=held.getItem() instanceof BlockItem block?block.getBlock().defaultBlockState().getSoundType().getPlaceSound():net.minecraft.sounds.SoundEvents.STONE_PLACE;
            if(!completes){
                build.pay(part,held.split(take));player.getInventory().setChanged();synced(c);
                player.displayClientMessage(Component.literal(section.name()+": "+build.paidCount(part)+" of "+section.count()+" "+section.material()),true);
                l.playSound(null,at(c),sound,net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.2f);return true;
            }
            // Own the section before placing, so the cells' shapes include it; roll back without payment if cancelled.
            int prior=build.sections;build.sections|=1<<part;
            var snapshots=new ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();boolean failed=false;
            for(int i=0;i<positions.size();i++){
                var snap=net.neoforged.neoforge.common.util.BlockSnapshot.create(l.dimension(),l,positions.get(i));snapshots.add(snap);
                if(!l.setBlock(positions.get(i),states.get(i),3)&&!l.getBlockState(positions.get(i)).equals(states.get(i))||net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snap,Direction.UP)||!c.stillValid(player)){failed=true;break;}
            }
            for(int i=0;i<positions.size();i++)if(!l.getBlockState(positions.get(i)).equals(states.get(i)))failed=true;
            if(failed){build.sections=prior;for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore();changed(c);return fail(player,"Section construction was cancelled.");}
            if(take>0){build.pay(part,held.split(take));player.getInventory().setChanged();}
            if(CalorieFoodData.active(player))CalorieFoodData.of(player).spendLabor(player,CalorieConfig.PLACE.get()*section.labor(),false,"civilization:machine_section");
            changed(c);l.playSound(null,at(c),sound,net.minecraft.sounds.SoundSource.BLOCKS,1,.8f);return true;
        }
        return false;
    }
    /** Sections whose cells or ports were broken or replaced are removed and refunded. */
    public static void reconcile(ModeledController c){
        var l=c.entity().getLevel();var model=c.model();int lost=0;
        for(int i=0;i<model.cells.size();i++){
            var cell=model.cells.get(i);var p=position(at(c),c.front(),cell);if(!Chunks.loaded(l,p))continue;
            if(!l.getBlockState(p).equals(cellState(model,i,c.front())))for(int part:cell.pieces().keySet())if(c.build().has(part))lost|=1<<part;
        }
        for(var port:model.ports){var p=MachineStructure.position(at(c),c.front(),port.part());if(c.build().has(port.section())&&Chunks.loaded(l,p)&&!MachineStructure.matches(l,p,port.part(),c.front()))lost|=1<<port.section();}
        for(int part=0;part<model.sections.size();part++)if((lost&(1<<part))!=0)removeSection(c,part,true);
    }
    /** Removes a section if it is built and refunds everything paid toward it, built or not, once. */
    public static void removeSection(ModeledController c,int part,boolean drops){
        var build=c.build();var model=c.model();var l=c.entity().getLevel();
        if(l==null||!build.has(part)&&!build.paid.containsKey(part))return;
        if(build.has(part)){
            build.sections&=~(1<<part);build.changing=true;
            try{
                for(int i=0;i<model.cells.size();i++){
                    var cell=model.cells.get(i);if(!cell.pieces().containsKey(part))continue;
                    var p=position(at(c),c.front(),cell);if(!Chunks.loaded(l,p))continue;var old=l.getBlockState(p);
                    if(old.isAir()||old.equals(cellState(model,i,c.front()))){if(cell.shape(c.front(),build.sections).isEmpty())l.removeBlock(p,false);else l.setBlock(p,cellState(model,i,c.front()),3);}
                }
                for(var port:model.ports)if(port.section()==part){var p=MachineStructure.position(at(c),c.front(),port.part());if(Chunks.loaded(l,p)&&MachineStructure.matches(l,p,port.part(),c.front()))l.removeBlock(p,false);}
            }finally{build.changing=false;}
        }
        var paid=build.paid.remove(part);var o=at(c);
        if(drops&&paid!=null)for(var stack:paid)Containers.dropItemStack(l,o.getX()+.5,o.getY()+.5,o.getZ()+.5,stack);
        changed(c);
    }
    public static void dismantle(ModeledController c){for(int part=0;part<c.model().sections.size();part++)removeSection(c,part,true);}
    private static void changed(ModeledController c){c.sectionsChanged();synced(c);}
    /** Saves the controller and sends its build to clients, whose guide shows what is still due. */
    private static void synced(ModeledController c){
        var e=c.entity();e.setChanged();
        if(e.getLevel()!=null)e.getLevel().sendBlockUpdated(e.getBlockPos(),e.getBlockState(),e.getBlockState(),3);
    }
    /** The section a cell belongs to that this build has, so breaking any cell removes one whole section. */
    static OptionalInt builtSection(ModeledController c,MachineModel.Cell cell){
        for(int part:cell.pieces().keySet())if(c.build().has(part))return OptionalInt.of(part);
        return OptionalInt.empty();
    }
}

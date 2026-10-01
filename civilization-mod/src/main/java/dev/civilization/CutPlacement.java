package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Shared by authoritative placement and the client ghost; vanilla whole-cell replacement is bypassed. */
public final class CutPlacement {
    public record Plan(BlockPos pos,BlockState[] cells,BlockState added,boolean valid) {}
    private record Pending(net.minecraft.server.level.ServerPlayer player,BlockPos pos,double volume) {}
    // Item use and NeoForge's placement event run synchronously. Keep the size of the
    // placed item, since joining may turn the resulting block into a slab or full block.
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();
    static double takePlacedVolume(net.minecraft.server.level.ServerPlayer player,BlockPos pos) {
        var pending=PENDING.get();
        if(pending==null||pending.player()!=player||!pending.pos().equals(pos))return 0;
        PENDING.remove();return pending.volume();
    }
    static void clearPending(){PENDING.remove();}
    public static Plan plan(UseOnContext context) {
        var level=context.getLevel();var face=context.getClickedFace();var hit=context.getClickLocation();
        // Move across the actual hit surface, including a surface halfway through a block.
        var target=hit.add(face.getStepX()*.001,face.getStepY()*.001,face.getStepZ()*.001);
        var pos=BlockPos.containing(target);var existing=level.getBlockState(pos);
        boolean partial=existing.is(CuttingContent.PIECE.get()) || existing.getBlock() instanceof SlabBlock && existing.getValue(SlabBlock.TYPE)!=net.minecraft.world.level.block.state.properties.SlabType.DOUBLE;
        var cells=partial?CutCells.read(level,pos):new BlockState[8];
        double x=target.x-pos.getX(),y=target.y-pos.getY(),z=target.z-pos.getZ();
        int anchor=(x>=.5?1:0)|(y>=.5?2:0)|(z>=.5?4:0);
        int units=CuttingContent.units(context.getItemInHand());
        var side=units==2?CutBlock.orientation(face,x,y,z,context.isSecondaryUseActive()):face.getOpposite();
        var preferred=CutGeometry.bounds(units,side,CutGeometry.corner(units,side,x,y,z));
        // Preserve the aimed orientation first. If it cannot fit, try the other axes at this exact cell.
        var candidates=new java.util.ArrayList<AABB>();candidates.add(atAnchor(preferred,anchor));
        if(!context.isSecondaryUseActive())for(Direction d:Direction.values())for(int c=0;c<4;c++) {
            var box=atAnchor(CutGeometry.bounds(units,d,c),anchor);if(!candidates.contains(box))candidates.add(box);
        }
        AABB chosen=candidates.getFirst();boolean fits=false;
        for(var box:candidates)if((CutCells.mask(box)&CutCells.mask(cells))==0){chosen=box;fits=true;break;}
        var added=CutGeometry.state(chosen);boolean valid=fits && (partial || existing.canBeReplaced()) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
        if(valid)valid=level.isUnobstructed(null,Shapes.create(chosen).move(pos.getX(),pos.getY(),pos.getZ()));
        var material=CuttingContent.material(context.getItemInHand());int mask=CutCells.mask(chosen);
        if(fits)for(int i=0;i<8;i++)if((mask&(1<<i))!=0)cells[i]=material;
        return new Plan(pos,cells,added,valid);
    }
    private static AABB atAnchor(AABB shape,int anchor) {
        var cell=CutCells.box(anchor);
        return new AABB(shape.getXsize()==1?0:cell.minX,shape.getYsize()==1?0:cell.minY,shape.getZsize()==1?0:cell.minZ,
                shape.getXsize()==1?1:cell.maxX,shape.getYsize()==1?1:cell.maxY,shape.getZsize()==1?1:cell.maxZ);
    }
    /** Native slab item use is intercepted before NeoForge's usual placement wrapper. */
    static InteractionResult placeNativeSlab(UseOnContext context) {
        var level=context.getLevel();
        if(level.isClientSide)return place(context);
        var plan=plan(context);
        if(!plan.valid())return InteractionResult.FAIL;
        var snapshot=BlockSnapshot.create(level.dimension(),level,plan.pos());
        var original=context.getItemInHand().copy();
        var result=place(context);
        if(result.consumesAction() && EventHooks.onBlockPlace(context.getPlayer(),snapshot,context.getClickedFace())) {
            snapshot.restore();
            if(context.getPlayer()!=null)context.getPlayer().setItemInHand(context.getHand(),original);
            PENDING.remove();
            return InteractionResult.FAIL;
        }
        return result;
    }
    public static InteractionResult place(UseOnContext context) {
        if(!context.getLevel().isClientSide)PENDING.remove();
        var plan=plan(context);var player=context.getPlayer();var level=context.getLevel();var stack=context.getItemInHand();
        if(!plan.valid() || player!=null && (!level.mayInteract(player,plan.pos()) || !player.mayUseItemAt(plan.pos(),context.getClickedFace(),stack)))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(level instanceof net.minecraft.server.level.ServerLevel server){var boat=BoatSystem.at(server,plan.pos());if(boat!=null&&(!BoatSystem.owner(boat,player)||(level.getBlockState(plan.pos()).isAir()&&!BoatSystem.attached(boat,plan.pos()))))return InteractionResult.FAIL;}

        var state=CutCells.canonical(plan.cells());if(state==null)state=CuttingContent.PIECE.get().defaultBlockState();
        if(state.hasProperty(CutBlock.WATERLOGGED))state=state.setValue(CutBlock.WATERLOGGED,CutCells.mask(plan.cells())!=255 && level.getFluidState(plan.pos()).is(net.minecraft.tags.FluidTags.WATER));
        // NeoForge snapshot rollback only restores entity NBT if the block state also changes.
        if(state.is(CuttingContent.PIECE.get()) && state.equals(level.getBlockState(plan.pos())))state=state.cycle(CutBlock.REVISION);
        if(!level.setBlock(plan.pos(),state,3))return InteractionResult.FAIL;
        if(level.getBlockEntity(plan.pos()) instanceof CutBlockEntity cut)cut.cells(plan.cells());
        var sound=CuttingContent.material(stack).getSoundType();
        level.playSound(null,plan.pos(),sound.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,(sound.getVolume()+1)/2,sound.getPitch()*.8f);
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,plan.pos(),net.minecraft.world.level.gameevent.GameEvent.Context.of(player,state));
        if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraft.advancements.CriteriaTriggers.PLACED_BLOCK.trigger(server,plan.pos(),stack);
        double volume=CuttingContent.volume(CuttingContent.units(stack));
        stack.consume(1,player);
        if(player instanceof net.minecraft.server.level.ServerPlayer server)PENDING.set(new Pending(server,plan.pos(),volume));
        return InteractionResult.CONSUME;
    }
}

package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** The physical cells of one machine model: collision from its built sections, and its still geometry drawn with the terrain by the client; clicks and damage go to the controller. */
public final class MachinePartBlock extends Block implements WorldlyContainerHolder {
    /** The model whose cells this block is, set while the block is constructed so its state definition can size the cell property. */
    static final ThreadLocal<MachineModel> PENDING=new ThreadLocal<>();
    public final MachineModel model;
    private Block controller;
    public MachinePartBlock(Properties p){
        super(p);model=PENDING.get();
        registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(model.cell,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,PENDING.get().cell);}
    private MachineModel.Cell cell(BlockState s){return model.cells.get(s.getValue(model.cell));}
    /** The machine's controller block: every part shows its name, so Jade and pick-block name the machine, not the part. */
    private Block controller(){
        if(controller==null)controller=net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream().filter(b->b instanceof ModeledBlock m&&m.model()==model).findFirst().orElseThrow();
        return controller;
    }
    @Override public String getDescriptionId(){return controller().getDescriptionId();}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(controller());}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        Level world=l instanceof Level level?level:l instanceof net.minecraft.world.level.chunk.LevelChunk chunk?chunk.getLevel():null;
        if(world==null)return cell(s).shape(s.getValue(CivicBlock.FACING));
        var owner=owner(world,p,s);return owner==null?Shapes.empty():cell(s).shape(s.getValue(CivicBlock.FACING),owner.build().sections);
    }
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!l.isClientSide)l.scheduleTick(p,this,100);}
    /** A cell whose controller is gone, or that no built section fills any more, removes itself once its controller's chunk is loaded. */
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,net.minecraft.util.RandomSource random){
        var at=controller(p,s);
        if(l.hasChunkAt(at)&&(owner(l,p,s)==null||getShape(s,l,p,CollisionContext.empty()).isEmpty()))l.removeBlock(p,false);else l.scheduleTick(p,this,1200);
    }
    /** Where this cell's controller stands. */
    public BlockPos controller(BlockPos p,BlockState s){return p.subtract(ModeledMachine.position(BlockPos.ZERO,s.getValue(CivicBlock.FACING),cell(s)));}
    public static ModeledController owner(Level l,BlockPos p,BlockState s){
        if(!(s.getBlock() instanceof MachinePartBlock part))return null;
        var at=part.controller(p,s);var c=l.hasChunkAt(at)?ModeledMachine.of(l.getBlockEntity(at)):null;
        return c!=null&&c.model()==part.model&&c.front()==s.getValue(CivicBlock.FACING)?c:null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        var c=owner(l,p,s);
        if(c!=null&&!l.isClientSide&&CivicAccess.allowed(l,p,player)&&CivicAccess.allowed(l,c.entity().getBlockPos(),player)&&c instanceof net.minecraft.world.MenuProvider menu)player.openMenu(menu);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    /** Held construction materials build from any cell, as from the controller. */
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        var c=owner(l,p,s);if(c==null||!ModeledMachine.accepts(c,stack))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!l.isClientSide)ModeledMachine.build(c,player,stack);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    /** Hoppers reach the controller through any cell, with the face they touch: from above, the side or below, never across a claim boundary between that cell and the controller. */
    @Override public WorldlyContainer getContainer(BlockState s,LevelAccessor l,BlockPos p){
        if(!(l instanceof Level level))return null;var c=owner(level,p,s);
        return c instanceof WorldlyContainer container&&CivicAccess.boundary(level,p,c.entity().getBlockPos())?container:null;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!l.isClientSide&&!s.is(next.getBlock())){var c=owner(l,p,s);if(c!=null&&!c.build().changing)ModeledMachine.builtSection(c,cell(s)).ifPresent(part->ModeledMachine.removeSection(c,part,true));}
        super.onRemove(s,l,p,next,moving);
    }
}

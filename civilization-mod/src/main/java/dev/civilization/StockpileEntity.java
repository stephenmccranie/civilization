package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * One pallet's own stack, up to two blocks high: 128 blocks of solid volume, drawn one for one. The stack fills
 * the pallet's block, then rises into the block above while that block is free, which it then holds as its top.
 * Side-connected pallets share a menu through StockpileGroup.
 */
public final class StockpileEntity extends BlockEntity implements CargoHolder,MenuProvider {
    /** Eight layers of 4x4 quarter-size blocks above the 2-pixel timber: four fill the pallet's block and four the block above. */
    public static final CargoGrid TWO_HIGH=new CargoGrid(4,4,8,7f/32);
    /** The stack within the pallet's own block, while the block above is taken: a full stack is a full block. */
    public static final CargoGrid ONE_HIGH=new CargoGrid(4,4,4,7f/32);
    private final CargoLedger stack=new CargoLedger(()->grid().capacity(),this::changed);
    StockpileGroup group;
    private boolean dirty=true;
    public StockpileEntity(BlockPos p,BlockState s){super(CargoContent.ENTITY.get(),p,s);}
    private void changed(){if(group!=null)group.changed();setChanged();dirty=true;}
    /** The block above changed, so the stack may now rise into it or must stay below. */
    void aboveChanged(){dirty=true;}
    public void tick(){if(dirty){dirty=false;settleTop();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    /** Two blocks high while the stack holds the block above or that block is free, otherwise one. */
    public CargoGrid grid(){return level==null||holdsTop()||free(worldPosition.above())?TWO_HIGH:ONE_HIGH;}
    private boolean holdsTop(){var above=level.getBlockState(worldPosition.above());return above.is(CargoContent.STOCKPILE.get())&&above.getValue(StockpileBlock.TOP);}
    /** Air the stack can rise into, with nobody standing in its first layer. */
    private boolean free(BlockPos at){return !level.isOutsideBuildHeight(at)&&level.getBlockState(at).isAir()&&level.isUnobstructed(null,Shapes.create(new AABB(at).setMaxY(at.getY()+TWO_HIGH.height())));}
    /** The stack takes the block above once it rises past its own block, and gives it back once it no longer does. */
    private void settleTop(){
        boolean held=holdsTop(),wanted=stack.load()>ONE_HIGH.capacity()&&(held||free(worldPosition.above()));
        if(wanted&&!held)level.setBlockAndUpdate(worldPosition.above(),getBlockState().setValue(StockpileBlock.TOP,true));
        else if(!wanted&&held)level.removeBlock(worldPosition.above(),false);
    }
    /** This pallet's own goods, bottom of the stack first. Clients receive it to draw and shape the stack. */
    public CargoLedger stack(){return stack;}
    /** Started layers; an overloaded pallet draws as full. */
    public int layers(){var grid=grid();return (int)Math.ceil(Math.min(stack.load(),grid.capacity())/grid.layer());}
    private StockpileGroup group(){if(group==null)group=StockpileGroup.find(this);return group;}
    @Override public CargoInventory cargo(){return level==null||level.isClientSide||isRemoved()?stack:group();}
    public int palletCount(){return level==null||level.isClientSide||isRemoved()?1:group().pallets();}
    /** Puts the first held goods the pallets accept onto them, main hand first. False when nothing fits. */
    boolean putHeld(Player p){
        for(var hand:InteractionHand.values()){
            var held=p.getItemInHand(hand);var name=held.getHoverName();
            int n=(int)cargo().insert(held,held.getCount());
            if(n>0){held.shrink(n);p.displayClientMessage(Component.literal("Stacked "+n+" ").append(name).append(" · "+CargoInventory.blocks(cargo().load(),cargo().capacity())),true);return true;}
        }
        return false;
    }
    @Override public void onLoad(){super.onLoad();if(group!=null)group.invalidate();StockpileGroup.invalidateAround(level,worldPosition);}
    @Override public void setRemoved(){if(group!=null)group.invalidate();super.setRemoved();}
    @Override public Level cargoLevel(){return level;}
    @Override public Vec3 cargoPosition(){return worldPosition.getCenter();}
    @Override public boolean cargoValid(Player p,double reach){return level!=null&&p.level()==level&&!isRemoved()&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(cargoPosition())<=reach*reach&&group().allowed(p);}
    @Override public Component cargoName(){return Component.translatable("block.civilization.material_stockpile");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("Cargo",stack.save(r));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);stack.load(tag.getList("Cargo",10),r);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveCustomOnly(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

package dev.civilization;

import java.text.*;
import java.util.Locale;
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
import net.minecraft.world.phys.Vec3;

/** One pallet's own stack: 64 blocks of solid volume, drawn one for one. Side-connected pallets share a menu through StockpileGroup. */
public final class StockpileEntity extends BlockEntity implements CargoHolder,MenuProvider {
    public static final int CAPACITY=64,LAYER=16;
    private final MassCargo stack=new MassCargo(CargoMass::volume,()->CAPACITY,this::changed);
    StockpileGroup group;
    private boolean networkDirty;
    public StockpileEntity(BlockPos p,BlockState s){super(CargoContent.ENTITY.get(),p,s);}
    private void changed(){if(group!=null)group.changed();setChanged();networkDirty=true;}
    public void tick(){if(networkDirty){networkDirty=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    /** This pallet's own goods, bottom of the stack first. Clients receive it to draw and shape the stack. */
    public MassCargo stack(){return stack;}
    /** Started layers; an overloaded pallet draws as full. */
    public int layers(){return (int)Math.ceil(Math.min(stack.load(),CAPACITY)/LAYER);}
    private StockpileGroup group(){if(group==null)group=StockpileGroup.find(this);return group;}
    @Override public CargoInventory cargo(){return level==null||level.isClientSide||isRemoved()?stack:group();}
    public int palletCount(){return level==null||level.isClientSide||isRemoved()?1:group().pallets();}
    /** Puts the first held goods the pallets accept onto them, main hand first. False when nothing fits. */
    boolean putHeld(Player p){
        for(var hand:InteractionHand.values()){
            var held=p.getItemInHand(hand);var name=held.getHoverName();
            int n=(int)cargo().insert(held,held.getCount());
            if(n>0){held.shrink(n);p.displayClientMessage(Component.literal("Stacked "+n+" ").append(name).append(" · "+blocks(cargo().load(),cargo().capacity())),true);return true;}
        }
        return false;
    }
    public static String blocks(double load,double capacity){
        if(!Double.isFinite(load))return "Unknown goods: take them off";
        var format=new DecimalFormat("#,##0.###",DecimalFormatSymbols.getInstance(Locale.ROOT));
        return load>capacity?"Overloaded: "+format.format(load)+" / "+format.format(capacity):format.format(load)+" / "+format.format(capacity)+" blocks";
    }
    @Override public void onLoad(){super.onLoad();if(group!=null)group.invalidate();StockpileGroup.invalidateAround(level,worldPosition);}
    @Override public void setRemoved(){if(group!=null)group.invalidate();super.setRemoved();}
    @Override public Level cargoLevel(){return level;}
    @Override public Vec3 cargoPosition(){return worldPosition.getCenter();}
    @Override public boolean cargoValid(Player p){return level!=null&&p.level()==level&&!isRemoved()&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(cargoPosition())<=36&&group().allowed(p);}
    @Override public Component cargoName(){return Component.translatable("block.civilization.material_stockpile");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("Cargo",stack.save(r));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);stack.load(tag.getList("Cargo",10),r);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveCustomOnly(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

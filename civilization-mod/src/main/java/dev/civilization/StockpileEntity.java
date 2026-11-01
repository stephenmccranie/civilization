package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class StockpileEntity extends BlockEntity implements CargoHolder,MenuProvider {
    private final MassCargo cargo=new MassCargo(()->CargoConfig.STOCKPILE.get(),this::changed);
    StockpileGroup group;
    private ItemStack icon=ItemStack.EMPTY;
    private float fill;
    private boolean networkDirty;
    public StockpileEntity(BlockPos p,BlockState s){super(CargoContent.ENTITY.get(),p,s);}
    private void changed(){
        if(group!=null)group.changed();
        setChanged();icon=cargo.isEmpty()?ItemStack.EMPTY:cargo.entry(0).item();fill=(float)Math.min(1,cargo.mass()/cargo.capacity());
        networkDirty=true;
    }
    public void tick(){if(networkDirty){networkDirty=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    public ItemStack cargoIcon(){return icon;}
    public float cargoFill(){return fill;}
    MassCargo localCargo(){return cargo;}
    private StockpileGroup group(){if(group==null)group=StockpileGroup.find(this);return group;}
    @Override public CargoInventory cargo(){return level==null||level.isClientSide||isRemoved()?cargo:group();}
    public int palletCount(){return level==null||level.isClientSide||isRemoved()?1:group().pallets();}
    @Override public void onLoad(){super.onLoad();if(group!=null)group.invalidate();StockpileGroup.invalidateAround(level,worldPosition);}
    @Override public void setRemoved(){if(group!=null)group.invalidate();super.setRemoved();}
    @Override public Level cargoLevel(){return level;}
    @Override public Vec3 cargoPosition(){return worldPosition.getCenter();}
    @Override public boolean cargoValid(Player p){return level!=null&&p.level()==level&&!isRemoved()&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(cargoPosition())<=36&&group().allowed(p);}
    @Override public Component cargoName(){return Component.translatable("block.civilization.material_stockpile");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("Cargo",cargo.save(r));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);cargo.load(tag.getList("Cargo",10),r);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();if(!icon.isEmpty())t.put("Icon",icon.save(r));t.putFloat("Fill",fill);return t;}
    @Override public void handleUpdateTag(CompoundTag t,HolderLookup.Provider r){icon=ItemStack.parseOptional(r,t.getCompound("Icon"));fill=t.getFloat("Fill");}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider r){handleUpdateTag(packet.getTag(),r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

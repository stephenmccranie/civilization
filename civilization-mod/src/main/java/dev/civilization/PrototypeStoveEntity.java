package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One reserved batch, one fuel reserve, and one continuous cooking coordinate. */
public final class PrototypeStoveEntity extends BlockEntity implements CoalFireHost,MenuProvider,SkilletHolder {
    private final NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);
    public final CoalFire fire=new CoalFire(this);
    private int heat;
    private double dial=.6,paidRemainder;
    private boolean hasSkillet=true;
    private SkilletContents skillet=new SkilletContents();
    public PrototypeStoveEntity(BlockPos p,BlockState s){super(PrototypeStoveContent.ENTITY.get(),p,s);}
    public double dial(){return dial;} public double work(){return skillet.work();} public boolean batch(){return hasSkillet&&skillet.batch();}
    public boolean hasSkillet(){return hasSkillet;} @Override public SkilletContents skillet(){return skillet;}
    public ItemStack liftSkillet(){if(!hasSkillet)return ItemStack.EMPTY;var item=SkilletItem.stack(skillet,level);hasSkillet=false;skillet=new SkilletContents();changed();return item;}
    public boolean putSkillet(ItemStack item){if(hasSkillet||!item.is(PrototypeStoveContent.SKILLET.get()))return false;skillet=SkilletItem.contents(item,level);hasSkillet=true;item.shrink(1);changed();return true;}
    public boolean valid(Player p){return level!=null&&p.level()==level&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(CivicAccess.world(level,worldPosition))<=64&&CivicAccess.allowed(level,worldPosition,p);}
    public boolean dial(Player p,double value){if(!valid(p)||!Double.isFinite(value)||value<0||value>1)return false;dial=value;changed();return true;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean start(){
        if(!hasSkillet||batch()||!items.get(4).isEmpty()||items.get(0).getCount()<2||!items.get(0).is(Items.POTATO)||items.get(1).getCount()<2||!items.get(1).is(Items.CARROT)||!items.get(2).is(Items.BREAD))return false;
        double budget=2*kcal(Items.BAKED_POTATO)+2*kcal(Items.CARROT)+kcal(Items.BREAD);
        items.get(0).shrink(2);items.get(1).shrink(2);items.get(2).shrink(1);skillet.start(budget);changed();return true;
    }
    private static double kcal(Item i){var s=i.getDefaultInstance();return FoodCalories.of(s,s.getFoodProperties(null));}
    public boolean finish(){
        if(!hasSkillet||!skillet.edible()||!items.get(4).isEmpty())return false;
        items.set(4,skillet.serve());changed();
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.4f,1.2f);return true;
    }
    public void tick(){
        boolean working=false;double rate=StoveCooking.rate(dial),supplied=0;
        if(hasSkillet&&skillet.needsHeat()&&rate>0&&fire.lit()){
            int charge=(int)Math.ceil(rate-paidRemainder-1e-10);
            if(fire.supply(charge)){
                heat-=charge;fire.spent(charge);paidRemainder+=charge-rate;supplied=rate;working=true;
            }
        }
        if(hasSkillet){skillet.tick(supplied);setChanged();}
        fire.finish(1,working);
        var state=getBlockState();if(state.getValue(PrototypeStoveBlock.LIT)!=fire.lit())level.setBlock(worldPosition,state.setValue(PrototypeStoveBlock.LIT,fire.lit()),3);
        if(level.getGameTime()%5==0&&hasSkillet)changed();
    }
    /** Breaking returns the actual vessel and batch; no cooked inputs are refunded again. */
    public void dropBatch(){var pan=liftSkillet();if(!pan.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,pan);}
    public final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->(int)Math.round(dial*10000);case 1->batch()?1:0;case 2->fire.state();case 3->fire.remaining();case 4->hasSkillet?1:0;default->0;};}public void set(int i,int v){}public int getCount(){return 5;}};
    @Override public int fireHeat(){return heat;} @Override public void fireHeat(int n){heat=n;}
    @Override public int coalCapacity(){return ProductionEnergy.HEAT_TICKS;} @Override public int coalBudget(){return (int)Math.round(coalCapacity()*ThermalField.efficiency(level,worldPosition));}
    @Override public double coalWasteHeatFactor(){return ThermalRules.STOVE_WASTE_HEAT_FACTOR;}
    @Override public int[] coalFuelSlots(){return new int[]{3};}
    @Override public int getContainerSize(){return 5;} @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);setChanged();return s;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack s){items.set(i,s);s.limitSize(getMaxStackSize(s));setChanged();}
    @Override public boolean stillValid(Player p){return valid(p);}
    @Override public boolean canPlaceItem(int i,ItemStack s){return switch(i){case 0->s.is(Items.POTATO);case 1->s.is(Items.CARROT);case 2->s.is(Items.BREAD);case 3->s.is(KilnContent.MINERAL_COAL.get());default->false;};}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public Component getDisplayName(){return Component.literal("Prototype Stove");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new PrototypeStoveMenu(id,inv,this,data);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);ContainerHelper.saveAllItems(t,items,r);t.putDouble("dial",dial);t.merge(skillet.save());t.putBoolean("hasSkillet",hasSkillet);t.putDouble("paid",paidRemainder);t.putInt("heat",heat);fire.save(t);}
    private static double finite(double n,double fallback,double max){return Double.isFinite(n)?Math.clamp(n,0,max):fallback;}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);items.clear();ContainerHelper.loadAllItems(t,items,r);dial=finite(t.getDouble("dial"),.6,1);skillet=SkilletContents.load(t);hasSkillet=!t.contains("hasSkillet")||t.getBoolean("hasSkillet");paidRemainder=finite(t.getDouble("paid"),0,1);if(!hasSkillet)skillet=new SkilletContents();heat=Math.clamp(t.getInt("heat"),0,10000);fire.load(t);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

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
public final class PrototypeStoveEntity extends BlockEntity implements CoalFireHost,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);
    public final CoalFire fire=new CoalFire(this);
    private int heat;
    private double dial=.6,work,budget,paidRemainder;
    private boolean batch;
    public PrototypeStoveEntity(BlockPos p,BlockState s){super(PrototypeStoveContent.ENTITY.get(),p,s);}
    public double dial(){return dial;} public double work(){return work;} public boolean batch(){return batch;}
    public boolean valid(Player p){return level!=null&&p.level()==level&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(CivicAccess.world(level,worldPosition))<=64&&CivicAccess.allowed(level,worldPosition,p);}
    public boolean dial(Player p,double value){if(!valid(p)||!Double.isFinite(value)||value<0||value>1)return false;dial=value;changed();return true;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean start(){
        if(batch||!items.get(4).isEmpty()||items.get(0).getCount()<2||!items.get(0).is(Items.POTATO)||items.get(1).getCount()<2||!items.get(1).is(Items.CARROT)||!items.get(2).is(Items.BREAD))return false;
        budget=2*kcal(Items.BAKED_POTATO)+2*kcal(Items.CARROT)+kcal(Items.BREAD);
        items.get(0).shrink(2);items.get(1).shrink(2);items.get(2).shrink(1);batch=true;work=paidRemainder=0;changed();return true;
    }
    private static double kcal(Item i){var s=i.getDefaultInstance();return FoodCalories.of(s,s.getFoodProperties(null));}
    public boolean finish(){
        if(!batch||work<StoveCooking.EDIBLE||!items.get(4).isEmpty())return false;
        items.set(4,PrototypeStoveContent.meal(StoveCooking.quality(work),budget));batch=false;work=budget=paidRemainder=0;changed();
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.4f,1.2f);return true;
    }
    public void tick(){
        boolean working=false;double rate=StoveCooking.rate(dial);
        if(batch&&work<StoveCooking.MAX&&rate>0&&fire.lit()){
            int charge=(int)Math.ceil(rate-paidRemainder-1e-10);
            if(fire.supply(charge)){
                heat-=charge;fire.spent(charge);paidRemainder+=charge-rate;work=Math.min(StoveCooking.MAX,work+rate);working=true;setChanged();
            }
        }
        fire.finish(1,working);
        var state=getBlockState();if(state.getValue(PrototypeStoveBlock.LIT)!=fire.lit())level.setBlock(worldPosition,state.setValue(PrototypeStoveBlock.LIT,fire.lit()),3);
        if(level.getGameTime()%5==0&&batch)changed();
    }
    /** Breaking returns the reserved raw ingredients; cooking cannot mint another batch. */
    public void dropBatch(){if(batch){for(var s:new ItemStack[]{new ItemStack(Items.POTATO,2),new ItemStack(Items.CARROT,2),new ItemStack(Items.BREAD)})Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,s);batch=false;}}
    public final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->(int)Math.round(dial*10000);case 1->batch?1:0;case 2->fire.state();case 3->fire.remaining();default->0;};}public void set(int i,int v){}public int getCount(){return 4;}};
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
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);ContainerHelper.saveAllItems(t,items,r);t.putDouble("dial",dial);t.putDouble("work",work);t.putDouble("budget",budget);t.putDouble("paid",paidRemainder);t.putBoolean("batch",batch);t.putInt("heat",heat);fire.save(t);}
    private static double finite(double n,double fallback,double max){return Double.isFinite(n)?Math.clamp(n,0,max):fallback;}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);items.clear();ContainerHelper.loadAllItems(t,items,r);dial=finite(t.getDouble("dial"),.6,1);work=finite(t.getDouble("work"),0,StoveCooking.MAX);budget=finite(t.getDouble("budget"),0,1e9);paidRemainder=finite(t.getDouble("paid"),0,1);batch=t.getBoolean("batch");heat=Math.clamp(t.getInt("heat"),0,10000);fire.load(t);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

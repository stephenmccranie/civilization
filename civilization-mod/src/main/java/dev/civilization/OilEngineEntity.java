package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
/** One server owner of fluid stock, fractional work credit and the current load. */
public final class OilEngineEntity extends BlockEntity implements MenuProvider {
 public final FluidTank fuel=tank(4000,true),oil=tank(2000,false);
 public boolean enabled,complete; public int warmup,testTicks,status; public double condition=1,fuelCredit,oilCredit;
 private long workedTick=Long.MIN_VALUE; private int activity; private boolean checked;
 public OilEngineEntity(BlockPos p,BlockState s){super(OilEngineContent.ENTITY.get(),p,s);}
 private FluidTank tank(int capacity,boolean isFuel){return new FluidTank(capacity,s->s.is(isFuel?IndustrialContent.FUEL.get():IndustrialContent.LUBE.get())){ @Override public int fill(FluidStack stack,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action){int room=getSpace()-((isFuel?fuelCredit:oilCredit)>0?1:0);return room<=0?0:super.fill(stack.copyWithAmount(Math.min(stack.getAmount(),room)),action);}
 @Override protected void onContentsChanged(){OilEngineEntity.this.setChanged();} };}
 public Direction front(){return getBlockState().getValue(CivicBlock.FACING);}
 public boolean valid(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(CivicAccess.world(level,worldPosition))<=64&&CivicAccess.allowed(level,worldPosition,p);}
 public void control(int button){if(button==0){enabled=!enabled;if(!enabled){warmup=0;testTicks=0;}}else if(button==1&&enabled&&warmup>=100)testTicks=200;setChanged();}
 public void tick(){
  if(level==null||level.isClientSide)return;
  if(!checked||level.getGameTime()%10==0){checked=true;complete=OilEngineStructure.complete(level,worldPosition,front());EngineFlywheelBlock.refresh((net.minecraft.server.level.ServerLevel)level,worldPosition.relative(front().getCounterClockWise()).relative(front().getOpposite()).above());PipeRouting.transfer(level,worldPosition,front(),fuel,false);PipeRouting.transfer(level,worldPosition,Direction.DOWN,oil,false);}
  if(!complete){status=1;warmup=0;testTicks=0;activity=0;}
  else if(!enabled){status=0;activity=0;}
  else if(fuel.getFluidAmount()+fuelCredit<=0){status=2;warmup=0;testTicks=0;activity=0;}
  else if(warmup<100){
   if(warmup==0){if(fuel.getFluidAmount()<10){status=2;sync();return;}fuel.drain(10,EXECUTE);ThermalField.fuel(level,worldPosition,80);EnergyLog.machine(level,worldPosition,"engine_start","civilization:refined_fuel_mb",10,null,0);}
   warmup++;status=3;setChanged();
  }else {if(activity>0)activity--;status=activity>0?5:4;
   if(testTicks>0){requestWork(worldPosition,1);testTicks--;setChanged();}
  }
  sync();
 }
 /** Consumer adapter calls at most once per server tick; repeats receive no extra work. */
 public double requestWork(BlockPos consumer,double demand){
  if(level==null||level.isClientSide||!enabled||warmup<100||!Double.isFinite(demand)||demand<=0||workedTick==level.getGameTime())return 0;
  if(!consumer.equals(worldPosition)&&level.getBlockEntity(consumer)==null)return 0;
  if(!level.hasChunkAt(consumer)||consumer.distSqr(worldPosition)>64||!CivicAccess.boundary(level,worldPosition,consumer)||!OilEngineStructure.complete(level,worldPosition,front()))return 0;
  double beforeFuel=fuel.getFluidAmount()+fuelCredit;
  workedTick=level.getGameTime();var result=BoatEngine.step(fuel.getFluidAmount()+fuelCredit,oil.getFluidAmount()+oilCredit,condition,demand,.05);
  // Whole mB remain transferable; sub-mB credit stays inside the engine and is persisted.
  int f=(int)Math.floor(result.fuel()),o=(int)Math.floor(result.oil());fuelCredit=result.fuel()-f;oilCredit=result.oil()-o;
  fuel.drain(fuel.getFluidAmount()-f,EXECUTE);oil.drain(oil.getFluidAmount()-o,EXECUTE);condition=result.condition();ThermalField.fuel(level,worldPosition,Math.max(0,beforeFuel-result.fuel())*8);
  if(result.power()>0){activity=2;status=5;}setChanged();return result.power();
 }
 private int lastState=-1;
 private void sync(){int state=status|(int)Math.round(condition*1000)<<4;if(state!=lastState){lastState=state;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
 public void canister(Player p,InteractionHand hand){var held=p.getItemInHand(hand);FluidTank t=held.is(IndustrialContent.FUEL_CAN.get())?fuel:held.is(IndustrialContent.LUBE_CAN.get())?oil:null;
  if(t!=null){var s=new FluidStack(t==fuel?IndustrialContent.FUEL.get():IndustrialContent.LUBE.get(),1000);if(t.fill(s,SIMULATE)==1000){t.fill(s,EXECUTE);p.setItemInHand(hand,IndustrialContent.CAN.toStack());}}
  else if(held.is(IndustrialContent.CAN.get())){t=p.isShiftKeyDown()?oil:fuel;if(t.getFluidAmount()>=1000){t.drain(1000,EXECUTE);p.setItemInHand(hand,(t==fuel?IndustrialContent.FUEL_CAN:IndustrialContent.LUBE_CAN).toStack());}}
 }
 public final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->fuel.getFluidAmount();case 1->oil.getFluidAmount();case 2->status;case 3->warmup;case 4->(int)(condition*1000);case 5->enabled?1:0;case 6->testTicks;default->0;};}public void set(int i,int v){}public int getCount(){return 7;}};
 @Override public Component getDisplayName(){return Component.translatable("block.civilization.oil_engine");}
 @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new OilEngineMenu(id,inv,this,data);}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("fuel",fuel.writeToNBT(r,new CompoundTag()));t.put("oil",oil.writeToNBT(r,new CompoundTag()));t.putDouble("fuelCredit",fuelCredit);t.putDouble("oilCredit",oilCredit);t.putDouble("condition",condition);t.putBoolean("enabled",enabled);t.putInt("warmup",warmup);t.putInt("status",status);}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);fuel.readFromNBT(r,t.getCompound("fuel"));oil.readFromNBT(r,t.getCompound("oil"));fuelCredit=Math.clamp(t.getDouble("fuelCredit"),0,.999999);oilCredit=Math.clamp(t.getDouble("oilCredit"),0,.999999);condition=t.contains("condition")?Math.clamp(t.getDouble("condition"),.2,1):1;enabled=t.getBoolean("enabled");warmup=Math.clamp(t.getInt("warmup"),0,100);status=t.getInt("status");testTicks=0;}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}

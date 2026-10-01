package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.nio.file.*;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
@EventBusSubscriber(modid="civilization")
public final class BoatPersistenceCheck {
 private static final String PHASE=System.getProperty("civilization.boatPhase","manual");private static int ticks;
 @SubscribeEvent public static void start(ServerStartedEvent e){ticks=0;if(PHASE.equals("manual"))return;for(int x=0;x<3;x++)for(int z=0;z<3;z++)e.getServer().overworld().setChunkForced(x,z,true);}
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){if(PHASE.equals("manual")||++ticks!=100)return;try{
  var server=e.getServer();var l=server.overworld();var path=server.getWorldPath(LevelResource.ROOT).resolve("boat-check.txt");
  if(PHASE.equals("seed")){
   var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);for(var old:java.util.List.copyOf(BoatSystem.boats(l)))container.removeSubLevel(old,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
   for(int x=8;x<=24;x++)for(int z=8;z<=24;z++)for(int y=97;y<=104;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),y<100?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
   var at=new BlockPos(16,100,16);l.setBlockAndUpdate(at,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,at,Direction.NORTH,part);
   var s=BoatSystem.launch(l,at,FakePlayerFactory.getMinecraft(l));var engineAt=BoatSystem.helm(s).offset(0,0,1);l.setBlockAndUpdate(engineAt,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,Direction.NORTH));for(var part:OilEngineStructure.PARTS)MachineStructure.placePart(l,engineAt,Direction.NORTH,part);var engine=(OilEngineEntity)l.getBlockEntity(engineAt);engine.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1379),EXECUTE);engine.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),499),EXECUTE);engine.condition=.65;engine.enabled=true;engine.warmup=100;engine.setChanged();
   if(CivicAccess.allowed(l,BoatSystem.helm(s),null)||!CivicAccess.allowed(l,BoatSystem.helm(s),FakePlayerFactory.getMinecraft(l))||!BoatSystem.attached(s,BoatSystem.helm(s).offset(-3,-1,0)))throw new IllegalStateException("Boat owner/freeform edge check failed");
   var chest=BoatSystem.helm(s).offset(2,0,-2);l.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());((ChestBlockEntity)l.getBlockEntity(chest)).setItem(0,KilnContent.STEEL.toStack(23));l.getBlockEntity(chest).setChanged();
   Files.writeString(path,s.getUniqueId().toString());System.out.println("BOAT_SEED_PASS");
  }else{
   var id=java.util.UUID.fromString(Files.readString(path).trim());var s=BoatSystem.boats(l).stream().filter(b->b.getUniqueId().equals(id)).findFirst().orElseThrow();var engine=(OilEngineEntity)l.getBlockEntity(BoatSystem.helm(s).offset(0,0,1));
   if(engine.fuel.getFluidAmount()!=1379||engine.oil.getFluidAmount()!=499||engine.condition!=.65||!engine.enabled||engine.warmup!=100){var missing=new StringBuilder();for(var part:OilEngineStructure.PARTS){var p=MachineStructure.position(engine.getBlockPos(),engine.front(),part);if(!MachineStructure.matches(l,p,part,engine.front()))missing.append(part.material()).append('@').append(p).append('=').append(l.getBlockState(p)).append(' ');}throw new IllegalStateException("Physical engine state changed across restart: fuel="+engine.fuel.getFluidAmount()+" oil="+engine.oil.getFluidAmount()+" condition="+engine.condition+" enabled="+engine.enabled+" warmup="+engine.warmup+" complete="+engine.complete+" mismatches="+missing);}
   var chest=(ChestBlockEntity)l.getBlockEntity(BoatSystem.helm(s).offset(2,0,-2));if(!chest.getItem(0).is(KilnContent.STEEL.get())||chest.getItem(0).getCount()!=23)throw new IllegalStateException("Cargo lost across restart");
   System.out.println("BOAT_RELOAD_PASS uuid="+id+" engine_fuel=1379 oil=499 condition=0.65 cargo=23");
  }server.halt(false);
 }catch(Exception ex){throw new IllegalStateException("Boat persistence check failed",ex);}}
}

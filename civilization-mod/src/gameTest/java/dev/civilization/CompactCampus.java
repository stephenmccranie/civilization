package dev.civilization;
import static dev.civilization.TestCampusBuilder.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
/** Compact, local testing map. Development-only generation; saved machines use normal gameplay. */
final class CompactCampus {
 static void wallButton(ServerLevel l,int x,String command,String label){
  block(l,x,65,-5,Blocks.COMMAND_BLOCK.defaultBlockState());((CommandBlockEntity)l.getBlockEntity(p(x,65,-5))).getCommandBlock().setCommand(command);
  block(l,x,65,-3,Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,AttachFace.WALL).setValue(ButtonBlock.FACING,Direction.SOUTH));
  block(l,x,66,-3,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.SOUTH));
  var be=(SignBlockEntity)l.getBlockEntity(p(x,66,-3));be.setText(new SignText().setMessage(1,net.minecraft.network.chat.Component.literal(label)),true);be.setChanged();
 }
 static void build(MinecraftServer server){var l=server.overworld();
  var owner=FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("832c94e5-4050-463f-b56c-4c8b6a9fe67e"),"19BitPrice"));
  for(int x=-4;x<=5;x++)for(int z=-3;z<=5;z++)l.getChunk(x,z);
  l.setDefaultSpawnPos(p(0,64,4),180);l.setDayTime(3000);l.setWeatherParameters(100000,0,false,false);
  l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);l.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,server);l.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,server);l.getGameRules().getRule(GameRules.RULE_COMMANDBLOCKOUTPUT).set(false,server);
  pad(l,-10,-8,10,12);pad(l,-46,-3,72,3);pad(l,10,-22,72,38);pad(l,-44,-30,-24,-12);pad(l,-10,-22,10,-8);
  box(l,-10,64,-6,10,67,-4,Blocks.STONE_BRICKS.defaultBlockState());
  wallButton(l,-8,"gamemode survival @p[distance=..6]","Survival");wallButton(l,-5,"gamemode creative @p[distance=..6]","Creative");wallButton(l,-2,"execute as @p[distance=..6] run civilization calories set 0","Empty calories");wallButton(l,2,"execute as @p[distance=..6] run civilization calories set 2000","Refill calories");wallButton(l,5,"time set day","Day");wallButton(l,8,"time set midnight","Night");
  for(int x:new int[]{18,30,42,54}){var b=x==18?KilnContent.KILN.get():x==30?KilnContent.FOUNDRY.get():x==42?KilnContent.RETORT.get():CookingContent.STATION.get();var f=furnace(l,b,x,-14);f.setItem(0,stack(x==18?Items.CLAY_BALL:x==30?Items.RAW_IRON:x==42?IndustrialContent.ENRICHED_BLEND.get():CookingContent.BREAD_DOUGH.get()));f.setItem(1,stack(KilnContent.MINERAL_COAL.get()));sign(l,x,64,-17,x==18?"KILN":x==30?"FOUNDRY":x==42?"FERTILIZER":"COOKING");}
  refinery(l);((IndustrialBlockEntity)l.getBlockEntity(p(24,65,21))).input.drain(16000,EXECUTE);
  var sites=new ListTag();for(var site:java.util.List.of(new Deposits.Site(-34,64,-22,Deposits.Kind.COAL,8),new Deposits.Site(18,64,26,Deposits.Kind.OIL,8))){var t=new CompoundTag();t.putInt("x",site.x());t.putInt("y",site.y());t.putInt("z",site.z());t.putInt("kind",site.kind().ordinal());t.putInt("radius",site.radius());sites.add(t);}
  var authored=new CompoundTag();authored.put("minecraft:overworld",sites);server.getCommandStorage().set(ResourceLocation.parse("civilization:authored_deposits"),authored);
  deposit(l,new Deposits.Site(-34,64,-22,Deposits.Kind.COAL,8));deposit(l,new Deposits.Site(18,64,26,Deposits.Kind.OIL,8));
  // Pump front -> rising pipe -> crude tank -> the existing three-stage refinery.
  pipe(l,18,64,25);pipe(l,18,65,25);for(int z=21;z<25;z++)pipe(l,18,65,z);for(int x=19;x<24;x++)pipe(l,x,65,21);
  chest(l,-25,64,-19,"DRILL SUPPLIES",stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()),stack(IndustrialContent.CAN.get()),stack(IndustrialContent.PROBE.get()));
  for(int x=-29;x<=-15;x++)for(int z=-18;z<=-8;z++){block(l,x,63,z,x==-22?Blocks.WATER.defaultBlockState():Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));if(x!=-22)block(l,x,64,z,(x<-22?Blocks.WHEAT:FarmingContent.FERTILIZED_WHEAT.get()).defaultBlockState().setValue(CropBlock.AGE,7));}
  chest(l,-17,64,-5,"FARM / FOOD",stack(Items.IRON_HOE),stack(Items.WHEAT_SEEDS),stack(FarmingContent.FERTILIZER.get()),stack(Items.WHEAT),stack(FarmingContent.RATION.get()),stack(Items.BREAD));
  for(int x=-42;x<=-18;x+=8){box(l,x,64,10,x,68,10,Blocks.OAK_LOG.defaultBlockState());box(l,x-2,68,8,x+2,70,12,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));block(l,x,64,5,Blocks.OAK_SAPLING.defaultBlockState());}chest(l,-34,64,5,"FORESTRY",stack(Items.IRON_AXE),stack(Items.IRON_HOE),stack(Items.OAK_SAPLING),stack(Items.SPRUCE_SAPLING));
  var land=p(-6,64,-16);block(l,-6,64,-16,CivicContent.LAND.get().defaultBlockState());CivicService.placed(l,land,owner,true);
  for(int x:new int[]{0,6}){var at=p(x,64,-16);l.setBlockAndUpdate(at,CivicContent.SHOP.get().defaultBlockState());CivicService.placed(l,at,owner,false);var shop=CivicData.get(server).shops.get(CivicService.address(l,at));shop.template=stack(x==0?Items.WHEAT:KilnContent.STEEL.get()).copyWithCount(1);shop.amount=2;shop.price=1;shop.inventory.set(0,stack(KilnContent.MINERAL_COAL.get()));}CivicData.get(server).setDirty();
  var map=p(0,64,-10);l.setBlockAndUpdate(map,CivicContent.TABLE.get().defaultBlockState());for(var part:SurveyTable.PARTS)MachineStructure.placePart(l,map,Direction.NORTH,part);
  box(l,-48,58,18,4,63,80,Blocks.AIR.defaultBlockState());box(l,-48,58,18,4,58,80,Blocks.STONE_BRICKS.defaultBlockState());box(l,-47,59,19,3,62,79,Blocks.WATER.defaultBlockState());
  for(int z=20;z<=78;z++)block(l,4,63,z,Blocks.OAK_SLAB.defaultBlockState());box(l,-2,63,35,4,63,37,Blocks.OAK_PLANKS.defaultBlockState());
  for(int x:new int[]{-30,-12}){var at=p(x,63,40);l.setBlockAndUpdate(at,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,at,Direction.NORTH,part);var boat=BoatSystem.launch(l,at,owner);var tag=boat.getUserDataTag().copy();tag.putDouble("fuel",x==-30?3000:0);tag.putDouble("oil",x==-30?1000:0);boat.setUserDataTag(tag);l.setBlockAndUpdate(BoatSystem.helm(boat).offset(1,0,2),Blocks.BARREL.defaultBlockState());}
  chest(l,8,64,35,"BOAT SUPPLIES",stack(BoatContent.HELM.get()),stack(Items.OAK_PLANKS),stack(Items.CHEST),stack(Items.BARREL),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()));
  pad(l,14,-40,65,-26);for(int x=14;x<=50;x+=18){box(l,x,63,-40,x+14,63,-40,Blocks.YELLOW_CONCRETE.defaultBlockState());sign(l,x+6,64,-38,"BUILD BAY");}
  var items=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("civilization")).toList();for(int start=0;start<items.size();start+=27){int n=start/27;chest(l,18+n*5,64,-24,"MOD ITEMS "+(n+1),items.subList(start,Math.min(start+27,items.size())).stream().map(TestCampusBuilder::stack).toArray(ItemStack[]::new));}
  chest(l,60,64,-24,"BUILDING",stack(Items.BRICKS),stack(Items.COBBLESTONE),stack(Items.OAK_PLANKS),stack(Items.RAW_IRON),stack(Items.CLAY_BALL),stack(KilnContent.MINERAL_COAL.get()));block(l,66,64,-24,Blocks.CRAFTING_TABLE.defaultBlockState());
  for(var at:java.util.List.of(p(-11,0,3),p(11,0,3),p(28,0,5),p(48,0,5),p(70,0,5),p(-44,0,-8)))lamp(l,at.getX(),at.getZ());
  sign(l,0,64,9,"COMPACT CAMPUS","Controls on wall","Everything nearby","No travel annexes");
  chest(l,0,64,1,"START / SUPPLIES",stack(Items.IRON_PICKAXE),stack(Items.IRON_AXE),stack(Items.IRON_HOE),stack(KilnContent.MINERAL_COAL.get()),stack(Items.BREAD));
  verify(l);System.out.println("COMPACT_CAMPUS_BUILD_PASS");
 }
 static void deposit(ServerLevel l,Deposits.Site site){int x=site.x(),z=site.z();
  for(int i=0;i<site.cells();i++){var at=site.cell(i);if(site.body(at))l.setBlockAndUpdate(at,site.kind()==Deposits.Kind.COAL?IndustrialContent.COAL_SEAM.get().defaultBlockState():IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());}
  // Window and lit walk-in gallery retain half the real deposit. Ladder provides local access.
  box(l,x-9,site.bottom(),z+1,x+9,62,z+5,Blocks.AIR.defaultBlockState());box(l,x-9,site.bottom()-1,z+1,x+9,site.bottom()-1,z+5,Blocks.STONE_BRICKS.defaultBlockState());
  box(l,x-9,site.bottom(),z+1,x+9,site.top()+1,z+1,Blocks.GLASS.defaultBlockState());box(l,x-9,63,z+1,x+9,63,z+5,Blocks.GLASS.defaultBlockState());
  for(int xx=x-8;xx<=x+8;xx+=4)block(l,xx,site.bottom()-1,z+3,Blocks.SEA_LANTERN.defaultBlockState());
  for(int y=site.bottom();y<=63;y++){block(l,x+9,y,z+4,Blocks.STONE_BRICKS.defaultBlockState());block(l,x+8,y,z+4,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.WEST));}
  block(l,x+8,63,z+4,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.WEST));
  var m=machine(l,site.kind()==Deposits.Kind.COAL?IndustrialContent.DRILL.get():IndustrialContent.PUMP.get(),x,64,z);
  if(site.kind()==Deposits.Kind.COAL){m.input.fill(new FluidStack(IndustrialContent.FUEL.get(),4000),EXECUTE);m.lubricant.fill(new FluidStack(IndustrialContent.LUBE.get(),2000),EXECUTE);}else m.setItem(0,stack(KilnContent.MINERAL_COAL.get()));
  sign(l,x,64,z-3,site.kind()+" DEPOSIT","Finite / physical","Ladder to window");
 }
 static void verify(ServerLevel l){
  if(Deposits.at(l,p(-34,64,-22)).kind()!=Deposits.Kind.COAL||Deposits.at(l,p(18,64,26)).kind()!=Deposits.Kind.OIL)throw new IllegalStateException("Missing local deposits");
  if(!Geography.canFarm(l,p(-26,63,-10))||!Geography.woodland(l,p(-34,64,10)))throw new IllegalStateException("Farm/forest not eligible");
  for(int x:new int[]{18,30,42})if(((KilnBlockEntity)l.getBlockEntity(p(x,64,-14))).checkStructure().status()!=MachineStructure.COMPLETE)throw new IllegalStateException("Workshop incomplete");
  for(int x:new int[]{30,42,56})if(!IndustrialStructure.bind((IndustrialBlockEntity)l.getBlockEntity(p(x,64,20))))throw new IllegalStateException("Refinery incomplete");
  if(!SurveyTable.complete(l,p(0,64,-10)))throw new IllegalStateException("Map incomplete");
 }
}

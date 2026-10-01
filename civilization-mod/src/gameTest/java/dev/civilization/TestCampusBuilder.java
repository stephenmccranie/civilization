package dev.civilization;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.tags.FluidTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
/** Builds a separate reusable user testing save; never packaged in the mod. */
@EventBusSubscriber(modid="civilization")
public final class TestCampusBuilder {
 private static final boolean ENABLED=Boolean.getBoolean("civilization.buildCampus");private static boolean built;private static int ticks;private static Path world;
 private static final com.mojang.authlib.GameProfile USER=new com.mojang.authlib.GameProfile(UUID.fromString("832c94e5-4050-463f-b56c-4c8b6a9fe67e"),"19BitPrice");
 static BlockPos p(int x,int y,int z){return new BlockPos(x,y,z);}
 static void block(ServerLevel l,int x,int y,int z,BlockState b){l.setBlockAndUpdate(p(x,y,z),b);}
 static void box(ServerLevel l,int x1,int y1,int z1,int x2,int y2,int z2,BlockState b){for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=y1;y<=y2;y++)block(l,x,y,z,b);}
 static void pad(ServerLevel l,int x1,int z1,int x2,int z2){box(l,x1,63,z1,x2,63,z2,Blocks.STONE_BRICKS.defaultBlockState());}
 static void sign(ServerLevel l,int x,int y,int z,String... lines){block(l,x,y,z,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,8));var be=(SignBlockEntity)l.getBlockEntity(p(x,y,z));var text=new SignText();for(int i=0;i<Math.min(4,lines.length);i++)text=text.setMessage(i,Component.literal(lines[i]));be.setText(text,true);be.setText(text,false);be.setChanged();}
 static void chest(ServerLevel l,int x,int y,int z,String name,ItemStack... stacks){block(l,x,y,z,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH));var c=(ChestBlockEntity)l.getBlockEntity(p(x,y,z));for(int i=0;i<Math.min(27,stacks.length);i++)c.setItem(i,stacks[i]);c.setChanged();sign(l,x,y,z-1,name);}
 static ItemStack stack(ItemLike i){var s=new ItemStack(i);s.setCount(Math.min(32,s.getMaxStackSize()));return s;}
 static void button(ServerLevel l,int x,int y,int z,String command,String... label){block(l,x,y-1,z,Blocks.COMMAND_BLOCK.defaultBlockState());((CommandBlockEntity)l.getBlockEntity(p(x,y-1,z))).getCommandBlock().setCommand(command);block(l,x,y,z,Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,AttachFace.FLOOR));sign(l,x,y,z-1,label);}
 static void warp(ServerLevel l,int x,int z,String dim,BlockPos to,String name){button(l,x,64,z,"execute as @p[distance=..6] in "+dim+" run tp @s "+(to.getX()+.5)+" "+to.getY()+" "+(to.getZ()+.5)+" 0 0",name,"Press button");}
 static void lamp(ServerLevel l,int x,int z){for(int y=64;y<=67;y++)block(l,x,y,z,Blocks.DARK_OAK_FENCE.defaultBlockState());block(l,x,68,z,Blocks.LANTERN.defaultBlockState());}
 static KilnBlockEntity furnace(ServerLevel l,Block b,int x,int z){var at=p(x,64,z);block(l,x,64,z,b.defaultBlockState());var f=(KilnBlockEntity)l.getBlockEntity(at);if(f.requiresStructure())for(var part:MachineStructure.parts(f.getBlockState()))MachineStructure.placePart(l,at,Direction.NORTH,part);if(f.checkStructure().status()!=MachineStructure.COMPLETE)throw new IllegalStateException("Incomplete furnace "+b);return f;}
 static IndustrialBlockEntity machine(ServerLevel l,IndustrialBlock b,int x,int y,int z){var at=p(x,y,z);l.setBlockAndUpdate(at,b.defaultBlockState());var m=(IndustrialBlockEntity)l.getBlockEntity(at);if(b.kind==IndustrialBlock.Kind.PUMP)DerrickFixture.assemble(m);else for(var part:IndustrialStructure.parts(b.kind))MachineStructure.placePart(l,at,Direction.NORTH,part);if(!IndustrialStructure.bind(m))throw new IllegalStateException("Incomplete industrial machine "+b.kind);return m;}
 static void pipe(ServerLevel l,int x,int y,int z){block(l,x,y,z,IndustrialContent.PIPE.get().defaultBlockState());}
 static ServerLevel dimension(MinecraftServer s,String id){return Objects.requireNonNull(s.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(id))),id);}
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){if(!ENABLED)return;if(++ticks==40){world=e.getServer().getWorldPath(LevelResource.ROOT);if(Boolean.getBoolean("civilization.campusTouchup")){touchup(e.getServer());}else{if(Files.exists(world.resolve("CAMPUS_READY.txt")))throw new IllegalStateException("Campus already built; refusing to overwrite it");if(Boolean.getBoolean("civilization.campusCompact"))CompactCampus.build(e.getServer());else build(e.getServer());}built=true;}if(built&&ticks==100){try{Files.writeString(world.resolve("CAMPUS_READY.txt"),"Civilization Test Campus\nSpawn: 0 64 0\nSuperflat campus generation complete. See testing.md for the active layout.\n");}catch(Exception ex){throw new RuntimeException(ex);}System.out.println("CAMPUS_BUILD_PASS");e.getServer().halt(false);}}
 static void build(MinecraftServer server){var l=server.overworld();var owner=FakePlayerFactory.get(l,USER);
  for(int x=-7;x<=7;x++)for(int z=-5;z<=6;z++)l.getChunk(x,z);
  for(var level:server.getAllLevels()){level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,server);level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,server);level.getGameRules().getRule(GameRules.RULE_COMMANDBLOCKOUTPUT).set(false,server);level.setDayTime(3000);level.setWeatherParameters(100000,0,false,false);}
  l.setDefaultSpawnPos(p(0,64,0),0);
  pad(l,-10,-10,10,10);pad(l,-6,-62,6,96);pad(l,-92,-7,108,7);pad(l,10,-42,88,-14);pad(l,10,14,88,40);pad(l,10,52,88,88);pad(l,-83,-42,-17,-13);pad(l,92,-42,110,86);
  for(int x=-90;x<=108;x+=18){lamp(l,x,9);lamp(l,x,-9);}for(int z=-54;z<=90;z+=18){lamp(l,-8,z);lamp(l,8,z);}
  sign(l,0,64,5,"CIVILIZATION","TEST CAMPUS","Read guide chest","Creative + cheats");
  button(l,-7,64,0,"gamemode survival @p[distance=..6]","SURVIVAL","Calories / labor");button(l,7,64,0,"gamemode creative @p[distance=..6]","CREATIVE","Build / inspect");
  button(l,-7,64,7,"execute as @p[distance=..6] run civilization calories set 0","EMPTY CALORIES","Recovery test");button(l,7,64,7,"time set day","DAYLIGHT");
  button(l,-7,64,-7,"execute as @p[distance=..6] run civilization calories set 2000","REFILL CALORIES");button(l,7,64,-7,"time set midnight","NIGHT / LIGHTS");
  workshop(l);refinery(l);farms(l);boats(l,owner);civic(l,owner);construction(l);catalog(l);
  var coal=mining(server,"civilization_test:coal",Deposits.Kind.COAL);var oil=mining(server,"civilization_test:oil",Deposits.Kind.OIL);
  warp(l,-4,-56,"civilization_test:coal",coal,"COAL QUARRY");warp(l,4,-56,"civilization_test:oil",oil,"OIL FIELD");forest(server);warp(l,0,-64,"civilization_test:forest",p(0,64,0),"FORESTRY");
  touchup(server);
  var pages=List.of("Welcome to Civilization Test Campus. Everything here is disposable. Existing worlds are untouched.\n\nSpawn buttons switch Creative/Survival, refill/empty calories, and change day/night.","WEST: deep boat basin. Two boats belong to you: supplied and empty. Add cabins/cargo AFTER launch. Right-click helm to pilot; WASD; Shift exits. Canisters fill fuel/oil. Fixed hulls cannot be dismantled yet.","EAST: workshop and refinery. Controllers face north. Stocked chests hold inputs, coal and canisters. Machines may pause with full outputs: empty them to resume. Pipe glass strips show moving fluids.","NORTHWEST: river farming, fertilizer and food. Raised soil is deliberately too high for farming. Forest button visits proper woodland. Bone meal remains disabled.","NORTH: claims, trade counters and Survey Table. They belong to your account. Claim is unfunded so it cannot accidentally interrupt factory transfers. Add coal to test it. Another player is needed for genuine ownership competition.","SOUTH/EAST: cut blocks and empty assembly bays. Supplies include every custom item, controllers, saws and raw building materials. Breaking one multiblock part lets you inspect its guide.","NORTH buttons visit coal/oil annexes. They are also superflat, with suitable natural biomes and real finite deposits. Pumps/drills consume actual blocks/fluid. Lower viewing galleries show depletion. Return buttons lead here.","These are testing supplies, not progression rewards. No special machine cheats run after generation. Default 60-minute day/night cycle stays active. Keep Inventory is on; natural monster spawning is off.");
  var book=new ItemStack(Items.WRITTEN_BOOK);book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Campus Guide"),"Civilization",0,pages.stream().map(t->net.minecraft.server.network.Filterable.<Component>passThrough(Component.literal(t))).toList(),true));
  chest(l,0,64,-4,"START HERE",book,stack(Items.IRON_PICKAXE),stack(Items.IRON_AXE),stack(Items.IRON_HOE),stack(KilnContent.MINERAL_COAL.get()),stack(Items.BREAD),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()));
 }
 static void workshop(ServerLevel l){
  var kiln=furnace(l,KilnContent.KILN.get(),18,-28);kiln.setItem(0,stack(Items.CLAY_BALL));kiln.setItem(1,stack(KilnContent.MINERAL_COAL.get()));sign(l,18,64,-31,"KILN","Clay -> bricks");
  var f=furnace(l,KilnContent.FOUNDRY.get(),32,-28);f.setItem(0,stack(Items.RAW_IRON));f.setItem(1,stack(KilnContent.MINERAL_COAL.get()));sign(l,32,64,-31,"FOUNDRY","Ore -> metal","Iron -> steel");
  var works=furnace(l,KilnContent.RETORT.get(),46,-28);works.setItem(0,stack(IndustrialContent.ENRICHED_BLEND.get()));works.setItem(1,stack(KilnContent.MINERAL_COAL.get()));sign(l,46,64,-31,"FERTILIZER","Sulfur + gravel","Blend -> 8");
  var stove=furnace(l,CookingContent.STATION.get(),60,-28);stove.setItem(0,stack(CookingContent.BREAD_DOUGH.get()));stove.setItem(1,stack(KilnContent.MINERAL_COAL.get()));sign(l,60,64,-31,"COOKING","Dough / raw food");
  chest(l,18,64,-18,"KILN INPUTS",stack(Items.CLAY_BALL),stack(KilnContent.MINERAL_COAL.get()),stack(Items.BRICKS));
  chest(l,32,64,-18,"METALWORK",stack(Items.RAW_IRON),stack(Items.RAW_COPPER),stack(Items.RAW_GOLD),stack(Items.IRON_INGOT),stack(KilnContent.MINERAL_COAL.get()),stack(KilnContent.STEEL.get()),stack(KilnContent.MACHINE_PARTS.get()));
  chest(l,46,64,-18,"FERTILIZER",stack(IndustrialContent.ENRICHED_BLEND.get()),stack(IndustrialContent.ENRICHED_BLEND.get()),stack(IndustrialContent.SULFUR.get()),stack(KilnContent.MINERAL_COAL.get()));
  chest(l,60,64,-18,"KITCHEN",stack(CookingContent.BREAD_DOUGH.get()),stack(CookingContent.COOKIE_DOUGH.get()),stack(CookingContent.UNBAKED_PIE.get()),stack(CookingContent.CAKE_BATTER.get()),stack(Items.BEEF),stack(Items.POTATO),stack(KilnContent.MINERAL_COAL.get()));
 }
 static void refinery(ServerLevel l){
  var heat=machine(l,IndustrialContent.REFINERY.get(),30,64,20);machine(l,IndustrialContent.COLUMN.get(),42,64,20);machine(l,IndustrialContent.CONDENSER.get(),56,64,20);
  var crude=machine(l,IndustrialContent.TANK.get(),24,65,21);machine(l,IndustrialContent.TANK.get(),64,66,22);machine(l,IndustrialContent.TANK.get(),50,65,25);
  for(int x=25;x<=27;x++)pipe(l,x,65,21);for(int x=33;x<=39;x++)pipe(l,x,65,22);for(int x=45;x<=52;x++)pipe(l,x,73,22);for(int y=66;y<73;y++)pipe(l,52,y,22);for(int z=22;z<=25;z++)pipe(l,45,65,z);for(int x=46;x<50;x++)pipe(l,x,65,25);for(int x=60;x<64;x++)pipe(l,x,66,22);
  crude.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),16000),EXECUTE);heat.setItem(0,stack(KilnContent.MINERAL_COAL.get()));
  sign(l,30,64,17,"FIRED HEATER","Coal + crude");sign(l,42,64,17,"DISTILLATION","Vapor / oil","Sulfur below");sign(l,56,64,17,"CONDENSER","Vapor -> fuel");
  chest(l,70,64,20,"REFINERY STOCK",stack(KilnContent.MINERAL_COAL.get()),stack(IndustrialContent.CRUDE_CAN.get()),stack(IndustrialContent.CRUDE_CAN.get()),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()),stack(IndustrialContent.CAN.get()),stack(IndustrialContent.PIPE.get()),stack(IndustrialContent.PORT.get()));
  sign(l,64,64,25,"FUEL OUTPUT","Empty canister","on the tank");sign(l,50,64,29,"OIL OUTPUT","Drain if full","to keep refining");
 }
 static void farms(ServerLevel l){
  for(int x=-76;x<=-26;x++)for(int z=-36;z<=-20;z++){if(x%10==0)block(l,x,63,z,Blocks.WATER.defaultBlockState());else {block(l,x,63,z,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));block(l,x,64,z,(x<-52?Blocks.WHEAT:FarmingContent.FERTILIZED_WHEAT.get()).defaultBlockState().setValue(CropBlock.AGE,x%3==0?7:0));}}
  sign(l,-67,64,-38,"RIVER FARM","Normal wheat");sign(l,-38,64,-38,"FERTILIZED","Harvest and","compare yield");
  chest(l,-46,64,-15,"FARM / FOOD",stack(Items.IRON_HOE),stack(Items.WHEAT_SEEDS),stack(FarmingContent.FERTILIZER.get()),stack(Items.WHEAT),stack(FarmingContent.RATION.get()),stack(Items.BREAD),stack(Items.COOKED_BEEF),stack(Items.APPLE),stack(Items.BONE_MEAL));
  box(l,-22,63,-35,-18,73,-31,Blocks.DIRT.defaultBlockState());box(l,-22,74,-35,-18,74,-31,Blocks.GRASS_BLOCK.defaultBlockState());sign(l,-20,75,-32,"TOO HIGH","Hoe test","No farming");
  pad(l,-82,-58,-55,-47);for(int x=-80;x<=-56;x++)for(int z=-57;z<=-48;z++)if(x==-80||x==-56||z==-57||z==-48)block(l,x,64,z,Blocks.OAK_FENCE.defaultBlockState());
  for(var type:List.of(net.minecraft.world.entity.EntityType.COW,net.minecraft.world.entity.EntityType.PIG,net.minecraft.world.entity.EntityType.SHEEP,net.minecraft.world.entity.EntityType.CHICKEN)){var a=type.create(l);a.moveTo(-77+new Random(type.hashCode()).nextInt(16),64,-52,0,0);l.addFreshEntity(a);}sign(l,-67,64,-59,"FOOD / ANIMALS","Survival tests");
 }
 static void boats(ServerLevel l,ServerPlayer owner){
  box(l,-92,58,14,-16,63,94,Blocks.AIR.defaultBlockState());box(l,-92,58,14,-16,58,94,Blocks.STONE_BRICKS.defaultBlockState());box(l,-91,59,15,-17,62,93,Blocks.WATER.defaultBlockState());
  for(int z=16;z<=92;z++)block(l,-16,63,z,Blocks.OAK_SLAB.defaultBlockState());for(int x=-38;x<=-16;x++)for(int z=38;z<=41;z++)block(l,x,63,z,Blocks.OAK_SLAB.defaultBlockState());
  for(int i=0;i<2;i++){var at=p(-47-i*22,63,45);block(l,at.getX(),at.getY(),at.getZ(),BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,at,Direction.NORTH,part);var boat=BoatSystem.launch(l,at,owner);var t=boat.getUserDataTag().copy();t.putDouble("fuel",i==0?3000:0);t.putDouble("oil",i==0?1000:0);boat.setUserDataTag(t);var hp=BoatSystem.helm(boat);l.setBlockAndUpdate(hp.offset(1,0,2),Blocks.BARREL.defaultBlockState());((BarrelBlockEntity)l.getBlockEntity(hp.offset(1,0,2))).setItem(0,stack(KilnContent.STEEL.get()));}
  var at=p(-26,63,53);block(l,at.getX(),at.getY(),at.getZ(),BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())if(part.depth()>0)MachineStructure.placePart(l,at,Direction.NORTH,part);
  chest(l,-12,64,38,"BOAT SUPPLIES",stack(BoatContent.HELM.get()),stack(Items.OAK_PLANKS),stack(Items.OAK_PLANKS),stack(Items.BARREL),stack(Items.CHEST),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()),stack(IndustrialContent.CAN.get()));
  sign(l,-12,64,43,"BOAT BASIN","2 owned boats","Fueled / empty","Partial hull too");
  box(l,-65,59,76,-57,63,83,Blocks.DIRT.defaultBlockState());box(l,-65,64,76,-57,64,83,Blocks.GRASS_BLOCK.defaultBlockState());
 }
 static void civic(ServerLevel l,ServerPlayer owner){
  pad(l,-12,-48,12,-22);var data=CivicData.get(l.getServer());
  var land=p(-8,64,-36);l.setBlockAndUpdate(land,CivicContent.LAND.get().defaultBlockState());CivicService.placed(l,land,owner,true);sign(l,-8,64,-39,"LAND CONTROLLER","Yours; unfunded","Add coal to test");
  for(int x:new int[]{0,7}){var at=p(x,64,-36);l.setBlockAndUpdate(at,CivicContent.SHOP.get().defaultBlockState());CivicService.placed(l,at,owner,false);var s=data.shops.get(CivicService.address(l,at));s.template=stack(x==0?Items.WHEAT:KilnContent.STEEL.get()).copyWithCount(1);s.amount=x==0?8:2;s.price=1;s.inventory.set(0,stack(KilnContent.MINERAL_COAL.get()));sign(l,x,64,-39,"TRADE COUNTER",x==0?"Buys 8 wheat":"Buys 2 steel","for 1 coal");}
  var at=p(0,64,-25);l.setBlockAndUpdate(at,CivicContent.TABLE.get().defaultBlockState());for(var part:SurveyTable.PARTS)MachineStructure.placePart(l,at,Direction.NORTH,part);sign(l,0,64,-28,"SURVEY TABLE","Claims + shops");data.setDirty();
  chest(l,-8,64,-25,"CIVIC SUPPLIES",stack(KilnContent.MINERAL_COAL.get()),stack(KilnContent.MINERAL_COAL.get()),stack(KilnContent.MINERAL_COAL.get()),stack(CivicContent.LAND.get()),stack(CivicContent.SHOP.get()),stack(CivicContent.TABLE.get()),stack(CivicContent.TABLE_PART.get()),stack(Items.WHEAT),stack(KilnContent.STEEL.get()));
 }
 static void construction(ServerLevel l){for(int x=16;x<=76;x+=20){pad(l,x,56,x+15,82);for(int z=56;z<=82;z++){block(l,x,63,z,Blocks.YELLOW_CONCRETE.defaultBlockState());block(l,x+15,63,z,Blocks.YELLOW_CONCRETE.defaultBlockState());}sign(l,x+7,64,54,"ASSEMBLY BAY","Build / break","Test the guides");}
  chest(l,23,64,80,"BUILD MATERIALS",stack(Items.COBBLESTONE),stack(Items.BRICKS),stack(Items.COPPER_BLOCK),stack(IndustrialContent.CASING.get()),stack(IndustrialContent.GUARDRAIL.get()),stack(IndustrialContent.COOLING.get()),stack(IndustrialContent.FLUE.get()),stack(Items.LADDER));
  var list=new ArrayList<ItemStack>();for(var item:BuiltInRegistries.ITEM)if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("civilization")&&BuiltInRegistries.ITEM.getKey(item).getPath().contains("saw"))list.add(stack(item));for(var b:List.of(Blocks.OAK_PLANKS,Blocks.BRICKS,Blocks.COBBLESTONE,IndustrialContent.CASING.get()))for(int units:new int[]{2,1,3})list.add(CuttingContent.stack(b.defaultBlockState(),units,16));chest(l,43,64,80,"CUTTING / JOIN",list.toArray(ItemStack[]::new));
  chest(l,63,64,80,"CONTROLLERS",stack(KilnContent.KILN.get()),stack(KilnContent.FOUNDRY.get()),stack(KilnContent.RETORT.get()),stack(IndustrialContent.REFINERY.get()),stack(IndustrialContent.COLUMN.get()),stack(IndustrialContent.CONDENSER.get()),stack(IndustrialContent.PUMP.get()),stack(IndustrialContent.DRILL.get()));
 }
 static void catalog(ServerLevel l){var items=BuiltInRegistries.ITEM.stream().filter(i->BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("civilization")).sorted(Comparator.comparing(i->BuiltInRegistries.ITEM.getKey(i).toString())).toList();for(int start=0;start<items.size();start+=27){int index=start/27;chest(l,100,64,-30+index*5,"MOD CATALOG "+(index+1),items.subList(start,Math.min(start+27,items.size())).stream().map(TestCampusBuilder::stack).toArray(ItemStack[]::new));}sign(l,100,64,-37,"ALL MOD ITEMS","Stocked catalog");block(l,96,64,-30,Blocks.CRAFTING_TABLE.defaultBlockState());block(l,96,64,-26,Blocks.STONECUTTER.defaultBlockState());block(l,96,64,-22,Blocks.ANVIL.defaultBlockState());}
 static BlockPos mining(MinecraftServer server,String id,Deposits.Kind kind){var l=dimension(server,id);Deposits.Site site=null;for(int radius=0;radius<=6&&site==null;radius++)for(int x=-radius;x<=radius&&site==null;x++)for(int z=-radius;z<=radius&&site==null;z++){var candidate=Deposits.candidate(l,x,z);if(candidate!=null&&candidate.kind()==kind)site=candidate;}if(site==null)throw new IllegalStateException("No real "+kind+" site in flat annex");
  int x=site.x(),z=site.z();for(int cx=(x-40)>>4;cx<=(x+40)>>4;cx++)for(int cz=(z-40)>>4;cz<=(z+40)>>4;cz++)l.getChunk(cx,cz);pad(l,x-20,z-12,x+20,z+16);
  for(int i=0;i<site.cells();i++){var at=site.cell(i);if(site.body(at))l.setBlockAndUpdate(at,kind==Deposits.Kind.COAL?IndustrialContent.COAL_SEAM.get().defaultBlockState():IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());}
  // A retained half-deposit behind glass makes actual block/fluid depletion visible.
  box(l,x-site.radius()-2,site.bottom(),z+1,x+site.radius()+2,site.top()+3,z+site.radius()+5,Blocks.AIR.defaultBlockState());box(l,x-site.radius()-2,site.bottom()-1,z+1,x+site.radius()+2,site.bottom()-1,z+site.radius()+5,Blocks.STONE_BRICKS.defaultBlockState());box(l,x-site.radius()-1,site.bottom(),z+1,x+site.radius()+1,site.top()+1,z+1,Blocks.GLASS.defaultBlockState());
  var m=machine(l,kind==Deposits.Kind.COAL?IndustrialContent.DRILL.get():IndustrialContent.PUMP.get(),x,64,z-4);
  if(kind==Deposits.Kind.COAL){m.input.fill(new FluidStack(IndustrialContent.FUEL.get(),4000),EXECUTE);m.lubricant.fill(new FluidStack(IndustrialContent.LUBE.get(),2000),EXECUTE);}else m.setItem(0,stack(KilnContent.MINERAL_COAL.get()));
  chest(l,x+7,64,z-4,"EXTRACTION",stack(KilnContent.MINERAL_COAL.get()),stack(IndustrialContent.FUEL_CAN.get()),stack(IndustrialContent.LUBE_CAN.get()),stack(IndustrialContent.CAN.get()),stack(IndustrialContent.PIPE.get()),stack(IndustrialContent.TANK.get()),stack(IndustrialContent.PROBE.get()));
  sign(l,x,64,z-8,kind+" EXTRACTION","Real finite body","Cutaway below");warp(l,x-8,z+8,"minecraft:overworld",p(0,64,0),"RETURN CAMPUS");warp(l,x+8,z+8,id,p(x,site.bottom(),z+5),"VIEW DEPOSIT");
  button(l,x,site.bottom(),z+6,"execute as @p[distance=..6] in "+id+" run tp @s "+x+" 64 "+(z+10),"BACK TO TOP");
  for(int xx=x-site.radius();xx<=x+site.radius();xx+=4)block(l,xx,site.bottom(),z+site.radius()+3,Blocks.SEA_LANTERN.defaultBlockState());
  if(Deposits.at(l,m.getBlockPos())==null)throw new IllegalStateException("Extractor not over actual site");return p(x,64,z+10);
 }
 static void forest(MinecraftServer server){var l=dimension(server,"civilization_test:forest");for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);pad(l,-6,-6,6,6);warp(l,0,3,"minecraft:overworld",p(0,64,0),"RETURN CAMPUS");sign(l,0,64,-3,"WOODLAND","Hoe: fast trees","Plant / harvest");chest(l,4,64,0,"TIMBER",stack(Items.IRON_AXE),stack(Items.IRON_HOE),stack(Items.OAK_SAPLING),stack(Items.SPRUCE_SAPLING),stack(Items.OAK_LOG),stack(Items.OAK_PLANKS));for(int x=-24;x<=24;x+=12)for(int z=12;z<=36;z+=12){box(l,x,64,z,x,68,z,Blocks.OAK_LOG.defaultBlockState());box(l,x-2,68,z-2,x+2,70,z+2,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));}for(int x=-20;x<=20;x+=5)block(l,x,64,-12,Blocks.OAK_SAPLING.defaultBlockState());}
 static void touchup(MinecraftServer server){
  for(String id:List.of("civilization_test:coal","civilization_test:oil")){var l=dimension(server,id);Deposits.Site site=null;for(int radius=0;radius<=6&&site==null;radius++)for(int x=-radius;x<=radius&&site==null;x++)for(int z=-radius;z<=radius&&site==null;z++)site=Deposits.candidate(l,x,z);if(site==null)throw new IllegalStateException("Missing site");
   for(int x=site.x()-site.radius();x<=site.x()+site.radius();x+=3){block(l,x,site.top()+2,site.z()+2,Blocks.SEA_LANTERN.defaultBlockState());block(l,x,site.bottom()-1,site.z()+2,Blocks.SEA_LANTERN.defaultBlockState());}
  }
 }
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){if(!ENABLED||!built||world==null)return;try{var file=world.resolve("level.dat");var nbt=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());var data=nbt.getCompound("Data");var dims=data.getCompound("WorldGenSettings").getCompound("dimensions");for(var key:new ArrayList<>(dims.getAllKeys()))if(key.startsWith("civilization:test_"))dims.remove(key);data.getCompound("GameRules").putString("spawnRadius","0");data.putString("LevelName",Boolean.getBoolean("civilization.campusCompact")?"Civilization Compact Campus":"Civilization Test Campus");data.putBoolean("allowCommands",true);data.putInt("GameType",1);data.putInt("SpawnX",0);data.putInt("SpawnY",64);data.putInt("SpawnZ",4);NbtIo.writeCompressed(nbt,file);System.out.println("CAMPUS_SAVED_PASS");}catch(Exception ex){throw new IllegalStateException("Could not finalize campus",ex);}}
}

package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public class BulkStorageGameTests {
    static BulkEntity build(GameTestHelper h,boolean liquid,Direction facing){return build(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),liquid,facing);}
    static BulkEntity build(net.minecraft.server.level.ServerLevel l,BlockPos at,boolean liquid,Direction facing){l.setBlockAndUpdate(at,(liquid?BulkContent.TANK:BulkContent.BUNKER).get().defaultBlockState().setValue(CivicBlock.FACING,facing));for(var part:BulkStructure.parts(liquid))MachineStructure.placePart(l,at,facing,part);return (BulkEntity)l.getBlockEntity(at);}
    @GameTest(template="industrial") public static void rotatedStoresAndCapacity(GameTestHelper h){
        for(var d:Direction.Plane.HORIZONTAL){var b=build(h,false,d);h.assertTrue(b.ready(),"Complete bunker "+d);h.assertTrue(b.insertCoal(9000,true)==8192&&b.amount()==0,"Simulation never stores");h.assertTrue(b.insertCoal(9000,false)==8192&&b.insertCoal(1,false)==0,"Exact capacity");var io=b.items(d);h.assertTrue(io.getStackInSlot(1).getCount()==32&&io.extractItem(1,500,true).getCount()==32&&b.amount()==8192,"Ordinary item stacks remain capped");h.assertTrue(io.insertItem(0,new ItemStack(Items.DIRT),false).is(Items.DIRT),"Reject other materials");h.assertTrue(b.extractCoal(9000,false)==8192,"Exact withdrawal");h.getLevel().removeBlock(b.getBlockPos(),false);var t=build(h,true,d);h.assertTrue(t.ready(),"Complete tank "+d);h.getLevel().removeBlock(t.getBlockPos(),false);}
        h.succeed();
    }
    @GameTest(template="industrial") public static void incompletePersistenceAndCachedHandlers(GameTestHelper h){
        var b=build(h,false,Direction.NORTH);b.insertCoal(1337,false);var io=b.items(Direction.NORTH);var part=BulkStructure.parts(false).getFirst();var pos=MachineStructure.position(b.getBlockPos(),b.front(),part);h.getLevel().removeBlock(pos,false);
        h.assertTrue(io.extractItem(1,32,false).isEmpty()&&io.insertItem(0,KilnContent.MINERAL_COAL.toStack(),false).getCount()==1,"Cached handler rechecks shell");h.assertTrue(b.amount()==1337,"Broken shell retains cargo");
        var data=b.saveWithoutMetadata(h.getLevel().registryAccess());b.loadWithComponents(data,h.getLevel().registryAccess());h.assertTrue(b.amount()==1337&&!b.ready(),"Reload retains locked cargo");MachineStructure.placePart(h.getLevel(),b.getBlockPos(),b.front(),part);h.assertTrue(io.extractItem(1,32,false).getCount()==32&&b.amount()==1305,"Repair restores transfer");
        h.getLevel().removeBlock(b.getBlockPos(),false);h.assertTrue(io.extractItem(1,32,false).isEmpty(),"Removed controller invalidates cached access");h.succeed();
    }
    @GameTest(template="industrial") public static void liquidConservationAndMixing(GameTestHelper h){
        var b=build(h,true,Direction.NORTH);var io=b.fluids(Direction.NORTH);var fuel=new FluidStack(IndustrialContent.FUEL.get(),65000);
        h.assertTrue(io.fill(fuel,SIMULATE)==64000&&b.amount()==0,"Fluid simulation");h.assertTrue(io.fill(fuel,EXECUTE)==64000&&b.amount()==64000,"Exact tank capacity");h.assertTrue(io.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE)==0,"No mixing");
        var menu=new BulkMenu(1,new net.minecraft.world.entity.player.Inventory(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)),b,b.data);h.assertTrue(menu.amount()==64000,"Unsigned menu quantity");
        var save=b.saveWithoutMetadata(h.getLevel().registryAccess());b.loadWithComponents(save,h.getLevel().registryAccess());h.assertTrue(b.amount()==64000&&b.fluid().is(IndustrialContent.FUEL.get()),"Liquid persistence");
        h.assertTrue(io.drain(100000,EXECUTE).getAmount()==64000&&io.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE)==1000,"Empty tank changes product");h.assertTrue(io.fill(new FluidStack(IndustrialContent.VAPOR.get(),1000),EXECUTE)==0,"No process intermediates");h.succeed();
    }
    @GameTest(template="industrial") public static void canistersAndRemovalPolicy(GameTestHelper h){
        var b=build(h,true,Direction.NORTH);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(b.getBlockPos().getCenter());var hand=net.minecraft.world.InteractionHand.MAIN_HAND;p.setItemInHand(hand,IndustrialContent.FUEL_CAN.toStack(2));b.useHeld(p,hand);h.assertTrue(b.amount()==1000&&p.getMainHandItem().getCount()==1,"One whole filled canister");
        p.setItemInHand(hand,IndustrialContent.CAN.toStack());b.useHeld(p,hand);h.assertTrue(b.amount()==0&&p.getMainHandItem().is(IndustrialContent.FUEL_CAN.get()),"Whole empty canister exchanged");
        b.fluids(Direction.NORTH).fill(new FluidStack(IndustrialContent.FUEL.get(),63500),EXECUTE);b.useHeld(p,hand);h.assertTrue(b.amount()==63500&&p.getMainHandItem().is(IndustrialContent.FUEL_CAN.get()),"Insufficient room leaves canister intact");
        var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),b.getBlockPos(),b.getBlockState(),p);BulkBlock.breaking(event);h.assertTrue(event.isCanceled(),"Stocked controller protected");b.fluids(Direction.NORTH).drain(64000,EXECUTE);var empty=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),b.getBlockPos(),b.getBlockState(),p);BulkBlock.breaking(empty);h.assertTrue(!empty.isCanceled(),"Empty controller removable");h.succeed();
    }
    @GameTest(template="industrial") public static void actualHoppersPastOneStack(GameTestHelper h){
        var b=build(h,false,Direction.NORTH);b.insertCoal(1000,false);var l=h.getLevel();var pos=b.getBlockPos().north();
        var s=Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING,Direction.SOUTH);l.setBlockAndUpdate(pos,s);
        var hopper=(net.minecraft.world.level.block.entity.HopperBlockEntity)l.getBlockEntity(pos);hopper.setItem(0,KilnContent.MINERAL_COAL.toStack(32));
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(l,pos,s,hopper);
        h.assertTrue(b.amount()==1001&&hopper.getItem(0).getCount()==31,"Real hopper inserts beyond a normal stack");
        l.removeBlock(pos,false);var below=b.getBlockPos().below();l.setBlockAndUpdate(below,Blocks.HOPPER.defaultBlockState());var out=(net.minecraft.world.level.block.entity.HopperBlockEntity)l.getBlockEntity(below);
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(l,below,l.getBlockState(below),out);h.assertTrue(b.amount()==1000&&out.getItem(0).getCount()==1,"Real hopper extracts exactly one");h.succeed();
    }
    @GameTest(template="industrial") public static void authorityRevocationAndFullInventory(GameTestHelper h){
        var b=build(h,false,Direction.NORTH);b.insertCoal(100,false);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(b.getBlockPos().getCenter());for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.DIRT,32));
        b.control(p,1);h.assertTrue(b.amount()==100,"Full inventory retains stock");
        var d=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=new CivicData.Address("minecraft:overworld",b.getBlockPos().south());c.owner=new CivicData.Owner(java.util.UUID.randomUUID(),false);c.radius=1;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=7200000;c.lastUpdate=System.currentTimeMillis();
        var io=b.items(Direction.NORTH);d.claims.put(c.at,c);d.rebuildIndex();try{h.assertTrue(!b.valid(p)&&!b.control(p,1),"Open menu loses authority immediately");h.assertTrue(io.extractItem(1,32,false).isEmpty(),"Cached automation cannot cross changed claim boundary");}finally{d.claims.remove(c.at);d.rebuildIndex();}h.succeed();
    }
    @GameTest(template="industrial") public static void localPipeRateAndPausedShell(GameTestHelper h){
        var b=build(h,true,Direction.NORTH);var at=b.getBlockPos();h.getLevel().setBlockAndUpdate(at.north(),IndustrialContent.PIPE.get().defaultBlockState());
        var target=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);b.fluids(Direction.NORTH).fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);
        int n=PipeRouting.transfer(h.getLevel(),at.north(2),Direction.SOUTH,target,false);h.assertTrue(n==25&&target.getFluidAmount()==25&&b.amount()==975,"Local pipe 25mB budget");h.assertTrue(PipeRouting.transfer(h.getLevel(),at.north(2),Direction.SOUTH,target,false)==0,"Shared pipe budget prevents double transfer");h.succeed();
    }

    @GameTest(template="industrial") public static void handAndShiftClickStoreCoalUntilFullOrShutOut(GameTestHelper h){
        var b=build(h,false,Direction.NORTH);var l=h.getLevel();var coal=KilnContent.MINERAL_COAL;
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"stoker");var p=new net.minecraft.server.level.ServerPlayer(l.getServer(),l,profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(l,profile).connection;p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(b.getBlockPos().getCenter());
        p.getData(Accessories.INVENTORY).setStackInSlot(0,Backpack.ITEM.toStack());Backpack.worn(p).setItem(0,coal.toStack(20));
        p.getInventory().setItem(9,coal.toStack(32));p.getInventory().setItem(10,new ItemStack(Items.APPLE,5));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coal.toStack(10));b.useHeld(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(b.amount()==10&&p.getMainHandItem().isEmpty(),"A right-click stores the held coal: bunker "+b.amount());
        // As the server's menu opening does: the Hand, then the pack.
        var menu=b.createMenu(1,p.getInventory(),p);BlockCarry.attachMenu(menu,p.getInventory());Backpack.attachMenu(menu,p);p.containerMenu=menu;int packed=-1;var quick=net.minecraft.world.inventory.ClickType.QUICK_MOVE;
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i) instanceof Backpack.PackSlot s&&s.getItem().is(coal.get())){packed=i;break;}
        h.assertTrue(menu instanceof BulkMenu&&packed>=0,"The bunker's screen shows the worn pack");
        menu.clicked(0,0,quick,p);menu.clicked(1,0,quick,p);
        h.assertTrue(b.amount()==42&&p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(10).getCount()==5,"Shift-click stores a pocket's coal and nothing else: bunker "+b.amount());
        menu.clicked(packed,0,quick,p);
        h.assertTrue(b.amount()==62&&Backpack.worn(p).isEmpty(),"Shift-click stores the pack's coal: bunker "+b.amount());
        b.insertCoal(BulkEntity.COAL_CAPACITY-62-5,false);p.getInventory().setItem(9,coal.toStack(32));menu.clicked(0,0,quick,p);
        h.assertTrue(b.amount()==BulkEntity.COAL_CAPACITY&&p.getInventory().getItem(9).getCount()==27,"A nearly full bunker takes what fits and the pocket keeps the rest: "+p.getInventory().getItem(9));
        var beside=b.getBlockPos().north();var state=Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING,Direction.SOUTH);l.setBlockAndUpdate(beside,state);
        var hopper=(net.minecraft.world.level.block.entity.HopperBlockEntity)l.getBlockEntity(beside);hopper.setItem(0,coal.toStack(5));
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(l,beside,state,hopper);
        h.assertTrue(b.amount()==BulkEntity.COAL_CAPACITY&&hopper.getItem(0).getCount()==5,"A hopper keeps the coal a full bunker cannot take");
        l.removeBlock(beside,false);b.extractCoal(100,false);
        var d=CivicData.get(l.getServer());var c=new CivicData.Claim();c.at=new CivicData.Address("minecraft:overworld",b.getBlockPos());c.owner=new CivicData.Owner(java.util.UUID.randomUUID(),false);c.radius=1;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=7200000;c.lastUpdate=System.currentTimeMillis();
        d.claims.put(c.at,c);d.rebuildIndex();
        try{menu.clicked(0,0,quick,p);h.assertTrue(b.amount()==BulkEntity.COAL_CAPACITY-100&&p.getInventory().getItem(9).getCount()==27,"A player shut out by a claim stores nothing through the open screen");}finally{d.claims.remove(c.at);d.rebuildIndex();}
        h.succeed();
    }
}



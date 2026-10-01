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
}



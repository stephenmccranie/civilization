package dev.civilization;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class IndustryGameTests {
    @GameTest(template="empty") public static void oldOilChunksGainPhysicalBodyAndSeepWithoutOverwritingBuilds(GameTestHelper h) {
        var level=h.getLevel();
        var top=h.absolutePos(new BlockPos(8,90,8));
        var site=new Deposits.Site(top.getX(),top.getY(),top.getZ(),Deposits.Kind.OIL,2);
        var oil=new BlockPos(site.x(),site.centerY(),site.z());
        var built=oil.east();
        level.setBlockAndUpdate(oil,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(built,Blocks.BRICKS.defaultBlockState());
        level.setBlockAndUpdate(top.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        var chunk=level.getChunkAt(oil);
        OilSeepRetrofit.process(level,chunk,site);
        h.assertTrue(level.getBlockState(oil).is(IndustrialContent.RESERVOIR_OIL.get()),"Natural underground stone becomes finite oil");
        h.assertTrue(level.getBlockState(built).is(Blocks.BRICKS),"Player construction is preserved");
        h.assertTrue(level.getBlockState(top.below()).is(IndustrialContent.OIL_SEEP.get())
                &&level.getBlockState(top).is(IndustrialContent.SURFACE_OIL.get()),"Surface marker and flowing source appear above the body");
        level.setBlockAndUpdate(oil,Blocks.AIR.defaultBlockState());
        OilSeepRetrofit.process(level,chunk,site);
        h.assertTrue(level.getBlockState(oil).isAir(),"Processed chunk never refills extracted oil");
        h.succeed();
    }

    @GameTest(template="empty",batch="derrick-construction") public static void modeledDerrickConstructionConservesMaterials(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(8,67,8));
        var player=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"derrick-builder"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(at.getX()+.5,at.getY()+1,at.getZ()-3);
        for(var front:Direction.Plane.HORIZONTAL){
            l.setBlockAndUpdate(at,IndustrialContent.PUMP.get().defaultBlockState().setValue(CivicBlock.FACING,front));var m=(IndustrialBlockEntity)l.getBlockEntity(at);
            for(var cell:ModeledDerrick.CELLS){var p=ModeledDerrick.position(at,front,cell);l.getChunkAt(p);l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());}
            var port=MachineStructure.position(at,front,ModeledDerrick.PORT);l.setBlockAndUpdate(port,Blocks.AIR.defaultBlockState());
            player.getInventory().clearContent();player.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,64));
            var held=new ItemStack(Items.OAK_PLANKS,5);
            h.assertTrue(!ModeledDerrick.build(m,player,held)&&held.getCount()==5&&player.getInventory().getItem(1).getCount()==64,"Never draw missing materials from another inventory slot");
            var blocked=ModeledDerrick.CELLS.stream().filter(c->c.pieces().containsKey(0)).findFirst().orElseThrow();var blockedAt=ModeledDerrick.position(at,front,blocked);l.setBlockAndUpdate(blockedAt,Blocks.DIAMOND_BLOCK.defaultBlockState());held=new ItemStack(Items.COBBLESTONE,7);
            h.assertTrue(!ModeledDerrick.build(m,player,held)&&held.getCount()==7&&m.derrickSections==0,"Obstruction preserves held stock and terrain");l.removeBlock(blockedAt,false);
            double labor=CalorieFoodData.of(player).laborSpent;
            h.assertTrue(ModeledDerrick.build(m,player,held)&&held.isEmpty()&&m.derrickSections==1&&!IndustrialStructure.bind(m),"Footings are visible and solid before completion");
            var partial=new IndustrialBlockEntity(at,m.getBlockState());partial.loadWithComponents(m.saveWithFullMetadata(l.registryAccess()),l.registryAccess());h.assertTrue(partial.derrickSections==1&&partial.derrickPaid.get(0).getFirst().is(Items.COBBLESTONE),"Partial construction and paid material identity survive save/load");
            for(var stock:List.of(new ItemStack(Items.OAK_PLANKS,60),new ItemStack(Items.BIRCH_PLANKS,60),new ItemStack(Items.OAK_PLANKS,29),new ItemStack(Items.IRON_BLOCK,8),IndustrialContent.PORT.toStack()))h.assertTrue(ModeledDerrick.build(m,player,stock)&&stock.isEmpty(),"Consume only the supplied stack");
            h.assertTrue(m.derrickSections==ModeledDerrick.ALL&&IndustrialStructure.bind(m)&&player.getInventory().getItem(1).getCount()==64,"Complete all rotations without touching inventory reserves");
            h.assertTrue(CalorieFoodData.of(player).laborSpent-labor>0&&CalorieFoodData.of(player).laborSpent-labor<500,"Charge material labor, not hidden cells");
            int dropped=derrickDrops(l,at);l.destroyBlock(port,true);
            h.assertTrue(l.getBlockEntity(at)==m&&Integer.bitCount(m.derrickSections)==24&&!IndustrialStructure.bind(m)&&derrickDrops(l,at)-dropped==1,"Port damage is local and refunds exactly one paid port");
            h.assertTrue(ModeledDerrick.build(m,player,IndustrialContent.PORT.toStack())&&IndustrialStructure.bind(m),"Repair port from held materials");
            dropped=derrickDrops(l,at);var cell=ModeledDerrick.CELLS.stream().filter(c->c.pieces().size()==1&&c.pieces().containsKey(1)).findFirst().orElseThrow();l.destroyBlock(ModeledDerrick.position(at,front,cell),true);
            h.assertTrue(l.getBlockEntity(at)==m&&Integer.bitCount(m.derrickSections)==24&&derrickDrops(l,at)-dropped==6,"A broken panel leaves the other sections standing");
            m.process();h.assertTrue(!m.formed&&Integer.bitCount(m.derrickSections)==24,"Incomplete pump stops without dismantling");
            h.assertTrue(ModeledDerrick.build(m,player,new ItemStack(Items.OAK_PLANKS,6))&&IndustrialStructure.bind(m),"Repair the missing frame panel");
            var copy=new IndustrialBlockEntity(at,m.getBlockState());copy.loadWithComponents(m.saveWithFullMetadata(l.registryAccess()),l.registryAccess());h.assertTrue(copy.derrickPaid.values().stream().flatMap(List::stream).mapToInt(ItemStack::getCount).sum()==165,"Exact per-section refund ledger survives reload");
            dropped=derrickDrops(l,at);l.removeBlock(at,false);h.assertTrue(derrickDrops(l,at)-dropped==165,"Controller removal refunds remaining materials exactly once");
            h.assertTrue(ModeledDerrick.CELLS.stream().allMatch(c->l.getBlockState(ModeledDerrick.position(at,front,c)).isAir())&&l.getBlockState(port).isAir(),"Controller removal clears owned physical cells");
        }h.succeed();
    }
    private static int derrickDrops(net.minecraft.server.level.ServerLevel l,BlockPos at){return l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(3)).stream().filter(e->!e.getItem().is(IndustrialContent.PUMP.asItem())).mapToInt(e->e.getItem().getCount()).sum();}
    @GameTest(template="empty",batch="derrick-construction") public static void derrickCollisionThroughChunkGetter(GameTestHelper h){
        var m=machine(h,IndustrialContent.PUMP.get(),8);var l=h.getLevel();
        var cell=ModeledDerrick.CELLS.stream().filter(c->c.y()==15&&c.pieces().containsKey(21)).findFirst().orElseThrow();var at=ModeledDerrick.position(m.getBlockPos(),m.front(),cell);var state=l.getBlockState(at);
        h.assertTrue(!state.getCollisionShape(l.getChunkAt(at),at).isEmpty(),"Collision getter uses a chunk, and still resolves controller ownership");h.succeed();
    }
    @GameTest(template="empty",batch="derrick-construction") public static void derrickCornerCollisionReusesExactShapes(GameTestHelper h){
        var cell=ModeledDerrick.CELLS.stream().filter(c->c.y()==15&&c.pieces().containsKey(21)).max(java.util.Comparator.comparingInt(c->c.pieces().size())).orElseThrow();
        int bits=0;for(int part:cell.pieces().keySet())bits|=1<<part;
        for(var front:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int mask:new int[]{0,ModeledDerrick.ALL,ModeledDerrick.ALL^(1<<21),bits&~Integer.lowestOneBit(bits)}){
            var expected=net.minecraft.world.phys.shapes.Shapes.empty();
            for(var entry:cell.pieces().entrySet())if((mask&(1<<entry.getKey()))!=0)expected=net.minecraft.world.phys.shapes.Shapes.or(expected,entry.getValue()[front.get2DDataValue()]);
            var shape=cell.shape(front,mask);
            h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(expected,shape,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Cached corner collision exactly matches section geometry");
            for(int i=0;i<100;i++)h.assertTrue(cell.shape(front,mask)==shape&&cell.shape(front,mask^(1<<24))==shape,"Repeated movement and unrelated section changes reuse the same local shape");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void stoneToolsRecoverControllers(GameTestHelper h){
        var pick=new ItemStack(Items.STONE_PICKAXE).get(net.minecraft.core.component.DataComponents.TOOL);
        for(var block:List.of(KilnContent.KILN.get(),KilnContent.RETORT.get(),CookingContent.STATION.get(),KilnContent.FOUNDRY.get(),
                WorkshopContent.TANNERY.get(),WorkshopContent.TEXTILE.get(),WorkshopContent.SMITHY.get(),CivicContent.LAND.get(),
                AirshipContent.CONTROLLER.get(),IndustrialContent.PUMP.get(),IndustrialContent.REFINERY.get(),IndustrialContent.DRILL.get(),
                IndustrialContent.TANK.get(),IndustrialContent.COLUMN.get(),IndustrialContent.CONDENSER.get(),OilEngineContent.ENGINE.get(),
                BulkContent.BUNKER.get(),BulkContent.TANK.get()))
            h.assertTrue(pick.isCorrectForDrops(block.defaultBlockState()),"Stone pickaxe recovers "+block.getDescriptionId());
        var axe=new ItemStack(Items.STONE_AXE).get(net.minecraft.core.component.DataComponents.TOOL);
        for(var block:List.of(CivicContent.SHOP.get(),CivicContent.TABLE.get(),CivicContent.TABLE_PART.get(),BoatContent.HELM.get()))
            h.assertTrue(axe.isCorrectForDrops(block.defaultBlockState()),"Stone axe recovers "+block.getDescriptionId());
        var pos=h.absolutePos(new BlockPos(3,1,3));var level=h.getLevel();level.setBlockAndUpdate(pos,WorkshopContent.SMITHY.get().defaultBlockState());
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"stone-miner"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_PICKAXE));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        h.assertTrue(player.gameMode.destroyBlock(pos),"Stone pickaxe breaks Smithy in survival");
        h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).stream().anyMatch(e->e.getItem().is(WorkshopContent.SMITHY.asItem())),"Smithy controller drops as an item");
        h.succeed();
    }
    private static IndustrialBlockEntity machine(GameTestHelper h,IndustrialBlock block,int x){
        var at=h.absolutePos(new BlockPos(x,67,2));h.getLevel().removeBlock(at,false);h.getLevel().setBlockAndUpdate(at,block.defaultBlockState());
        var m=(IndustrialBlockEntity)h.getLevel().getBlockEntity(at);
        if(m.kind==IndustrialBlock.Kind.PUMP)DerrickFixture.assemble(m);else for(var part:IndustrialStructure.parts(m.kind))MachineStructure.placePart(h.getLevel(),at,m.front(),part);return m;
    }
    private static void bind(IndustrialBlockEntity m,Deposits.Site s){try{var f=IndustrialBlockEntity.class.getDeclaredField("site");f.setAccessible(true);f.set(m,s);f=IndustrialBlockEntity.class.getDeclaredField("surveyed");f.setAccessible(true);f.setBoolean(m,true);}catch(Exception e){throw new RuntimeException(e);}}
    private static Deposits.Site site(IndustrialBlockEntity m,Deposits.Kind k,int count){
        var p=m.getBlockPos();var site=new Deposits.Site(p.getX(),p.getY(),p.getZ(),k,2);
        for(int i=0;i<site.cells();i++){var cell=site.cell(i);if(!site.body(cell))continue;m.getLevel().setBlockAndUpdate(cell,count-->0?(k==Deposits.Kind.OIL?IndustrialContent.RESERVOIR_OIL.get():IndustrialContent.COAL_SEAM.get()).defaultBlockState():Blocks.AIR.defaultBlockState());}
        return site;
    }
    private static int stock(GameTestHelper h,Deposits.Site s){int total=0;for(int i=0;i<s.cells();i++){var p=s.cell(i);if(!s.body(p))continue;var state=h.getLevel().getBlockState(p);if(state.is(IndustrialContent.COAL_SEAM.get()))total+=16;else if(state.is(IndustrialContent.RESERVOIR_OIL.get()))total+=(8-state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL))*125;}return total;}
    private static void run(IndustrialBlockEntity m,int steps){CoalFireFixture.light(m);for(int i=0;i<steps;i++)m.process();}
    @GameTest(template="empty") public static void industrialBanksUseLastFuelAndAllOutputs(GameTestHelper h){
        var col=machine(h,IndustrialContent.COLUMN.get(),3);col.input.fill(new FluidStack(IndustrialContent.HEATED.get(),4000),EXECUTE);
        for(int slot:MachineInventory.INDUSTRIAL_OUTPUT)col.setItem(slot,IndustrialContent.SULFUR.toStack(31));run(col,160);
        var copy=new IndustrialBlockEntity(col.getBlockPos(),col.getBlockState());copy.loadWithComponents(col.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.input.isEmpty()&&copy.output.getFluidAmount()==3200&&copy.lubricant.getFluidAmount()==800,"Four coproduct batches conserve fluid");
        for(int slot:MachineInventory.INDUSTRIAL_OUTPUT)h.assertTrue(copy.getItem(slot).getCount()==32&&copy.canTakeItemThroughFace(slot,copy.getItem(slot),Direction.DOWN),"Every output is used, saved and hopper-accessible");
        var heat=machine(h,IndustrialContent.REFINERY.get(),14);heat.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),250),EXECUTE);heat.setItem(2,KilnContent.MINERAL_COAL.toStack());run(heat,10);
        h.assertTrue(heat.output.getFluidAmount()==250&&heat.getItem(2).isEmpty()&&heat.getItem(0).isEmpty(),"Heater draws fuel from second slot without duplication");h.succeed();
    }
    @GameTest(template="empty") public static void competingDrillsConsumePhysicalBlocksOnce(GameTestHelper h){
        var drill=machine(h,IndustrialContent.DRILL.get(),3);var s=site(drill,Deposits.Kind.COAL,1);var a=new DepositWork();var b=new DepositWork();
        var first=a.find(h.getLevel(),s,drill.getBlockPos());var stale=b.find(h.getLevel(),s,drill.getBlockPos());
        h.assertTrue(a.extract(h.getLevel(),s,drill.getBlockPos(),first)&&!b.extract(h.getLevel(),s,drill.getBlockPos(),stale),"Two drill attempts cannot collect the same coal block");
        h.assertTrue(stock(h,s)==0&&h.getLevel().getBlockState(first.pos()).isAir(),"Extraction leaves a physical hole");
        h.succeed();
    }
    @GameTest(template="empty") public static void coalBootstrapsOilAndIndustrialCoal(GameTestHelper h){
        var pump=machine(h,IndustrialContent.PUMP.get(),2);var oil=site(pump,Deposits.Kind.OIL,1);bind(pump,oil);pump.setItem(0,KilnContent.MINERAL_COAL.toStack(4));run(pump,100);
        h.assertTrue(pump.output.getFluidAmount()==1000&&pump.getItem(0).isEmpty()&&pump.heat==0&&stock(h,oil)==0,"Four coal pump precisely one physical 1,000 mB block");
        var ref=machine(h,IndustrialContent.REFINERY.get(),7);ref.input.fill(pump.output.drain(1000,EXECUTE),EXECUTE);ref.setItem(0,KilnContent.MINERAL_COAL.toStack(4));run(ref,40);
        h.assertTrue(ref.input.isEmpty()&&ref.output.getFluidAmount()==1000&&ref.getItem(0).isEmpty()&&ref.heat==0,"Four coal heat 1,000 crude");
        var column=machine(h,IndustrialContent.COLUMN.get(),24);column.input.fill(ref.output.drain(1000,EXECUTE),EXECUTE);run(column,40);
        h.assertTrue(column.output.getFluidAmount()==800&&column.lubricant.getFluidAmount()==200&&column.getItem(1).is(IndustrialContent.SULFUR.get()),"Column separates vapor, lubricating oil and sulfur");
        var condenser=machine(h,IndustrialContent.CONDENSER.get(),34);condenser.input.fill(column.output.drain(800,EXECUTE),EXECUTE);run(condenser,40);
        h.assertTrue(condenser.output.getFluidAmount()==800,"Condenser produces engine fuel");
        var drill=machine(h,IndustrialContent.DRILL.get(),12);var seam=site(drill,Deposits.Kind.COAL,8);bind(drill,seam);drill.input.fill(condenser.output.drain(800,EXECUTE),EXECUTE);drill.lubricant.fill(column.lubricant.drain(200,EXECUTE),EXECUTE);
        int produced=0;for(int i=0;i<8;i++){run(drill,10);produced+=drill.removeItem(1,32).getCount();}
        h.assertTrue(produced==128&&drill.input.getFluidAmount()==160&&stock(h,seam)==0,"640 fuel removes eight blocks and produces 128 coal");h.succeed();
    }
    @GameTest(template="empty") public static void blockedOrBrokenMachineSpendsNothing(GameTestHelper h){
        var pump=machine(h,IndustrialContent.PUMP.get(),2);var s=site(pump,Deposits.Kind.OIL,1);bind(pump,s);pump.setItem(0,KilnContent.MINERAL_COAL.toStack(2));pump.output.fill(new FluidStack(IndustrialContent.CRUDE.get(),4000),EXECUTE);run(pump,12);
        h.assertTrue(pump.getItem(0).getCount()==1&&pump.heat==243&&stock(h,s)==1000&&pump.status==4,"Full output preserves oil while burning idle coal");
        pump.output.drain(4000,EXECUTE);var frame=IndustrialStructure.parts(pump.kind).stream().filter(p->!p.material().equals("air")).findFirst().orElseThrow();
        var area=new net.minecraft.world.phys.AABB(pump.getBlockPos()).inflate(3);
        int coal=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(KilnContent.MINERAL_COAL.get())).mapToInt(e->e.getItem().getCount()).sum();
        h.getLevel().removeBlock(MachineStructure.position(pump.getBlockPos(),pump.front(),frame),false);
        int dropped=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(KilnContent.MINERAL_COAL.get())).mapToInt(e->e.getItem().getCount()).sum();
        run(pump,2);h.assertTrue(h.getLevel().getBlockEntity(pump.getBlockPos())==pump&&!pump.formed&&dropped==coal&&pump.getItem(0).getCount()==1&&stock(h,s)==1000,"Local damage stops production without consuming coal or oil or dropping inventory");h.succeed();
    }
    @GameTest(template="empty") public static void machineSaveReloadAndRebuildDoNotRefillDeposit(GameTestHelper h){
        var pump=machine(h,IndustrialContent.PUMP.get(),2);var s=site(pump,Deposits.Kind.OIL,1);bind(pump,s);pump.setItem(0,KilnContent.MINERAL_COAL.toStack());run(pump,5);
        h.assertTrue(stock(h,s)==875,"First completed batch lowers actual fluid height");
        var copy=new IndustrialBlockEntity(pump.getBlockPos(),pump.getBlockState());copy.loadWithComponents(pump.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.setLevel(h.getLevel());h.getLevel().setBlockEntity(copy);bind(copy,s);
        h.assertTrue(copy.heat==125&&copy.output.getFluidAmount()==125,"Part-used coal and extracted liquid persist");
        copy.setItem(0,KilnContent.MINERAL_COAL.toStack(3));run(copy,100);h.assertTrue(copy.output.getFluidAmount()==1000&&stock(h,s)==0,"Restored cursor extracts only remaining physical oil");
        h.getLevel().removeBlock(pump.getBlockPos(),false);var rebuilt=machine(h,IndustrialContent.PUMP.get(),2);bind(rebuilt,s);rebuilt.setItem(0,KilnContent.MINERAL_COAL.toStack());run(rebuilt,100);
        h.assertTrue(rebuilt.status==3&&rebuilt.getItem(0).isEmpty()&&rebuilt.output.isEmpty(),"Rebuilding cannot refresh an empty reservoir");h.succeed();
    }
    @GameTest(template="empty") public static void oilLowersAtTheWellBeforeMovingOutward(GameTestHelper h){
        var pump=machine(h,IndustrialContent.PUMP.get(),2);var s=site(pump,Deposits.Kind.OIL,0);var center=new BlockPos(s.x(),s.centerY(),s.z());
        for(var p:java.util.List.of(center.above(),center.above().east(),center))h.getLevel().setBlockAndUpdate(p,IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());
        var work=new DepositWork();for(int i=0;i<16;i++){DepositWork.Target t;do{t=work.find(h.getLevel(),s,pump.getBlockPos());}while(t.status()==10);h.assertTrue(t.status()==7&&t.pos().equals(i<8?center.above():center),"The well column drains from its top downward");h.assertTrue(work.extract(h.getLevel(),s,pump.getBlockPos(),t),"Physical drain commits");}
        h.assertTrue(h.getLevel().getBlockState(center.above()).isAir()&&h.getLevel().getBlockState(center).isAir()&&h.getLevel().getBlockState(center.above().east()).is(IndustrialContent.RESERVOIR_OIL.get()),"Nearby oil remains physical and unspent");h.succeed();
    }
    @GameTest(template="empty") public static void extractionRespectsResourceClaim(GameTestHelper h){
        var drill=machine(h,IndustrialContent.DRILL.get(),3);var s=site(drill,Deposits.Kind.COAL,1);var work=new DepositWork();var t=work.find(h.getLevel(),s,drill.getBlockPos());
        var d=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=CivicService.address(h.getLevel(),t.pos());c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=1;c.height=4;c.coalUnit=3600000;c.energy=3600000;c.lastUpdate=System.currentTimeMillis();d.claims.put(c.at,c);d.rebuildIndex();
        try{h.assertTrue(!work.extract(h.getLevel(),s,drill.getBlockPos(),t)&&stock(h,s)==16,"New claim prevents even a previously selected target from being mined");h.assertTrue(work.find(h.getLevel(),s,drill.getBlockPos()).status()==9,"Machine explains resource protection");}finally{d.claims.remove(c.at);d.rebuildIndex();}h.succeed();
    }
    @GameTest(template="empty") public static void physicalSearchIsBoundedAndNeverLoadsChunks(GameTestHelper h){
        var s=new Deposits.Site(4000000,80,4000000,Deposits.Kind.OIL,12);var w=new DepositWork();var pos=h.absolutePos(new BlockPos(2,3,2));
        h.assertTrue(w.find(h.getLevel(),s,pos).status()==MachineStatus.Industry.SEARCHING&&w.cursor<=1024&&h.getLevel().getChunkSource().getChunkNow(s.x()>>4,s.z()>>4)==null,"Unloaded slices are skipped without force loading");
        var drill=machine(h,IndustrialContent.DRILL.get(),3);var small=site(drill,Deposits.Kind.OIL,0);var scan=new DepositWork();scan.find(h.getLevel(),small,drill.getBlockPos());h.assertTrue(scan.cursor<=1024,"One scan has a fixed work budget");h.succeed();
    }
    @GameTest(template="empty") public static void wideDepositUsesNearbyLoadedStock(GameTestHelper h){
        var pump=machine(h,IndustrialContent.PUMP.get(),2);
        var site=new Deposits.Site(pump.getBlockPos().getX(),pump.getBlockPos().getY(),pump.getBlockPos().getZ(),Deposits.Kind.OIL,70);
        var oil=new BlockPos(site.x(),site.top(),site.z());
        h.getLevel().setBlockAndUpdate(oil,IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());
        var work=new DepositWork();var target=work.find(h.getLevel(),site,pump.getBlockPos());
        h.assertTrue(target.status()==MachineStatus.Industry.WORKING&&target.pos().equals(oil),"A derrick finds physical stock in the local part of a wide field");
        h.assertTrue(work.extract(h.getLevel(),site,pump.getBlockPos(),target),"Nearby stock drains without loading the whole field");h.succeed();
    }
    @GameTest(template="empty") public static void wideDepositGeometryStaysThinAndScannable(GameTestHelper h){
        int coalBlocks=0,oilBlocks=0;
        for(var kind:Deposits.Kind.values()){
            var site=new Deposits.Site(128,100,128,kind,kind==Deposits.Kind.COAL?75:35);
            var columns=new java.util.HashSet<Long>();int height=site.ceiling()-site.bottom()+1;
            for(int n=0;n<site.width()*site.width();n++)
                h.assertTrue(columns.add(site.cell(n*height).asLong()),"Each scan column is unique");
            int stock=0;
            for(int n=0;n<site.cells();n++){
                var cell=site.cell(n);
                if(site.body(cell)&&(kind==Deposits.Kind.COAL||cell.getY()<=site.top()))stock++;
            }
            if(kind==Deposits.Kind.OIL)oilBlocks=stock;else coalBlocks=stock;
            h.assertTrue(kind==Deposits.Kind.OIL?stock>7000&&stock<12000:stock>20000&&stock<60000,
                    kind+" field has an unexpected physical reserve: "+stock);
            h.assertTrue(site.workCell(0).equals(new BlockPos(site.x(),site.ceiling(),site.z())),"Search starts at field center");
        }
        double fieldsAfterTransport=oilBlocks*1000.0*.8*.8/IndustrialRates.DRILL_FUEL_MB/coalBlocks;
        h.assertTrue(fieldsAfterTransport>=1.5&&fieldsAfterTransport<=2.8,
                "One oil field should power roughly two coal fields after a 20% transport reserve: "+fieldsAfterTransport);
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40) public static void oilDoesNotSpreadRefillOrAllowBuckets(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(2,3,2));var block=IndustrialContent.RESERVOIR_OIL.get();h.getLevel().setBlockAndUpdate(p,block.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,4));
        h.assertTrue(block.pickupBlock(null,h.getLevel(),p,h.getLevel().getBlockState(p)).isEmpty(),"Bucket cannot bypass industrial extraction");
        h.getLevel().getFluidState(p).tick(h.getLevel(),p);
        h.runAfterDelay(10,()->{h.assertTrue(h.getLevel().getBlockState(p).is(block)&&h.getLevel().getBlockState(p).getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL)==4&&!h.getLevel().getBlockState(p.below()).is(block),"Finite fluid height neither flows nor regenerates");h.succeed();});
    }
    // The 13x6x13 template reserves the whole bed and its flow margin, unlike empty (5x3x5).
    private static BlockPos surfaceOilBed(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(6,4,6));
        h.assertTrue(h.getBounds().contains(Vec3.atCenterOf(source.offset(-3,-1,-3)))
                &&h.getBounds().contains(Vec3.atCenterOf(source.offset(3,0,3))),"Oil flow bed stays inside the reserved test area");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)
            h.getLevel().setBlockAndUpdate(source.offset(x,-1,z),Blocks.DIRT.defaultBlockState());
        return source;
    }
    @GameTest(template="surface_oil",timeoutTicks=90) public static void surfaceOilFlowsAndDerricksUseTheColumn(GameTestHelper h){
        var source=surfaceOilBed(h);
        h.getLevel().setBlockAndUpdate(source,IndustrialContent.SURFACE_OIL.get().defaultBlockState());
        var site=new Deposits.Site(source.getX(),source.getY(),source.getZ(),Deposits.Kind.OIL,3);
        h.assertTrue(site.contains(source.above(50))&&site.contains(source.east(3).above(20))
                &&!site.contains(source.east(4))&&!site.contains(source.below(40)),
                "Oil derricks can stand anywhere above the reservoir's actual footprint");
        h.runAfterDelay(65,()->{
            h.assertTrue(h.getLevel().getBlockState(source).is(IndustrialContent.SURFACE_OIL.get())
                    &&h.getLevel().getBlockState(source.east()).is(IndustrialContent.SURFACE_OIL.get()),
                    "One surface source slowly flows across adjacent ground");
            h.succeed();
        });
    }
    @GameTest(template="surface_oil",timeoutTicks=90) public static void generatedSurfaceOilStartsWithoutNeighborChange(GameTestHelper h){
        var source=surfaceOilBed(h);
        h.getLevel().setBlockAndUpdate(source.east(),Blocks.SHORT_GRASS.defaultBlockState());
        h.runAfterDelay(1,()->h.assertTrue(h.getLevel().getBlockState(source.east()).is(Blocks.SHORT_GRASS),"Grass survives on the bed until the scheduled oil flow"));
        h.getLevel().setBlock(source,IndustrialContent.SURFACE_OIL.get().defaultBlockState(),2);
        SurfaceOilFlow.activate(h.getLevel(),source);
        h.assertTrue(h.getLevel().getFluidTicks().hasScheduledTick(source,IndustrialContent.CRUDE.get()),
                "Worldgen's source has its first fluid tick queued without a block update");
        h.runAfterDelay(65,()->{
            h.assertTrue(h.getLevel().getBlockState(source.east()).is(IndustrialContent.SURFACE_OIL.get()),
                    "A newly placed seep displaces adjacent grass without player action");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=40) public static void crudeOilResistsSwimming(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(3,2,3));
        h.getLevel().setBlockAndUpdate(source.below(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(source,IndustrialContent.SURFACE_OIL.get().defaultBlockState());
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"oil-swimmer"));
        player.setPos(source.getX()+.5,source.getY()+.1,source.getZ()+.5);
        h.getLevel().addFreshEntity(player);
        h.runAfterDelay(4,()->{
            player.setDeltaMovement(1,1,1);
            OilMovement.slow(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
            var motion=player.getDeltaMovement();
            h.assertTrue(motion.x<.7&&motion.y<.85&&motion.z<.7,"Crude resists horizontal and upward swimming");
            player.discard();h.succeed();
        });
    }
    @GameTest(template="empty") public static void canistersAreExactAndLiquidsCannotMix(GameTestHelper h){
        var tank=machine(h,IndustrialContent.TANK.get(),2);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"canister-test"));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IndustrialContent.CRUDE_CAN.toStack());tank.canister(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(tank.input.getFluidAmount()==1000&&p.getMainHandItem().is(IndustrialContent.CAN.get()),"Full canister deposits exactly 1,000 mB and returns empty");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IndustrialContent.FUEL_CAN.toStack());tank.canister(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(tank.input.getFluidAmount()==1000&&p.getMainHandItem().is(IndustrialContent.FUEL_CAN.get()),"Wrong liquid leaves tank and held item unchanged");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IndustrialContent.CAN.toStack());tank.canister(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(tank.input.isEmpty()&&p.getMainHandItem().is(IndustrialContent.CRUDE_CAN.get()),"Withdrawing conserves liquid");
        tank.input.fill(new FluidStack(IndustrialContent.FUEL.get(),15500),EXECUTE);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IndustrialContent.FUEL_CAN.toStack());tank.canister(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(tank.input.getFluidAmount()==15500&&p.getMainHandItem().is(IndustrialContent.FUEL_CAN.get()),"Insufficient room never partially empties a canister");h.succeed();
    }
    @GameTest(template="empty") public static void transfersRespectFacingAndClaims(GameTestHelper h){
        var ref=machine(h,IndustrialContent.DRILL.get(),3);var behind=ref.getBlockPos().relative(ref.front().getOpposite());h.getLevel().setBlockAndUpdate(behind,IndustrialContent.TANK.get().defaultBlockState());var tank=(IndustrialBlockEntity)h.getLevel().getBlockEntity(behind);tank.input.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);ref.transfer();
        h.assertTrue(ref.input.getFluidAmount()==25&&tank.input.getFluidAmount()==975,"Adjacent tank feeds intake without loss");
        var port=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,behind,ref.front());h.assertTrue(port!=null&&port.drain(100,SIMULATE).getAmount()==100,"Native fluid port exposes stored fluid without consuming on simulation");
        var d=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();claim.at=CivicService.address(h.getLevel(),behind.relative(ref.front().getOpposite()));claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=1;claim.height=2;claim.coalUnit=3600000;claim.energy=3600000;claim.lastUpdate=System.currentTimeMillis();d.claims.put(claim.at,claim);d.rebuildIndex();
        try{h.assertTrue(port.drain(100,EXECUTE).isEmpty(),"Cached fluid capability respects a newly created claim");ref.transfer();h.assertTrue(ref.input.getFluidAmount()==25&&tank.input.getFluidAmount()==975,"Separate ownership blocks fluid transfer");}finally{d.claims.remove(claim.at);d.rebuildIndex();}h.succeed();
    }
    @GameTest(template="empty") public static void fuelGatesAndUnmineableClues(GameTestHelper h){
        var ref=machine(h,IndustrialContent.REFINERY.get(),2);var drill=machine(h,IndustrialContent.DRILL.get(),7);
        h.assertTrue(!ref.canPlaceItem(0,new ItemStack(Items.CHARCOAL))&&!drill.canPlaceItem(0,KilnContent.MINERAL_COAL.toStack()),"No biomass and no direct coal-powered drill");
        h.assertTrue(ref.input.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE)==0&&drill.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),1000),EXECUTE)==0,"Crude cannot bypass refining");
        h.assertTrue(IndustrialContent.COAL_SEAM.get().defaultBlockState().getDestroySpeed(h.getLevel(),drill.getBlockPos())<0&&Blocks.COAL_ORE.defaultBlockState().getDestroySpeed(h.getLevel(),drill.getBlockPos())>0,"Large seams are machine-only while ordinary ore stays mineable");h.succeed();
    }
    @GameTest(template="empty") public static void industrialStructuresRotateWithCutSlabs(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(3,3,3));for(var block:java.util.List.of(IndustrialContent.PUMP.get(),IndustrialContent.REFINERY.get(),IndustrialContent.DRILL.get(),IndustrialContent.COLUMN.get(),IndustrialContent.CONDENSER.get()))for(var facing:Direction.Plane.HORIZONTAL){
            h.getLevel().setBlockAndUpdate(at,block.defaultBlockState().setValue(CivicBlock.FACING,facing));if(block.kind==IndustrialBlock.Kind.PUMP)DerrickFixture.assemble((IndustrialBlockEntity)h.getLevel().getBlockEntity(at));else for(var part:IndustrialStructure.parts(block.kind))MachineStructure.placePart(h.getLevel(),at,facing,part);
            h.assertTrue(IndustrialStructure.complete(h.getLevel(),at,facing,block.kind),"Shaped frame recognizes all rotations");if(block.kind==IndustrialBlock.Kind.PUMP)h.getLevel().removeBlock(at,false);else for(var part:IndustrialStructure.parts(block.kind))h.getLevel().removeBlock(MachineStructure.position(at,facing,part),false);
        }h.succeed();
    }

    private static BlockPos port(IndustrialBlockEntity m,String role){return IndustrialStructure.parts(m.kind).stream().filter(p->p.material().equals(role)).map(p->MachineStructure.position(m.getBlockPos(),m.front(),p)).findFirst().orElseThrow();}
    private static void pipe(GameTestHelper h,BlockPos p){h.getLevel().setBlockAndUpdate(p,IndustrialContent.PIPE.get().defaultBlockState());}
    @GameTest(template="empty",timeoutTicks=460,batch="industry-pipeline") public static void onePumpSustainsOnePrimedRefineryLine(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(0,67,0));
        // This 60-block pipeline extends beyond the tiny empty template's ticking area.
        for(int cx=(origin.getX()-5)>>4;cx<=(origin.getX()+64)>>4;cx++)for(int cz=(origin.getZ()-1)>>4;cz<=(origin.getZ()+12)>>4;cz++)l.setChunkForced(cx,cz,true);
        h.runAfterDelay(20,()->{var pump=machine(h,IndustrialContent.PUMP.get(),15);
        var oil=site(pump,Deposits.Kind.OIL,1000);bind(pump,oil);int initialStock=stock(h,oil);
        var heater=machine(h,IndustrialContent.REFINERY.get(),24);
        var column=machine(h,IndustrialContent.COLUMN.get(),38);
        var condenser=machine(h,IndustrialContent.CONDENSER.get(),52);
        // The derrick's crude port is beside its ground-level controller.
        for(int x=17;x<=21;x++)pipe(h,h.absolutePos(new BlockPos(x,67,2)));
        pipe(h,h.absolutePos(new BlockPos(21,68,2)));
        pipe(h,h.absolutePos(new BlockPos(21,68,3)));
        for(int x=27;x<=35;x++)pipe(h,h.absolutePos(new BlockPos(x,68,4)));
        for(int x=41;x<=48;x++)pipe(h,h.absolutePos(new BlockPos(x,76,4)));
        for(int y=69;y<76;y++)pipe(h,h.absolutePos(new BlockPos(48,y,4)));
        pipe(h,h.absolutePos(new BlockPos(56,69,4)));
        var tankPos=h.absolutePos(new BlockPos(57,69,4));l.setBlockAndUpdate(tankPos,IndustrialContent.TANK.get().defaultBlockState());
        var tank=(IndustrialBlockEntity)l.getBlockEntity(tankPos);
        // Let newly placed block-entity tickers/ports settle before the fixed 400-tick measurement.
        h.runAfterDelay(20,()->{
        for(var m:java.util.List.of(pump,heater,column,condenser))
            h.assertTrue(l.isPositionEntityTicking(m.getBlockPos())&&m.formed&&m.progress==0,"Every stage is loaded and idle before priming: "+m.kind);
        pump.setItem(0,KilnContent.MINERAL_COAL.toStack(32));heater.setItem(0,KilnContent.MINERAL_COAL.toStack(32));
        CoalFireFixture.light(pump);CoalFireFixture.light(heater);
        // Startup inventory isolates sustained throughput from first-fill latency.
        pump.output.fill(new FluidStack(IndustrialContent.CRUDE.get(),125),EXECUTE);
        heater.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),500),EXECUTE);
        heater.output.fill(new FluidStack(IndustrialContent.HEATED.get(),250),EXECUTE);
        column.input.fill(new FluidStack(IndustrialContent.HEATED.get(),2000),EXECUTE);
        column.output.fill(new FluidStack(IndustrialContent.VAPOR.get(),800),EXECUTE);
        condenser.input.fill(new FluidStack(IndustrialContent.VAPOR.get(),400),EXECUTE);
        h.runAfterDelay(400,()->{
            h.assertTrue(initialStock-stock(h,oil)==1000,"Twenty seconds removes precisely 1,000 mB physical crude");
            h.assertTrue(heater.input.getFluidAmount()==500&&column.input.getFluidAmount()==2000,"Matched upstream stages hold stable input inventories: "+heater.input.getFluidAmount()+", "+column.input.getFluidAmount());
            h.assertTrue(column.lubricant.getFluidAmount()==200&&column.getItem(1).getCount()==1,"One column batch yields 200 mB oil and one sulfur: "+column.lubricant.getFluidAmount()+", sulfur="+column.getItem(1)+", progress="+column.progress);
            h.assertTrue(condenser.output.getFluidAmount()+tank.input.getFluidAmount()==800,"One condenser delivers 800 mB fuel per twenty seconds: "+condenser.output.getFluidAmount()+" + "+tank.input.getFluidAmount()+", status="+condenser.status+", progress="+condenser.progress);
            for(var m:java.util.List.of(pump,heater,column,condenser))h.assertTrue(m.status==7&&m.progress==0,"Every primed stage completes its matched batches without starving: "+m.kind);
            for(int cx=(origin.getX()-5)>>4;cx<=(origin.getX()+64)>>4;cx++)for(int cz=(origin.getZ()-1)>>4;cz<=(origin.getZ()+12)>>4;cz++)l.setChunkForced(cx,cz,false);
            h.succeed();
        });
        });
        });
    }
    @GameTest(template="empty") public static void pipeCapacitySharedAcrossPushPullAndBranches(GameTestHelper h){
        var l=h.getLevel();var start=h.absolutePos(new BlockPos(2,167,2));
        l.setBlockAndUpdate(start,IndustrialContent.TANK.get().defaultBlockState());
        var source=(IndustrialBlockEntity)l.getBlockEntity(start);source.input.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);
        var junction=start.east();pipe(h,junction);
        var end=junction.east();l.setBlockAndUpdate(end,IndustrialContent.TANK.get().defaultBlockState());
        var target=(IndustrialBlockEntity)l.getBlockEntity(end);
        var branch=junction.south();l.setBlockAndUpdate(branch,IndustrialContent.TANK.get().defaultBlockState());
        var other=(IndustrialBlockEntity)l.getBlockEntity(branch);
        h.assertTrue(PipeRouting.transfer(l,start,Direction.EAST,source.input,true)==25,"One shared pipe permits 25 mB");
        h.assertTrue(PipeRouting.transfer(l,end,Direction.WEST,target.input,false)==0&&PipeRouting.transfer(l,branch,Direction.NORTH,other.input,false)==0,"Pull and branch cannot multiply the shared capacity");
        h.assertTrue(source.input.getFluidAmount()==975&&target.input.getFluidAmount()+other.input.getFluidAmount()==25,"Branches conserve all fluid");
        h.runAfterDelay(10,()->{
            h.assertTrue(PipeRouting.transfer(l,start,Direction.EAST,source.input,true)==25,"Budget renews next work step without accumulating idle credit");
            h.assertTrue(source.input.getFluidAmount()==950&&target.input.getFluidAmount()+other.input.getFluidAmount()==50,"Two steps deliver exactly 50 mB total");h.succeed();
        });
    }
    @GameTest(template="empty") public static void refineryPipesConserveWithoutValves(GameTestHelper h){
        var ref=machine(h,IndustrialContent.REFINERY.get(),3);IndustrialStructure.bind(ref);var at=port(ref,"input_port");var mid=at.west();var source=mid.west().south();
        pipe(h,mid);pipe(h,mid.west());pipe(h,mid.north());h.getLevel().setBlockAndUpdate(source,IndustrialContent.TANK.get().defaultBlockState());var tank=(IndustrialBlockEntity)h.getLevel().getBlockEntity(source);tank.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),1000),EXECUTE);
        ref.transfer();h.assertTrue(ref.input.getFluidAmount()==25&&tank.input.getFluidAmount()==975,"Bent pipes transfer exactly 25 mB per step");
        h.getLevel().setBlockAndUpdate(mid,h.getLevel().getBlockState(mid).setValue(FluidPipeBlock.OPEN,false));ref.transfer();h.assertTrue(tank.input.getFluidAmount()==975,"Changing legacy valve state cannot reset the shared pipe budget");
        h.getLevel().setBlockAndUpdate(mid,h.getLevel().getBlockState(mid).setValue(FluidPipeBlock.OPEN,true));h.getLevel().removeBlock(ref.getBlockPos().offset(0,3,3),false);ref.transfer();h.assertTrue(tank.input.getFluidAmount()==975,"Broken chimney prevents remote intake");
        var cap=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,at,Direction.WEST);h.assertTrue(cap.fill(new FluidStack(IndustrialContent.CRUDE.get(),100),EXECUTE)==0,"Broken build also closes external capability");
        h.runAfterDelay(2,()->{
            var flow=(PipeFlowEntity)h.getLevel().getBlockEntity(mid);var dead=(PipeFlowEntity)h.getLevel().getBlockEntity(mid.north());
            h.assertTrue(flow.fluids[Direction.EAST.ordinal()]==1&&flow.signs[Direction.EAST.ordinal()]==1,"Crude flow points into heater");
            h.assertTrue(java.util.Arrays.stream(dead.until).allMatch(t->t==0),"Dead branch never displays flow");
            h.assertTrue(h.getLevel().getBlockState(mid).getValue(FluidPipeBlock.CONNECTIONS.get(Direction.EAST)),"Authored pipe connects to flange after refresh");
            h.runAfterDelay(18,()->{h.assertTrue(flow.until[Direction.EAST.ordinal()]<=h.getLevel().getGameTime(),"Stopped flow expires");h.succeed();});
        });
    }
    @GameTest(template="empty") public static void columnBlockedCoproductsAndSaveReload(GameTestHelper h){
        var col=machine(h,IndustrialContent.COLUMN.get(),3);col.input.fill(new FluidStack(IndustrialContent.HEATED.get(),2000),EXECUTE);col.lubricant.fill(new FluidStack(IndustrialContent.LUBE.get(),4000),EXECUTE);run(col,20);
        h.assertTrue(col.input.getFluidAmount()==2000&&col.output.isEmpty()&&col.status==4,"Oil outlet full stops entire split");
        col.lubricant.drain(4000,EXECUTE);for(int slot:MachineInventory.INDUSTRIAL_OUTPUT)col.setItem(slot,IndustrialContent.SULFUR.toStack(32));run(col,20);h.assertTrue(col.input.getFluidAmount()==2000,"Sulfur full also blocks without discarding products");
        col.removeItem(1,32);run(col,40);var copy=new IndustrialBlockEntity(col.getBlockPos(),col.getBlockState());copy.loadWithComponents(col.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.input.getFluidAmount()==1000&&copy.output.getFluidAmount()==800&&copy.lubricant.getFluidAmount()==200&&copy.getItem(1).getCount()==1,"All refinery coproducts persist");h.succeed();
    }
    @GameTest(template="empty") public static void lubricationIsWorkBasedRecoverableAndSaved(GameTestHelper h){
        var drill=machine(h,IndustrialContent.DRILL.get(),3);var coal=site(drill,Deposits.Kind.COAL,3);bind(drill,coal);drill.input.fill(new FluidStack(IndustrialContent.FUEL.get(),300),EXECUTE);drill.lubrication=200;
        run(drill,49);h.assertTrue(drill.getItem(1).isEmpty()&&drill.lubrication==200,"Dry machine runs at one-fifth speed, never destroys itself");run(drill,1);h.assertTrue(drill.getItem(1).getCount()==16,"Dry machine eventually completes");drill.removeItem(1,32);
        drill.lubricant.fill(new FluidStack(IndustrialContent.LUBE.get(),10),EXECUTE);run(drill,10);h.assertTrue(drill.lubrication==1000&&drill.lubricant.isEmpty(),"Oil restores full condition");
        var copy=new IndustrialBlockEntity(drill.getBlockPos(),drill.getBlockState());copy.loadWithComponents(drill.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.lubrication==drill.lubrication,"Lubrication persists");h.succeed();
    }
    @GameTest(template="empty") public static void remotePortRevokesAfterControllerRemoval(GameTestHelper h){
        var ref=machine(h,IndustrialContent.REFINERY.get(),3);IndustrialStructure.bind(ref);var at=port(ref,"output_port");ref.output.fill(new FluidStack(IndustrialContent.HEATED.get(),1000),EXECUTE);
        var cap=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,at,Direction.EAST);h.assertTrue(cap.drain(100,SIMULATE).getAmount()==100,"Output port exposes heated stream");h.getLevel().removeBlock(ref.getBlockPos(),false);h.assertTrue(cap.drain(100,EXECUTE).isEmpty(),"Cached port cannot drain removed controller");h.succeed();
    }
    @GameTest(template="empty") public static void hotStreamsCannotBecomeFuelCanisters(GameTestHelper h){
        var ref=machine(h,IndustrialContent.REFINERY.get(),3);ref.output.fill(new FluidStack(IndustrialContent.HEATED.get(),1000),EXECUTE);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"hot-test"));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IndustrialContent.CAN.toStack());ref.canister(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(ref.output.getFluidAmount()==1000&&p.getMainHandItem().is(IndustrialContent.CAN.get()),"Hot stream cannot bypass condenser using canister");h.succeed();
    }

    @GameTest(template="empty") public static void pipeLoopsAndNetworkBudgetConserveLiquid(GameTestHelper h){
        var l=h.getLevel();var start=h.absolutePos(new BlockPos(2,167,2));l.setBlockAndUpdate(start,IndustrialContent.TANK.get().defaultBlockState());var source=(IndustrialBlockEntity)l.getBlockEntity(start);source.input.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);
        for(int x=1;x<=3;x++){pipe(h,start.east(x));pipe(h,start.east(x).south());}var end=start.east(4);l.setBlockAndUpdate(end,IndustrialContent.TANK.get().defaultBlockState());var target=(IndustrialBlockEntity)l.getBlockEntity(end);
        PipeRouting.transfer(l,start,Direction.EAST,source.input,true);h.assertTrue(source.input.getFluidAmount()==975&&target.input.getFluidAmount()==25,"Loop has a single bounded transfer, no duplicate delivery");
        for(int x=4;x<=132;x++)pipe(h,start.east(x).south());int before=source.input.getFluidAmount();PipeRouting.transfer(l,start,Direction.EAST,source.input,true);h.assertTrue(source.input.getFluidAmount()==before&&target.input.getFluidAmount()==25,"Oversized graph aborts before delivering to even the nearby endpoint");h.succeed();
    }
    @GameTest(template="empty") public static void fertilizerRequiresSulfurAndGravel(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        h.assertTrue(manager.byKey(net.minecraft.resources.ResourceLocation.parse("civilization:retort_fertilizer")).isEmpty()&&manager.byKey(net.minecraft.resources.ResourceLocation.parse("civilization:mineral_blend")).isEmpty(),"Clay blend and its firing route are retired");
        var old=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(Items.CLAY_BALL),new ItemStack(Items.GRAVEL)));
        h.assertTrue(manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,old,h.getLevel()).isEmpty(),"Clay plus gravel cannot make fertilizer feedstock");
        var recipes=manager.getAllRecipesFor(KilnContent.RETORT_RECIPE_TYPE.get());
        h.assertTrue(recipes.size()==1&&recipes.getFirst().value().getIngredients().getFirst().test(IndustrialContent.ENRICHED_BLEND.toStack())&&!recipes.getFirst().value().getIngredients().getFirst().test(FarmingContent.MINERAL_BLEND.toStack()),"Only sulfur blend can be fired, including existing raw blend stock");h.succeed();
    }
}

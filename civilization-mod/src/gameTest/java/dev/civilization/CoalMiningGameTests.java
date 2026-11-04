package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class CoalMiningGameTests {
    @GameTest(template="industrial")
    public static void survivalSmithyMakesAndRepairsManualCoalTools(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);
        for(var tool:java.util.List.of(CoalMiningContent.PICK.get(),CoalMiningContent.SHOVEL.get())){
            w.clearContent();w.heat=0;
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tool).toString();
            int index=WorkshopJobs.index(3,id);h.assertTrue(index>=0,"Coal tool has a Smithy job");w.select(index);
            var job=WorkshopJobs.jobs(3).get(index);
            for(int i=0;i<4;i++)w.setItem(i,job.needs().get(i).icon());
            WorkshopGameTests.run(w,job.ticks());
            var result=w.getItem(5).copy();
            h.assertTrue(result.is(tool)&&EquipmentGrade.hasGrade(result),"Smithy produces a graded coal tool");
            h.assertTrue(w.getItem(4).isEmpty()&&w.heat==0,"Hand assembly spends no Coal");
            for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Actual tool materials are consumed");
            EquipmentGrade.apply(result,1000); // Repairability here must not depend on the production grade roll.
            result.setDamageValue(Math.max(1,result.getMaxDamage()/2));
            h.assertTrue(WorkshopJobs.repair(result)!=null&&WorkshopJobs.gated(result),"Manual tools use Smithy repair and equipment gates");
        }
        h.succeed();
    }
    @GameTest(template="industrial")
    public static void survivalCoalHandlingCraftsFromOrdinaryMaterials(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);
        for(Item output:java.util.List.of(CoalMiningContent.CART_ITEM.get(),CoalMiningContent.SCREEN_ITEM.get())){
            var job=ManufacturingGameTests.job(3,output);ManufacturingGameTests.supply(w,job);WorkshopGameTests.run(w,job.ticks());
            h.assertTrue(w.getItem(5).is(output),"Prepared stock assembles coal infrastructure");
            for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Handling assembly conserves stock");
        }
        h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h,BlockPos at){var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"coal-miner"));p.setGameMode(GameType.SURVIVAL);p.setPos(at.getX()+.5,at.getY()-1,at.getZ()-2);p.setItemInHand(InteractionHand.MAIN_HAND,CoalMiningContent.PICK.toStack());EquipmentGrade.apply(p.getMainHandItem(),1000,42);CalorieFoodData.of(p).reserve().set(2400);return p;}
    private static BlockPos face(GameTestHelper h){var a=h.absolutePos(new BlockPos(3,3,3));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){h.getLevel().setBlockAndUpdate(a.offset(x,-2,z),Blocks.STONE.defaultBlockState());for(int y=-1;y<=2;y++)h.getLevel().setBlockAndUpdate(a.offset(x,y,z),Blocks.AIR.defaultBlockState());}h.getLevel().setBlockAndUpdate(a,CoalMiningContent.FACE.get().defaultBlockState());return a;}
    private static void aim(net.minecraft.server.level.ServerPlayer p,Vec3 at){var d=at.subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
    @GameTest(template="industrial") public static void partialCoalUsesRealCollisionAndCannotPushOutOfEmptySpace(GameTestHelper h){
        var a=face(h);var l=h.getLevel();var e=(CoalWorkfaceEntity)l.getBlockEntity(a);e.mask(1L);
        var state=l.getBlockState(a);
        h.assertTrue(state.getCollisionShape(l,a).bounds().maxX==.25&&state.getCollisionShape(l,a).bounds().maxY==.25,"Block-state collision follows the saved remnant, not a cached full cube");
        h.assertTrue(!state.isSuffocating(l,a)&&!state.isFaceSturdy(l,a,Direction.UP),"A remnant neither triggers client push-out nor supports a floating pile");
        var p=player(h,a);p.setPos(a.getX()+.65,a.getY(),a.getZ()-.4);p.setDeltaMovement(Vec3.ZERO);
        p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,0,1.2));
        h.assertTrue(p.getZ()>a.getZ()+.79,"Player movement crosses the actual empty part of a worked block");h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=100) public static void miningSupportDropsThePileAboveTheSameBlockState(GameTestHelper h){
        var a=face(h);var l=h.getLevel();var e=(CoalWorkfaceEntity)l.getBlockEntity(a);
        h.assertTrue(RawCoalPileBlock.insert(l,a.above(),12)==12,"An intact face initially supports a pile");
        e.mask(1L);
        h.runAfterDelay(3,()->h.assertTrue(l.getBlockState(a.above()).isAir(),"Partial material cannot leave its former pile floating above it"));
        h.runAfterDelay(65,()->{
            int stock=0;for(var q:BlockPos.betweenClosed(a.offset(-2,-1,-2),a.offset(2,1,2))){var s=l.getBlockState(q);if(s.is(CoalMiningContent.PILE.get()))stock+=s.getValue(RawCoalPileBlock.UNITS);}
            stock+=l.getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(3)).stream().mapToInt(LooseCoalEntity::units).sum();
            h.assertTrue(stock==12,"Falling and settled stock is conserved after the mask changes");h.succeed();
        });
    }
    @GameTest(template="industrial",timeoutTicks=100) public static void looseChunkRollsOffASmallCoalRemnant(GameTestHelper h){
        var a=face(h);var l=h.getLevel();long pillar=0;for(int y=0;y<4;y++)pillar|=1L<<CoalGeometry.index(1,y,1);
        ((CoalWorkfaceEntity)l.getBlockEntity(a)).mask(pillar);
        var chunk=LooseCoalEntity.create(l,new Vec3(a.getX()+.375,a.getY()+1,a.getZ()+.375),4);chunk.setOnGround(true);
        h.runAfterDelay(75,()->{
            int stock=0;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var s=l.getBlockState(a.offset(x,-1,z));if(s.is(CoalMiningContent.PILE.get()))stock+=s.getValue(RawCoalPileBlock.UNITS);}
            h.assertTrue(chunk.isRemoved()&&stock==4,"A coherent four-unit chunk rolls off the narrow support and settles on the lower floor");h.succeed();
        });
    }
    @GameTest(template="industrial",timeoutTicks=30) public static void landedChunkResumesGravityWhenFloorDisappears(GameTestHelper h){
        var a=face(h).north();var l=h.getLevel();l.setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());
        var chunk=LooseCoalEntity.create(l,new Vec3(a.getX()+.5,a.getY()+1,a.getZ()+.5),4);chunk.setOnGround(true);
        l.removeBlock(a,false);
        h.runAfterDelay(3,()->{h.assertTrue(chunk.getY()<a.getY()+.9&&chunk.units()==4,"Previously landed stock immediately resumes falling without loss");h.succeed();});
    }
    @GameTest(template="industrial") public static void edgeOfDeepNotchReleasesChunksIntoActuallyRemovedMaterial(GameTestHelper h){
        var a=face(h);var l=h.getLevel();var p=player(h,a);
        p.setPos(a.getX()+.49,a.getY()+.49-p.getEyeHeight(),a.getZ()-2);p.setYRot(0);p.setXRot(0);
        for(int depth=0;depth<4;depth++){
            h.assertTrue(CoalMiningSystem.strike(p),"The edge of every newly exposed layer can be chipped");
            var chunk=l.getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(3)).stream().max(java.util.Comparator.comparingInt(net.minecraft.world.entity.Entity::getId)).orElseThrow();
            h.assertTrue(chunk.units()==4&&l.noCollision(chunk,chunk.getBoundingBox()),"The released chunk starts inside removed cells, clear of neighboring coal");
        }
        h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=100) public static void strikeChipsPersistsAndPaysOnce(GameTestHelper h){
        var a=face(h);var p=player(h,a);aim(p,Vec3.atLowerCornerOf(a).add(.25,.25,0));h.assertTrue(CoalMiningSystem.strike(p),"An actual aimed strike chips the face");
        var e=(CoalWorkfaceEntity)h.getLevel().getBlockEntity(a);h.assertTrue(Long.bitCount(e.mask())==60&&p.getMainHandItem().getDamageValue()==1,"Four cells removed, one durability");
        var saved=e.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new CoalWorkfaceEntity(a,e.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(restored.mask()==e.mask()&&restored.shape()==e.shape(),"Exact mask/cache survive save");
        var carrier=h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(3)).getFirst();h.assertTrue(carrier.units()==4,"One saved batch owns the extracted material");
        h.runAfterDelay(45,()->{int piles=0;for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var s=h.getLevel().getBlockState(a.offset(x,-1,z));if(s.is(CoalMiningContent.PILE.get()))piles+=s.getValue(RawCoalPileBlock.UNITS);}h.assertTrue(piles==4,"Loose coal settles without item drops");h.assertTrue(CalorieFoodData.of(p).broken==1&&Math.abs(CalorieFoodData.of(p).laborSpent-2)<.01,"Exactly one chipping labor charge");h.succeed();});
    }
    @GameTest(template="industrial") public static void canceledOrOrdinaryTargetsProduceNothing(GameTestHelper h){
        var a=face(h);var p=player(h,a);aim(p,a.getCenter());java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> cancel=e->{if(e.getPlayer()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{h.assertTrue(!CoalMiningSystem.strike(p)&&Long.bitCount(((CoalWorkfaceEntity)h.getLevel().getBlockEntity(a)).mask())==64&&p.getMainHandItem().getDamageValue()==0,"Canceled extraction changes nothing");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.getLevel().setBlockAndUpdate(a,Blocks.COAL_ORE.defaultBlockState());h.assertTrue(!CoalMiningSystem.strike(p)&&h.getLevel().getBlockState(a).is(Blocks.COAL_ORE),"Special pick leaves ordinary coal mining rules alone");h.succeed();
    }
    @GameTest(template="industrial") public static void scoopCartScreenConservePartialLoads(GameTestHelper h){
        var a=face(h);var p=player(h,a);var pile=a.north();h.getLevel().setBlockAndUpdate(pile.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(pile,CoalMiningContent.PILE.get().defaultBlockState().setValue(RawCoalPileBlock.UNITS,20));p.setItemInHand(InteractionHand.MAIN_HAND,CoalMiningContent.SHOVEL.toStack());
        var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pile.getCenter(),Direction.UP,pile,false));CoalMiningContent.SHOVEL.get().useOn(ctx);h.assertTrue(CoalMiningContent.load(p.getMainHandItem())==16&&h.getLevel().getBlockState(pile).getValue(RawCoalPileBlock.UNITS)==4,"Bounded scoop leaves remainder");
        var cart=new CoalMinecart(CoalMiningContent.CART.get(),h.getLevel());cart.setPos(pile.getCenter());cart.insert(CoalMinecart.CAPACITY-8);cart.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(cart.load()==CoalMinecart.CAPACITY&&CoalMiningContent.load(p.getMainHandItem())==8,"Full cart leaves excess on shovel");
        cart.extract(cart.load());cart.insert(16);var at=pile.east();h.getLevel().setBlockAndUpdate(at,CoalMiningContent.SCREEN.get().defaultBlockState());var screen=(CoalScreenEntity)h.getLevel().getBlockEntity(at);h.assertTrue(screen.unload(cart,p)==16&&cart.load()==0,"Actual cart-to-screen transfer");
        h.assertTrue(screen.work(p)&&!screen.work(p),"One accepted batch; repeated work is paced");
        for(int i=1;i<4;i++)h.runAfterDelay(i*10,()->h.assertTrue(screen.work(p),"Each paced real batch produces Coal"));
        h.runAfterDelay(35,()->{
        h.assertTrue(screen.raw()==0&&screen.coal()==4&&!screen.work(p),"No empty-screen production");
        var save=screen.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new CoalScreenEntity(at,screen.getBlockState());copy.loadWithComponents(save,h.getLevel().registryAccess());h.assertTrue(copy.raw()==0&&copy.coal()==4,"Screen stock survives save");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);screen.use(p);h.assertTrue(screen.coal()==0&&p.getInventory().countItem(KilnContent.MINERAL_COAL.get())==4,"Collect actual existing usable Coal");h.succeed();});
    }
    @GameTest(template="industrial",timeoutTicks=100) public static void blockedLandingAndLostSupportRetainStock(GameTestHelper h){
        var a=face(h).north();h.getLevel().setBlockAndUpdate(a.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(a,CoalMiningContent.PILE.get().defaultBlockState().setValue(RawCoalPileBlock.UNITS,64));h.getLevel().removeBlock(a.below(),false);
        h.runAfterDelay(2,()->{h.assertTrue(h.getLevel().getBlockState(a).isAir()&&h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(3)).stream().mapToInt(LooseCoalEntity::units).sum()==64,"Unsupported pile becomes one saved falling batch");h.succeed();});
    }
    @GameTest(template="industrial",timeoutTicks=100) public static void wholeSeamYieldsSixteenCoalWithoutRepeatPay(GameTestHelper h){
        var a=face(h);h.getLevel().setBlockAndUpdate(a,IndustrialContent.COAL_SEAM.get().defaultBlockState());var p=player(h,a);
        for(double x:new double[]{.25,.75})for(double y:new double[]{.25,.75}){
            p.setPos(a.getX()+x,a.getY()+y-p.getEyeHeight(),a.getZ()-2);p.setYRot(0);p.setXRot(0);
            for(int depth=0;depth<4;depth++)h.assertTrue(CoalMiningSystem.strike(p),"Every exposed patch can be reached through its notch");
        }
        h.assertTrue(h.getLevel().getBlockState(a).isAir()&&!CoalMiningSystem.strike(p),"Depleted vein cannot pay twice");
        int raw=h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(4)).stream().mapToInt(LooseCoalEntity::units).sum();
        h.assertTrue(raw==64&&raw/4==16&&p.getMainHandItem().getDamageValue()==16,"Exactly 64 raw units / 16 usable Coal, one wear per contact");
        h.runAfterDelay(50,()->{
            int piles=0;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var s=h.getLevel().getBlockState(a.offset(x,-1,z));if(s.is(CoalMiningContent.PILE.get()))piles+=s.getValue(RawCoalPileBlock.UNITS);}
            h.assertTrue(piles==64&&h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(4)).isEmpty(),"Deep-notch extraction expels every batch onto the floor without stranded stock");h.succeed();
        });
    }
    @GameTest(template="industrial") public static void rejectedCarrierAndChangedTargetCannotDuplicate(GameTestHelper h){
        var a=face(h);var p=player(h,a);aim(p,a.getCenter());
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=e->{if(e.getEntity() instanceof LooseCoalEntity&&e.getEntity().blockPosition().distManhattan(a)<4)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{h.assertTrue(!CoalMiningSystem.strike(p)&&((CoalWorkfaceEntity)h.getLevel().getBlockEntity(a)).mask()==CoalGeometry.FULL&&p.getMainHandItem().getDamageValue()==0,"Rejected carrier leaves the source and tool unchanged");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> change=e->{if(e.getPlayer()==p)h.getLevel().setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(change);
        try{h.assertTrue(!CoalMiningSystem.strike(p)&&h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(4)).isEmpty(),"Changed hook target grants no stock");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(change);}h.succeed();
    }
    @GameTest(template="industrial") public static void windupRechecksAccessAndTool(GameTestHelper h){
        var a=face(h);var p=player(h,a);aim(p,a.getCenter());var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),a);c.owner=new CivicData.Owner(java.util.UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();c.whitelist.put(p.getUUID(),"coal-miner");data.claims.put(c.at,c);data.rebuildIndex();
        h.assertTrue(CoalMiningSystem.start(p)&&!CoalMiningSystem.start(p),"One click begins one bounded swing");c.whitelist.clear();
        h.runAfterDelay(CoalPickItem.CONTACT+1,()->{try{CoalMiningSystem.step(p);h.assertTrue(((CoalWorkfaceEntity)h.getLevel().getBlockEntity(a)).mask()==CoalGeometry.FULL&&p.getMainHandItem().getDamageValue()==0,"Access revoked before contact rejects stock and wear");}finally{data.claims.remove(c.at);data.rebuildIndex();}});
        h.runAfterDelay(CoalPickItem.DURATION+1,()->{p.getCooldowns().removeCooldown(CoalMiningContent.PICK.get());h.assertTrue(CoalMiningSystem.start(p),"Next discrete swing accepted after recovery");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});
        h.runAfterDelay(CoalPickItem.DURATION+CoalPickItem.CONTACT+2,()->{CoalMiningSystem.step(p);h.assertTrue(((CoalWorkfaceEntity)h.getLevel().getBlockEntity(a)).mask()==CoalGeometry.FULL,"Swapping away cancels contact");h.succeed();});
    }
    @GameTest(template="industrial") public static void savedCartAndCreativeDestructionPreserveCargo(GameTestHelper h){
        var a=face(h);var p=player(h,a);p.setGameMode(GameType.CREATIVE);var cart=new CoalMinecart(CoalMiningContent.CART.get(),h.getLevel());cart.setPos(a.getCenter());cart.insert(123);h.getLevel().addFreshEntity(cart);
        var save=new net.minecraft.nbt.CompoundTag();cart.saveWithoutId(save);var copy=new CoalMinecart(CoalMiningContent.CART.get(),h.getLevel());copy.load(save);h.assertTrue(copy.load()==123,"Saved rail cart retains its exact raw cargo");
        cart.hurt(h.getLevel().damageSources().playerAttack(p),1);h.assertTrue(cart.isRemoved()&&h.getLevel().getEntitiesOfClass(LooseCoalEntity.class,new AABB(a).inflate(3)).stream().mapToInt(LooseCoalEntity::units).sum()==123,"Native Creative discard still spills saved raw cargo exactly once");h.succeed();
    }
}

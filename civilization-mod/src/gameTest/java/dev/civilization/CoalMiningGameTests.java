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
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h,BlockPos at){var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"coal-miner"));p.setGameMode(GameType.SURVIVAL);p.setPos(at.getX()+.5,at.getY()-1,at.getZ()-2);p.setItemInHand(InteractionHand.MAIN_HAND,CoalMiningContent.PICK.toStack());EquipmentGrade.apply(p.getMainHandItem(),100,42);CalorieFoodData.of(p).reserve().set(2400);return p;}
    private static BlockPos face(GameTestHelper h){var a=h.absolutePos(new BlockPos(3,3,3));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){h.getLevel().setBlockAndUpdate(a.offset(x,-2,z),Blocks.STONE.defaultBlockState());for(int y=-1;y<=2;y++)h.getLevel().setBlockAndUpdate(a.offset(x,y,z),Blocks.AIR.defaultBlockState());}h.getLevel().setBlockAndUpdate(a,CoalMiningContent.FACE.get().defaultBlockState());return a;}
    private static void aim(net.minecraft.server.level.ServerPlayer p,Vec3 at){var d=at.subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
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

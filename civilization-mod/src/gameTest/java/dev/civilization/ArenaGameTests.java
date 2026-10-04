package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ArenaGameTests {
    public static ArenaData.Pit build(ServerLevel level,BlockPos at,Direction front){
        var pit=new ArenaData.Pit();pit.at=ArenaService.address(level,at);pit.front=front;pit.owner=UUID.randomUUID();
        for(var c:ArenaLayout.FLOOR){var pos=ArenaStructure.position(at,front,c.x(),0,c.z());level.setBlockAndUpdate(pos,Blocks.STONE_BRICKS.defaultBlockState());for(int y=1;y<=5;y++)level.setBlockAndUpdate(pos.above(y),Blocks.AIR.defaultBlockState());}
        level.setBlockAndUpdate(at,ArenaContent.PIT.get().defaultBlockState().setValue(CivicBlock.FACING,front));
        for(var part:ArenaStructure.PARTS)MachineStructure.placePart(level,at,front,part);return pit;
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,String name){
        var profile=new GameProfile(UUID.randomUUID(),name);var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        // Only transport is mocked; health, PvP, equipment and damage use ordinary ServerPlayer.
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);for(int i=0;i<61;i++)p.tick();return p;
    }
    private static ArenaData.Pit pit(GameTestHelper h){return build(h.getLevel(),h.absolutePos(new BlockPos(21,1,11)),Direction.NORTH);}
    private static void pair(ArenaData.Pit p,net.minecraft.server.level.ServerPlayer a,net.minecraft.server.level.ServerPlayer b){p.fighters[0]=a.getUUID();p.fighters[1]=b.getUUID();}
    @GameTest(template="arena") public static void structureFloorAndOutsideSeats(GameTestHelper h){
        var p=pit(h);h.assertTrue(ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Complete supported pit validates");
        ArenaStructure.gates(h.getLevel(),p,true);h.assertTrue(ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Open gates validate");ArenaStructure.gates(h.getLevel(),p,false);
        h.getLevel().setBlockAndUpdate(p.at.pos().offset(0,1,10),Blocks.OAK_STAIRS.defaultBlockState());h.assertTrue(ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Outside seats independent");
        var wall=ArenaStructure.position(p.at.pos(),p.front,0,1,9);h.getLevel().removeBlock(wall,false);h.assertTrue(!ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Missing wall rejects");h.getLevel().setBlockAndUpdate(wall,Blocks.STONE_BRICKS.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.at.pos().above(),Blocks.STONE.defaultBlockState());h.assertTrue(ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Interior obstacles validate");h.getLevel().removeBlock(p.at.pos().above(),false);h.getLevel().removeBlock(p.at.pos().offset(1,0,0),false);h.assertTrue(ArenaStructure.problem(h.getLevel(),p).isEmpty(),"Floor holes validate");h.succeed();
    }
    @GameTest(template="arena") public static void heldConstructionConservesMaterials(GameTestHelper h){
        var p=pit(h);var user=player(h,"builder");var missing=ArenaStructure.position(p.at.pos(),p.front,0,1,9);h.getLevel().removeBlock(missing,false);var stock=new ItemStack(Items.STONE_BRICKS,2);
        h.assertTrue(MachineConstruction.build(h.getLevel(),p.at.pos(),p.front,ArenaStructure.PARTS,user,stock)==1&&stock.getCount()==1,"One missing wall consumes one block");h.assertTrue(MachineConstruction.build(h.getLevel(),p.at.pos(),p.front,ArenaStructure.PARTS,user,stock)==0&&stock.getCount()==1,"Retry does not spend again");h.succeed();
    }
    @GameTest(template="arena") public static void fighterStakesClearConsentAndRefund(GameTestHelper h){
        var p=pit(h);var d=new ArenaData();var a=player(h,"a");var b=player(h,"b");pair(p,a,b);p.accepted[0]=p.accepted[1]=true;a.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));
        h.assertTrue(ArenaService.action(d,p,a,2,0)&&p.stakes[0].getCount()==3&&a.getMainHandItem().isEmpty()&&!p.accepted[0]&&!p.accepted[1],"Stake clears both consents");ArenaService.action(d,p,a,3,0);h.assertTrue(p.stakes[0].isEmpty()&&d.credits.get(a.getUUID()).getFirst().getCount()==3,"Refund original depositor");ArenaService.collect(d,a);h.assertTrue(CivicItems.count(a,Items.DIAMOND.getDefaultInstance())==3,"Collect exact stake");h.succeed();
    }
    @GameTest(template="arena") public static void competingBetAcceptorsAndPayoutRetry(GameTestHelper h){
        var p=pit(h);var d=new ArenaData();p.fighters[0]=UUID.randomUUID();p.fighters[1]=UUID.randomUUID();var a=player(h,"offer");var b=player(h,"match");var c=player(h,"late");for(var x:List.of(a,b,c))x.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,8));
        h.assertTrue(ArenaService.action(d,p,a,6,0),"Offer funded");int id=p.bets.getFirst().id;h.assertTrue(ArenaService.action(d,p,b,8,id)&&!ArenaService.action(d,p,c,8,id)&&CivicItems.count(c,Items.IRON_INGOT.getDefaultInstance())==8,"One counterparty pays");d.settle(p,0,"winner");
        h.assertTrue(d.credits.get(a.getUUID()).stream().mapToInt(ItemStack::getCount).sum()==16&&!d.credits.containsKey(b.getUUID()),"Winning backer gets exact stakes");d.settle(p,0,"retry");h.assertTrue(d.credits.get(a.getUUID()).size()==2,"Retry cannot duplicate");h.succeed();
    }
    @GameTest(template="arena") public static void componentsAndFullInventoryRecovery(GameTestHelper h){
        var p=pit(h);var d=new ArenaData();p.fighters[0]=UUID.randomUUID();p.fighters[1]=UUID.randomUUID();var a=player(h,"offer");var b=player(h,"match");var named=new ItemStack(Items.IRON_INGOT,2);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Marked Iron"));a.getInventory().setItem(0,named.copy());b.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,2));
        ArenaService.action(d,p,a,6,0);h.assertTrue(!ArenaService.action(d,p,b,8,p.bets.getFirst().id),"Components must match");d.settle(p,-1,"refund");for(int i=0;i<36;i++)a.getInventory().setItem(i,new ItemStack(Items.STONE,Items.STONE.getDefaultMaxStackSize()));
        h.assertTrue(!ArenaService.collect(d,a)&&ItemStack.isSameItemSameComponents(d.credits.get(a.getUUID()).getFirst(),named),"Full inventory retains component-exact refund");a.getInventory().setItem(0,ItemStack.EMPTY);h.assertTrue(ArenaService.collect(d,a)&&ItemStack.isSameItemSameComponents(a.getMainHandItem(),named),"Later collect retains components");h.succeed();
    }
    @GameTest(template="arena") public static void restartAndControllerRemovalKeepCredits(GameTestHelper h){
        var p=pit(h);var d=new ArenaData();var a=UUID.randomUUID();p.fighters[0]=a;p.fighters[1]=UUID.randomUUID();p.stakes[0]=new ItemStack(Items.DIAMOND,4);p.ready[0]=true;p.health[0]=20;p.phase=ArenaData.LIVE;d.pits.put(p.at,p);
        var restored=ArenaData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(restored.credits.get(a).getFirst().getCount()==4&&restored.recovery.get(a)==20&&restored.pits.get(p.at).phase==ArenaData.LOBBY,"Restart refunds rather than resumes");restored.pits.clear();var again=ArenaData.load(restored.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(again.credits.get(a).getFirst().getCount()==4,"Credit survives controller deletion");h.succeed();
    }
    @GameTest(template="arena") public static void knockoutAndOutsideDamageProtection(GameTestHelper h){
        var p=pit(h);var d=ArenaData.get(h.getLevel().getServer());var a=player(h,"a");var b=player(h,"b");var outsider=player(h,"outside");pair(p,a,b);p.phase=ArenaData.LIVE;Arrays.fill(p.ready,true);Arrays.fill(p.health,20);d.pits.put(p.at,p);
        a.setPos(p.at.pos().getX()+2.5,p.at.pos().getY()+1,p.at.pos().getZ()+.5);b.setPos(p.at.pos().getX()-1.5,p.at.pos().getY()+1,p.at.pos().getZ()+.5);b.getInventory().setItem(0,new ItemStack(Items.DIAMOND,2));b.setHealth(3);
        b.hurt(b.damageSources().playerAttack(outsider),100);h.assertTrue(b.getHealth()==3&&p.phase==ArenaData.LIVE,"Outside attacks blocked");b.hurt(b.damageSources().generic(),100);h.assertTrue(b.getHealth()==3,"Environmental lethal damage blocked");b.hurt(b.damageSources().playerAttack(a),100);
        h.assertTrue(b.isAlive()&&b.getHealth()>0&&p.phase==ArenaData.RESULT&&p.winner==0&&CivicItems.count(b,Items.DIAMOND.getDefaultInstance())==2,"Opponent lethal hit becomes knockout without drops");d.pits.remove(p.at);d.recovery.remove(a.getUUID());d.recovery.remove(b.getUUID());h.succeed();
    }
    @GameTest(template="arena") public static void lobbyDoesNotGrantWorldInvulnerability(GameTestHelper h){
        var p=pit(h);var d=ArenaData.get(h.getLevel().getServer());var user=player(h,"lobby");p.fighters[0]=user.getUUID();d.pits.put(p.at,p);user.setHealth(20);user.setPos(p.at.pos().getX(),p.at.pos().getY()+1,p.at.pos().getZ());user.hurt(user.damageSources().generic(),2);h.assertTrue(user.getHealth()<20,"Unready lobby fighter has normal damage");d.pits.remove(p.at);h.succeed();
    }
    @GameTest(template="arena") public static void staleMenuAndControllerDestruction(GameTestHelper h){
        var p=pit(h);var d=ArenaData.get(h.getLevel().getServer());var user=player(h,"menu");d.pits.put(p.at,p);user.setPos(p.at.pos().getX()+.5,p.at.pos().getY()+1,p.at.pos().getZ()+.5);user.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));var menu=new ArenaMenu(1,user,p);user.containerMenu=menu;
        int old=p.revision;menu.action(old,0,0);menu.action(old,2,0);h.assertTrue(p.side(user.getUUID())==0&&p.stakes[0].isEmpty()&&user.getMainHandItem().getCount()==3,"Stale stake request rejected");menu.action(p.revision,2,0);
        h.getLevel().removeBlock(p.at.pos(),false);h.assertTrue(!d.pits.containsKey(p.at)&&d.credits.get(user.getUUID()).getFirst().getCount()==3,"Actual removal refunds to persistent personal credit");h.assertTrue(ArenaService.collect(d,user)&&CivicItems.count(user,Items.DIAMOND.getDefaultInstance())==3,"Removal never drops escrow or duplicates it");h.succeed();
    }
    @GameTest(template="arena") public static void interruptionCannotAwardWinner(GameTestHelper h){
        var p=pit(h);var d=new ArenaData();p.fighters[0]=UUID.randomUUID();p.fighters[1]=UUID.randomUUID();p.stakes[0]=new ItemStack(Items.DIAMOND,4);p.stakes[1]=new ItemStack(Items.IRON_INGOT,6);p.phase=ArenaData.LIVE;
        h.getLevel().removeBlock(ArenaStructure.position(p.at.pos(),p.front,0,1,9),false);ArenaService.finish(d,p,h.getLevel(),0,"attempted knockout");h.assertTrue(p.winner==-1&&d.credits.get(p.fighters[0]).getFirst().getCount()==4&&d.credits.get(p.fighters[1]).getFirst().getCount()==6,"Damaged wall converts result into original-player refunds");h.succeed();
    }
    @GameTest(template="arena") public static void customTerrainRoomsAndRaisedFightBounds(GameTestHelper h){
        var p=pit(h);var level=h.getLevel();var user=player(h,"custom");
        level.removeBlock(ArenaStructure.position(p.at.pos(),p.front,2,0,0),false);
        level.setBlockAndUpdate(ArenaStructure.position(p.at.pos(),p.front,3,1,0),Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(ArenaStructure.position(p.at.pos(),p.front,4,4,0),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(ArenaStructure.position(p.at.pos(),p.front,-17,1,0),Blocks.CHEST.defaultBlockState());
        h.assertTrue(ArenaStructure.problem(level,p).isEmpty(),"Terrain, hazards, raised blocks and prep furniture are independent of the shell");
        var spawn=ArenaStructure.prepPosition(user,p,0);h.assertTrue(spawn!=null&&level.noCollision(user,user.getBoundingBox().move(spawn.subtract(user.position()))),"Prep spawn avoids furniture");
        h.assertTrue(ArenaStructure.inside(p,net.minecraft.world.phys.Vec3.atCenterOf(p.at.pos()).add(2,-8,0),true)&&ArenaStructure.inside(p,net.minecraft.world.phys.Vec3.atCenterOf(p.at.pos()).add(2,12,0),true),"Custom elevations remain fighting space");
        for(int x=-19;x<=-15;x++)for(int z=-2;z<=2;z++)for(int y=1;y<=3;y++)level.setBlockAndUpdate(ArenaStructure.position(p.at.pos(),p.front,x,y,z),Blocks.STONE.defaultBlockState());
        h.assertTrue(ArenaStructure.problem(level,p).isEmpty()&&ArenaStructure.prepPosition(user,p,0)==null,"Filled room still forms but cannot teleport a fighter into solid blocks");h.succeed();
    }
}

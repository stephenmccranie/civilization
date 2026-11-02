package dev.civilization;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class SharedCraftingGameTests {
    private static BlockPos table(GameTestHelper h){var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());return pos;}
    private static FakePlayer player(GameTestHelper h,BlockPos pos){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"shared-crafter"));p.setGameMode(GameType.SURVIVAL);p.setPos(pos.offset(1,0,0).getCenter());return p;
    }
    private static CraftingMenu open(GameTestHelper h,BlockPos pos,FakePlayer player){
        var menu=new CraftingMenu(1,player.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));player.containerMenu=menu;BlockCarry.attachMenu(menu,player.getInventory());return menu;
    }
    @GameTest(template="industrial") public static void handCanStageLogsAndPlanksAcrossClosing(GameTestHelper h){
        var pos=table(h);var p=player(h,pos);var menu=open(h,pos,p);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_LOG,2));
        menu.clicked(46,0,ClickType.PICKUP,p);menu.clicked(1,0,ClickType.PICKUP,p);menu.removed(p);
        h.assertTrue(p.getOffhandItem().isEmpty()&&menu.getSlot(1).getItem().getCount()==2,"Closing leaves logs on the table and frees the hand");
        var planks=new ItemStack(Items.OAK_PLANKS,8);h.assertTrue(BlockCarry.insert(p.getInventory(),planks)==8,"A free hand can now pick up planks");
        var next=open(h,pos,p);next.clicked(46,0,ClickType.PICKUP,p);next.clicked(2,0,ClickType.PICKUP,p);next.removed(p);
        var other=player(h,pos);var shared=open(h,pos,other);
        h.assertTrue(shared.getSlot(1).getItem().is(Items.OAK_LOG)&&shared.getSlot(1).getItem().getCount()==2
                &&shared.getSlot(2).getItem().is(Items.OAK_PLANKS)&&shared.getSlot(2).getItem().getCount()==8,"Another player sees both staged stacks after reopening");
        shared.removed(other);h.succeed();
    }
    @GameTest(template="industrial") public static void bothViewersRefreshAndCannotTakeTheSameCraftTwice(GameTestHelper h){
        var pos=table(h);var a=player(h,pos);var b=player(h,pos);var first=open(h,pos,a);var second=open(h,pos,b);
        first.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        h.assertTrue(second.getSlot(1).getItem().is(Items.OAK_LOG)&&first.getSlot(0).getItem().getCount()==1&&second.getSlot(0).getItem().getCount()==1,"Both viewers receive the real shared input and recipe");
        first.clicked(0,0,ClickType.PICKUP,a);second.clicked(0,0,ClickType.PICKUP,b);
        h.assertTrue(first.getCarried().is(Items.OAK_PLANKS)&&first.getCarried().getCount()==1&&second.getCarried().isEmpty()
                &&second.getSlot(0).getItem().isEmpty()&&second.getSlot(1).getItem().isEmpty(),"One log produces exactly the configured one plank, never a stale second output");
        first.removed(a);second.removed(b);h.succeed();
    }
    @GameTest(template="industrial") public static void savedGridKeepsSlotsCountsAndItemComponents(GameTestHelper h){
        var pos=table(h);var p=player(h,pos);var menu=open(h,pos,p);
        var named=new ItemStack(Items.STICK,7);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Shared supplies"));
        menu.getSlot(9).set(named);menu.getSlot(4).set(new ItemStack(Items.OAK_LOG,12));menu.removed(p);
        var original=SharedCraftingTables.get(h.getLevel());var copy=SharedCraftingTables.load(original.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var restored=copy.open(h.getLevel(),pos,new CraftingMenu(3,p.getInventory()),p);
        h.assertTrue(restored.getItem(8).getCount()==7&&ItemStack.isSameItemSameComponents(named,restored.getItem(8))&&restored.getItem(3).getCount()==12,"World data round trip retains exact slots, counts and components");
        restored.close();h.succeed();
    }
    @GameTest(template="industrial") public static void removingTableDropsInputsOnceAndInvalidatesOpenMenus(GameTestHelper h){
        var pos=table(h);var p=player(h,pos);var menu=open(h,pos,p);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG,3));menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS,8));
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        int logs=drops.stream().filter(e->e.getItem().is(Items.OAK_LOG)).mapToInt(e->e.getItem().getCount()).sum();
        int planks=drops.stream().filter(e->e.getItem().is(Items.OAK_PLANKS)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(logs==3&&planks==8&&!menu.stillValid(p)&&menu.getSlot(1).getItem().isEmpty(),"Removal spills exact inputs and clears all old references");
        menu.removed(p);SharedCraftingTables.get(h.getLevel()).removed(h.getLevel(),pos);
        h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==11,"Repeated removal/close cannot duplicate inputs");
        h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());var replacement=open(h,pos,p);
        h.assertTrue(replacement.getSlot(1).getItem().isEmpty()&&replacement.getSlot(2).getItem().isEmpty(),"A new table does not inherit removed stock");replacement.removed(p);h.succeed();
    }
    @GameTest(template="industrial") public static void tablesStaySeparateAndReopeningRestoresRecipe(GameTestHelper h){
        var pos=table(h);var other=pos.offset(2,0,0);h.getLevel().setBlockAndUpdate(other,Blocks.CRAFTING_TABLE.defaultBlockState());
        var p=player(h,pos);var first=open(h,pos,p);first.getSlot(1).set(new ItemStack(Items.OAK_LOG,2));first.removed(p);
        var second=open(h,other,p);h.assertTrue(second.getSlot(1).getItem().isEmpty()&&second.getSlot(0).getItem().isEmpty(),"Nearby tables have separate stock");second.removed(p);
        var reopened=open(h,pos,p);h.assertTrue(reopened.getSlot(0).getItem().is(Items.OAK_PLANKS)&&reopened.getSlot(0).getItem().getCount()==1,"An existing recipe is available immediately on reopen");reopened.removed(p);h.succeed();
    }
    @GameTest(template="industrial") public static void revokingClaimAccessKeepsTheSharedStock(GameTestHelper h){
        var pos=table(h);var p=player(h,pos);var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();
        c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),pos);c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();c.whitelist.put(p.getUUID(),"crafter");data.claims.put(c.at,c);data.rebuildIndex();
        try{
            var menu=open(h,pos,p);menu.getSlot(1).set(new ItemStack(Items.OAK_LOG,5));c.whitelist.clear();
            h.assertTrue(!CivicAccess.container(menu.getSlot(1).container,p),"Open grids respect revoked claim access");
            menu.clicked(1,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(1).getItem().getCount()==5,"Denied clicks leave shared ingredients on the table");
        }finally{data.claims.remove(c.at);data.rebuildIndex();}h.succeed();
    }
}

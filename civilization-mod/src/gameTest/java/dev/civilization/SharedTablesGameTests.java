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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class SharedTablesGameTests {
    private static BlockPos table(GameTestHelper h){var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());return pos;}
    private static BlockPos enchantingTable(GameTestHelper h){var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,Blocks.ENCHANTING_TABLE.defaultBlockState());return pos;}
    private static EnchantmentMenu enchanting(GameTestHelper h,BlockPos pos,FakePlayer player){
        var menu=new EnchantmentMenu(1,player.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));player.containerMenu=menu;return menu;
    }
    /** A live claim on the table whose whitelist lets the member in. */
    private static CivicData.Claim claim(GameTestHelper h,BlockPos pos,FakePlayer member){
        var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();
        c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),pos);c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();c.whitelist.put(member.getUUID(),"crafter");data.claims.put(c.at,c);data.rebuildIndex();
        return c;
    }
    private static void unclaim(GameTestHelper h,CivicData.Claim c){var data=CivicData.get(h.getLevel().getServer());data.claims.remove(c.at);data.rebuildIndex();}
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
        var original=SharedTables.get(h.getLevel(),SharedTables.Kind.CRAFTING);var copy=SharedTables.load(SharedTables.Kind.CRAFTING,original.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
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
        menu.removed(p);SharedTables.get(h.getLevel(),SharedTables.Kind.CRAFTING).removed(h.getLevel(),pos);
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
        var pos=table(h);var p=player(h,pos);var c=claim(h,pos,p);
        try{
            var menu=open(h,pos,p);menu.getSlot(1).set(new ItemStack(Items.OAK_LOG,5));c.whitelist.clear();
            h.assertTrue(!CivicAccess.container(menu.getSlot(1).container,p),"Open grids respect revoked claim access");
            menu.clicked(1,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(1).getItem().getCount()==5,"Denied clicks leave shared ingredients on the table");
        }finally{unclaim(h,c);}h.succeed();
    }
    @GameTest(template="industrial") public static void enchantingTableKeepsItsItemAndLapisForEveryone(GameTestHelper h){
        var pos=enchantingTable(h);var a=player(h,pos);var menu=enchanting(h,pos,a);
        a.getInventory().setItem(0,new ItemStack(Items.LAPIS_LAZULI,10));a.getInventory().setItem(1,new ItemStack(Items.BOOK));
        int hotbar=menu.slots.size()-9;menu.quickMoveStack(a,hotbar);menu.quickMoveStack(a,hotbar+1);
        h.assertTrue(menu.getSlot(0).getItem().is(Items.BOOK)&&menu.getSlot(1).getItem().getCount()==10,"Shift-click puts the book and all ten lapis on the table");
        menu.removed(a);
        h.assertTrue(a.getInventory().countItem(Items.LAPIS_LAZULI)==0&&a.getInventory().countItem(Items.BOOK)==0,"Closing leaves the book and lapis on the table, not back in the pockets");
        var b=player(h,pos);var other=enchanting(h,pos,b);
        h.assertTrue(other.getSlot(0).getItem().is(Items.BOOK)&&other.getSlot(1).getItem().getCount()==10&&other.costs[0]>0,"Another player sees the same book and lapis, with enchantments on offer at once");
        other.removed(b);h.succeed();
    }
    @GameTest(template="industrial") public static void anEnchantmentSpendsTheSharedLapisOnce(GameTestHelper h){
        var pos=enchantingTable(h);var a=player(h,pos);var b=player(h,pos);var first=enchanting(h,pos,a);var second=enchanting(h,pos,b);
        first.getSlot(0).set(new ItemStack(Items.BOOK));first.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));a.experienceLevel=30;b.experienceLevel=30;
        h.assertTrue(first.costs[0]>0&&second.costs[0]>0,"Both viewers are offered enchantments for the shared book");
        h.assertTrue(first.clickMenuButton(a,0),"The first viewer's choice enchants");
        h.assertTrue(second.getSlot(0).getItem().is(Items.ENCHANTED_BOOK)&&second.getSlot(1).getItem().getCount()==2,"The other viewer sees the enchanted book and one lapis spent: "+second.getSlot(1).getItem().getCount());
        h.assertTrue(!second.clickMenuButton(b,0)&&second.getSlot(1).getItem().getCount()==2,"The enchanted book cannot be enchanted again from the other viewer's offer");
        first.removed(a);second.removed(b);h.succeed();
    }
    @GameTest(template="industrial") public static void savedEnchantingTableKeepsItsItemAndLapis(GameTestHelper h){
        var pos=enchantingTable(h);var p=player(h,pos);var menu=enchanting(h,pos,p);
        var sword=new ItemStack(Items.IRON_SWORD);sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Edge"));
        menu.getSlot(0).set(sword);menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,17));menu.removed(p);
        var original=SharedTables.get(h.getLevel(),SharedTables.Kind.ENCHANTING);
        var copy=SharedTables.load(SharedTables.Kind.ENCHANTING,original.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var restored=copy.open(h.getLevel(),pos,new EnchantmentMenu(3,p.getInventory()),p);
        h.assertTrue(ItemStack.isSameItemSameComponents(sword,restored.getItem(0))&&restored.getItem(1).is(Items.LAPIS_LAZULI)&&restored.getItem(1).getCount()==17,"World data round trip keeps the named sword and seventeen lapis");
        restored.close();h.succeed();
    }
    @GameTest(template="industrial") public static void breakingEnchantingTableDropsItsSlotsOnce(GameTestHelper h){
        var pos=enchantingTable(h);var p=player(h,pos);var menu=enchanting(h,pos,p);
        menu.getSlot(0).set(new ItemStack(Items.BOOK));menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,9));
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());menu.removed(p);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        int lapis=drops.stream().filter(e->e.getItem().is(Items.LAPIS_LAZULI)).mapToInt(e->e.getItem().getCount()).sum();
        int books=drops.stream().filter(e->e.getItem().is(Items.BOOK)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(lapis==9&&books==1&&!menu.stillValid(p)&&p.getInventory().countItem(Items.LAPIS_LAZULI)==0,"Breaking drops the book and nine lapis once, none back to the viewer");
        h.getLevel().setBlockAndUpdate(pos,Blocks.ENCHANTING_TABLE.defaultBlockState());var replacement=enchanting(h,pos,p);
        h.assertTrue(replacement.getSlot(0).getItem().isEmpty()&&replacement.getSlot(1).getItem().isEmpty(),"A new table does not inherit removed stock");replacement.removed(p);h.succeed();
    }
    @GameTest(template="industrial") public static void revokedAccessCannotSpendTheSharedLapis(GameTestHelper h){
        var pos=enchantingTable(h);var p=player(h,pos);var c=claim(h,pos,p);
        try{
            var menu=enchanting(h,pos,p);menu.getSlot(0).set(new ItemStack(Items.BOOK));menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));p.experienceLevel=30;
            h.assertTrue(menu.costs[0]>0,"A member is offered enchantments");
            c.whitelist.clear();
            h.assertTrue(!menu.clickMenuButton(p,0)&&menu.getSlot(0).getItem().is(Items.BOOK)&&menu.getSlot(1).getItem().getCount()==3,"A denied enchantment spends no lapis and leaves the book");
            menu.clicked(1,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(1).getItem().getCount()==3,"A denied click leaves the lapis on the table");
        }finally{unclaim(h,c);}h.succeed();
    }
    /** A grid holding a Civilization recipe's own ingredients must craft that recipe, never another one that shares its shape. */
    @GameTest(template="empty") public static void everyCraftingRecipeResolvesToItself(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        for(var holder:manager.getAllRecipesFor(RecipeType.CRAFTING)){
            var recipe=holder.value();
            if(!holder.id().getNamespace().equals("civilization"))continue;
            int width;
            if(recipe.getClass()==ShapedRecipe.class)width=((ShapedRecipe)recipe).getWidth();
            else if(recipe.getClass()==ShapelessRecipe.class)width=Math.min(3,recipe.getIngredients().size());
            else continue;
            var items=new java.util.ArrayList<ItemStack>();
            for(var ingredient:recipe.getIngredients())items.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(1));
            while(items.size()%width!=0)items.add(ItemStack.EMPTY);
            var match=manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(width,items.size()/width,items),h.getLevel());
            h.assertTrue(match.isPresent()&&match.get().id().equals(holder.id()),holder.id()+" crafts "+match.map(m->m.id().toString()).orElse("nothing"));
        }
        h.succeed();
    }
}

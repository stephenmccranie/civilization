package dev.civilization;

import dev.civilization.client.jei.CivilizationJei;
import mezz.jei.api.constants.*;
import mezz.jei.api.recipe.*;
import net.minecraft.client.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Real JEI registration, output/use lookup and rendered processing pages. */
final class JeiVisualCheck {
    private static int ticks;
    private static volatile Throwable failure;
    static void tick(Minecraft mc){
        if(CivilizationJei.runtime()==null)return;
        ticks++;if(failure!=null)throw new IllegalStateException("JEI scene",failure);
        var runtime=CivilizationJei.runtime();var manager=runtime.getRecipeManager();
        if(ticks==40){
            mc.options.hideGui=false;mc.options.guiScale().set(3);
            var types=List.of("kiln","foundry","fertilizer","cooking","tannery","textile","smithy","assembly","oil_derrick","heater","column","condenser","coal_drill","canisters","anvil","cutting","recombining");
            for(var type:types){
                var recipeType=CivilizationJei.type(type);var category=manager.getRecipeCategory(recipeType);
                if(category==null||manager.createRecipeLookup(recipeType).get().findAny().isEmpty())throw new IllegalStateException("Missing JEI recipes: "+type);
                if(manager.createRecipeCatalystLookup(recipeType).getItemStack().findAny().isEmpty())throw new IllegalStateException("Missing controller catalyst: "+type);
            }
            var iron=focus(RecipeIngredientRole.OUTPUT,new ItemStack(Items.IRON_INGOT));
            var hammered=manager.createRecipeLookup(CivilizationJei.type("anvil")).limitFocus(List.of(iron)).get().toList();
            if(hammered.stream().noneMatch(r->r.slots().stream().anyMatch(s->s.items().stream().anyMatch(i->i.is(KilnContent.IRON_BLOOM.get())))))throw new IllegalStateException("Iron lookup does not lead to hammering an iron bloom");
            var bloom=focus(RecipeIngredientRole.OUTPUT,KilnContent.IRON_BLOOM.toStack());
            var foundry=manager.createRecipeLookup(CivilizationJei.type("foundry")).limitFocus(List.of(bloom)).get().toList();
            if(foundry.stream().noneMatch(r->r.slots().stream().anyMatch(s->s.items().stream().anyMatch(i->i.is(Items.RAW_IRON)))))throw new IllegalStateException("Bloom lookup does not lead to raw-iron Foundry processing");
            if(manager.createRecipeLookup(RecipeTypes.SMELTING).limitFocus(List.of(iron)).get().findAny().isPresent())throw new IllegalStateException("Retired iron smelting is still shown");
            var raw=focus(RecipeIngredientRole.INPUT,new ItemStack(Items.RAW_IRON));
            if(manager.createRecipeLookup(CivilizationJei.type("foundry")).limitFocus(List.of(raw)).get().findAny().isEmpty())throw new IllegalStateException("Raw iron uses missing");
            long loaded=mc.level.getRecipeManager().getAllRecipesFor(KilnContent.FOUNDRY_RECIPE_TYPE.get()).size();
            if(manager.createRecipeLookup(CivilizationJei.type("foundry")).get().count()!=loaded)throw new IllegalStateException("Foundry JEI coverage differs from actual recipes");
            var pieces=CuttingContent.stack(net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(),1,1);
            var cuts=manager.createRecipeLookup(CivilizationJei.type("cutting")).limitFocus(List.of(focus(RecipeIngredientRole.OUTPUT,pieces))).get().toList();
            if(cuts.isEmpty()||cuts.stream().anyMatch(r->r.slots().stream().filter(s->s.role()==RecipeIngredientRole.OUTPUT).flatMap(s->s.items().stream()).anyMatch(s->!ItemStack.isSameItemSameComponents(s,pieces))))throw new IllegalStateException("Cut-piece material/shape subtypes mixed together");
            System.out.println("JEI check: all 17 categories, controller catalysts, iron recipes/uses, recipe parity and cut-piece identities passed.");
            runtime.getRecipesGui().show(iron);
        }
        if(ticks==50)showCategory("foundry",KilnContent.IRON_BLOOM.toStack());
        if(ticks==65)shot(mc,"iron-foundry");
        if(ticks==70)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,IndustrialContent.SULFUR.toStack()));
        if(ticks==90)shot(mc,"sulfur-column");
        if(ticks==95)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,new ItemStack(Items.IRON_PICKAXE)));
        if(ticks==100)showCategory("assembly",new ItemStack(Items.IRON_PICKAXE));
        if(ticks==115)shot(mc,"smithy");
        if(ticks==120)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,IndustrialContent.FUEL_CAN.toStack()));
        if(ticks==140)shot(mc,"canister");
        if(ticks==145)runtime.getRecipesGui().showTypes(List.of(CivilizationJei.type("cutting")));
        if(ticks==165)shot(mc,"cutting");
        // JEI's + on a crafting recipe places it through the game's own recipe-book path, from the Hand, with no JEI on the server.
        if(ticks==170){
            mc.setScreen(null);
            var server=mc.getSingleplayerServer();server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS,4));p.inventoryMenu.broadcastFullState();});
        }
        if(ticks==180)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==190){
            @SuppressWarnings("unchecked") var table=(net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>)mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("crafting_table")).orElseThrow();
            var category=manager.getRecipeCategory(RecipeTypes.CRAFTING);var menu=mc.player.inventoryMenu;
            var handler=runtime.getRecipeTransferManager().getRecipeTransferHandler(menu,category).orElseThrow(()->new IllegalStateException("No crafting transfer handler for the inventory"));
            if(!handler.getClass().getName().contains("CivilizationJei"))throw new IllegalStateException("JEI's own transfer handler, which needs server JEI, is still in use: "+handler.getClass());
            var view=manager.createRecipeLayoutDrawable(category,table,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow().getRecipeSlotsView();
            var error=handler.transferRecipe(menu,table,view,mc.player,false,true);
            if(error!=null)throw new IllegalStateException("The crafting transfer was refused: "+error.getType());
        }
        if(ticks==205){
            var server=mc.getSingleplayerServer();server.execute(()->{try{var p=server.getPlayerList().getPlayers().getFirst();int placed=0;
                for(int i=1;i<5;i++)if(p.inventoryMenu.getSlot(i).getItem().is(Items.OAK_PLANKS))placed+=p.inventoryMenu.getSlot(i).getItem().getCount();
                if(placed!=4||!p.getOffhandItem().isEmpty())throw new IllegalStateException("JEI's + did not move the Hand's four planks into the grid: "+placed);
                System.out.println("JEI check: the + button places crafting recipes without JEI on the server.");}catch(Throwable e){failure=e;}});
        }
        if(ticks==210)shot(mc,"transfer");
        if(ticks>215)mc.stop();
    }
    private static void showCategory(String name,ItemStack output){
        var runtime=CivilizationJei.runtime();var manager=runtime.getRecipeManager();var focus=focus(RecipeIngredientRole.OUTPUT,output);var type=CivilizationJei.type(name);
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(type),manager.createRecipeLookup(type).limitFocus(List.of(focus)).get().toList(),List.of(focus));
    }
    private static IFocus<ItemStack> focus(RecipeIngredientRole role,ItemStack stack){return CivilizationJei.runtime().getJeiHelpers().getFocusFactory().createFocus(role,VanillaTypes.ITEM_STACK,stack);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"jei-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}

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
    static void tick(Minecraft mc){
        if(CivilizationJei.runtime()==null)return;
        ticks++;
        var runtime=CivilizationJei.runtime();var manager=runtime.getRecipeManager();
        if(ticks==40){
            mc.options.hideGui=false;mc.options.guiScale().set(3);
            var types=List.of("kiln","foundry","fertilizer","cooking","tannery","textile","smithy","oil_pump","heater","column","condenser","coal_drill","canisters","cutting","recombining");
            for(var type:types){
                var recipeType=CivilizationJei.type(type);var category=manager.getRecipeCategory(recipeType);
                if(category==null||manager.createRecipeLookup(recipeType).get().findAny().isEmpty())throw new IllegalStateException("Missing JEI recipes: "+type);
                if(manager.createRecipeCatalystLookup(recipeType).getItemStack().findAny().isEmpty())throw new IllegalStateException("Missing controller catalyst: "+type);
            }
            var iron=focus(RecipeIngredientRole.OUTPUT,new ItemStack(Items.IRON_INGOT));
            var foundry=manager.createRecipeLookup(CivilizationJei.type("foundry")).limitFocus(List.of(iron)).get().toList();
            if(foundry.stream().noneMatch(r->r.slots().stream().anyMatch(s->s.items().stream().anyMatch(i->i.is(Items.RAW_IRON)))))throw new IllegalStateException("Iron lookup does not lead to raw-iron Foundry processing");
            if(manager.createRecipeLookup(RecipeTypes.SMELTING).limitFocus(List.of(iron)).get().findAny().isPresent())throw new IllegalStateException("Retired iron smelting is still shown");
            var raw=focus(RecipeIngredientRole.INPUT,new ItemStack(Items.RAW_IRON));
            if(manager.createRecipeLookup(CivilizationJei.type("foundry")).limitFocus(List.of(raw)).get().findAny().isEmpty())throw new IllegalStateException("Raw iron uses missing");
            long loaded=mc.level.getRecipeManager().getAllRecipesFor(KilnContent.FOUNDRY_RECIPE_TYPE.get()).size();
            if(manager.createRecipeLookup(CivilizationJei.type("foundry")).get().count()!=loaded)throw new IllegalStateException("Foundry JEI coverage differs from actual recipes");
            var pieces=CuttingContent.stack(net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(),1,1);
            var cuts=manager.createRecipeLookup(CivilizationJei.type("cutting")).limitFocus(List.of(focus(RecipeIngredientRole.OUTPUT,pieces))).get().toList();
            if(cuts.isEmpty()||cuts.stream().anyMatch(r->r.slots().stream().filter(s->s.role()==RecipeIngredientRole.OUTPUT).flatMap(s->s.items().stream()).anyMatch(s->!ItemStack.isSameItemSameComponents(s,pieces))))throw new IllegalStateException("Cut-piece material/shape subtypes mixed together");
            System.out.println("JEI check: all 15 categories, controller catalysts, iron recipes/uses, recipe parity and cut-piece identities passed.");
            runtime.getRecipesGui().show(iron);
        }
        if(ticks==50)showCategory("foundry",new ItemStack(Items.IRON_INGOT));
        if(ticks==65)shot(mc,"iron-foundry");
        if(ticks==70)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,IndustrialContent.SULFUR.toStack()));
        if(ticks==90)shot(mc,"sulfur-column");
        if(ticks==95)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,new ItemStack(Items.IRON_PICKAXE)));
        if(ticks==100)showCategory("smithy",new ItemStack(Items.IRON_PICKAXE));
        if(ticks==115)shot(mc,"smithy");
        if(ticks==120)runtime.getRecipesGui().show(focus(RecipeIngredientRole.OUTPUT,IndustrialContent.FUEL_CAN.toStack()));
        if(ticks==140)shot(mc,"canister");
        if(ticks==145)runtime.getRecipesGui().showTypes(List.of(CivilizationJei.type("cutting")));
        if(ticks==165)shot(mc,"cutting");
        if(ticks>175)mc.stop();
    }
    private static void showCategory(String name,ItemStack output){
        var runtime=CivilizationJei.runtime();var manager=runtime.getRecipeManager();var focus=focus(RecipeIngredientRole.OUTPUT,output);var type=CivilizationJei.type(name);
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(type),manager.createRecipeLookup(type).limitFocus(List.of(focus)).get().toList(),List.of(focus));
    }
    private static IFocus<ItemStack> focus(RecipeIngredientRole role,ItemStack stack){return CivilizationJei.runtime().getJeiHelpers().getFocusFactory().createFocus(role,VanillaTypes.ITEM_STACK,stack);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"jei-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}

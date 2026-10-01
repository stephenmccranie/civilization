package dev.civilization;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public final class RecombiningGameTests {
    @GameTest(template="industrial") public static void everyPairOfGridSlotsRecombinesExactlyOneLargerPiece(GameTestHelper h) {
        for(var material:List.of(Blocks.BRICKS,Blocks.STONE_BRICKS,Blocks.COPPER_BLOCK,Blocks.OAK_PLANKS,Blocks.BAMBOO_PLANKS,Blocks.PURPUR_BLOCK))
            for(int units:new int[]{3,1,2})for(int size:new int[]{2,3})for(int a=0;a<size*size;a++)for(int b=a+1;b<size*size;b++) {
                var slots=new ArrayList<ItemStack>(Collections.nCopies(size*size,ItemStack.EMPTY));
                slots.set(a,CuttingContent.stack(material.defaultBlockState(),units,32));
                slots.set(b,CuttingContent.stack(material.defaultBlockState(),units,7));
                var input=CraftingInput.of(size,size,slots);
                var found=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
                h.assertTrue(found.isPresent(),"Recombining recipe found in any slots");
                var output=found.orElseThrow().value().assemble(input,h.getLevel().registryAccess());
                var expected=units==2?new ItemStack(material):CuttingContent.stack(material.defaultBlockState(),units==3?1:2,1);
                h.assertTrue(output.getCount()==1 && ItemStack.isSameItemSameComponents(output,expected),"One larger piece, never chiseled variant or stack-multiplied yield");
                h.assertTrue(found.get().value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Both ingredients consumed without returning duplicate pieces");
                h.assertTrue(slots.get(a).getCount()==32 && slots.get(b).getCount()==7,"Preview must not mutate ingredient stacks");
            }
        h.succeed();
    }
    @GameTest(template="industrial") public static void mismatchesAndExtraIngredientsDoNotRecombine(GameTestHelper h) {
        var recipe=new RecombiningRecipe(CraftingBookCategory.BUILDING);
        var beam=CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),1,2);
        for(var other:List.of(CuttingContent.stack(Blocks.STONE_BRICKS.defaultBlockState(),1,1),CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,1),new ItemStack(Items.BRICKS),CuttingContent.IRON_SAW.toStack()))
            h.assertTrue(!recipe.matches(CraftingInput.of(2,1,List.of(beam,other)),h.getLevel()),"Different size/material or a tool cannot be recombined");
        h.assertTrue(!recipe.matches(CraftingInput.of(1,1,List.of(beam)),h.getLevel()),"Two stacked items in one slot are not two ingredients");
        h.assertTrue(!recipe.matches(CraftingInput.of(3,1,List.of(beam,beam,beam)),h.getLevel()),"Extra ingredients rejected");
        h.succeed();
    }
    @GameTest(template="industrial") public static void legacyAndVanillaHalvesMixAndChiselingRemainsAvailable(GameTestHelper h) {
        var legacy=new ItemStack(CuttingContent.ITEM.get());var tag=new net.minecraft.nbt.CompoundTag();tag.putString("material","minecraft:stone_bricks");tag.putInt("units",2);
        legacy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var mixed=CraftingInput.of(2,1,List.of(legacy,new ItemStack(Items.STONE_BRICK_SLAB)));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,mixed,h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(mixed,h.getLevel().registryAccess()).is(Items.STONE_BRICKS),"Old and native slab representations recombine together");
        var whole=CraftingInput.of(1,1,List.of(new ItemStack(Items.STONE_BRICKS)));
        var chisel=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,whole,h.getLevel()).orElseThrow();
        h.assertTrue(chisel.value().assemble(whole,h.getLevel().registryAccess()).is(Items.CHISELED_STONE_BRICKS),"Chiseled variant still craftable from equivalent full-block input");
        for(var pair:List.of(List.of(Items.BAMBOO_PLANKS,Items.BAMBOO_MOSAIC),List.of(Items.PURPUR_BLOCK,Items.PURPUR_PILLAR))) {
            var input=CraftingInput.of(1,1,List.of(new ItemStack(pair.get(0))));
            var decorative=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
            h.assertTrue(decorative.value().assemble(input,h.getLevel().registryAccess()).is(pair.get(1)),"Other two-slab decorative recipes use their full source block");
        }
        h.succeed();
    }
}

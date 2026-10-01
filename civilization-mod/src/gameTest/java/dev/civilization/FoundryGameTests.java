package dev.civilization;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public class FoundryGameTests {
    private static FoundryBlockEntity build(GameTestHelper h,Direction facing) {
        var pos=h.absolutePos(new BlockPos(4,1,2));
        h.getLevel().setBlockAndUpdate(pos,KilnContent.FOUNDRY.get().defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING,facing));
        for(var p:MachineStructure.parts(h.getLevel().getBlockState(pos)))MachineStructure.placePart(h.getLevel(),pos,facing,p);
        return (FoundryBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void run(GameTestHelper h,FoundryBlockEntity f,int ticks) {CoalFireFixture.light(f);for(int i=0;i<ticks;i++)KilnBlockEntity.tick(h.getLevel(),f.getBlockPos(),f.getBlockState(),f);}
    @GameTest(template="industrial") public static void oreSteelAndPartsProgression(GameTestHelper h) {
        var f=build(h,Direction.NORTH);f.setItem(0,new ItemStack(Items.RAW_IRON));f.setItem(1,KilnContent.MINERAL_COAL.toStack());run(h,f,200);
        h.assertTrue(f.getItem(2).is(Items.IRON_INGOT),"Foundry smelts raw iron");
        f.setItem(0,f.removeItemNoUpdate(2));f.setItem(1,KilnContent.MINERAL_COAL.toStack(2));run(h,f,800);
        h.assertTrue(f.getItem(2).is(KilnContent.STEEL.get())&&f.getItem(2).getCount()==1,"Longer coal-fired batch makes one steel");
        var input=CraftingInput.of(2,2,List.of(f.getItem(2).copy(),new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),KilnContent.STEEL.toStack()));
        var result=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(KilnContent.MACHINE_PARTS.get())&&result.getCount()==2,"Steel and copper make shared parts");h.succeed();
    }
    @GameTest(template="industrial") public static void vanillaMetalSmeltingCannotBypassFoundry(GameTestHelper h) {
        for(var item:List.of(Items.RAW_IRON,Items.RAW_COPPER,Items.RAW_GOLD,Items.IRON_ORE,Items.DEEPSLATE_IRON_ORE,Items.COPPER_ORE,Items.DEEPSLATE_COPPER_ORE,Items.GOLD_ORE,Items.DEEPSLATE_GOLD_ORE,Items.NETHER_GOLD_ORE,Items.IRON_SWORD,Items.GOLDEN_SWORD))
            for(var type:List.of(RecipeType.SMELTING,RecipeType.BLASTING))h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(type,new SingleRecipeInput(new ItemStack(item)),h.getLevel()).isEmpty(),"Vanilla metal bypass: "+item+" / "+type);
        h.succeed();
    }
    @GameTest(template="industrial") public static void foundryRotationFuelAndBrokenShell(GameTestHelper h) {
        for(var d:Direction.Plane.HORIZONTAL){var f=build(h,d);h.assertTrue(f.checkStructure().status()==MachineStructure.COMPLETE,"Rotated foundry complete: "+d);}
        var f=build(h,Direction.NORTH);h.assertTrue(!f.canPlaceItem(1,new ItemStack(Items.CHARCOAL)),"No biomass fuel");
        f.setItem(0,new ItemStack(Items.RAW_COPPER));f.setItem(1,KilnContent.MINERAL_COAL.toStack());run(h,f,100);
        h.getLevel().removeBlock(f.getBlockPos().east(),false);run(h,f,300);
        h.assertTrue(f.getItem(2).isEmpty()&&f.getItem(0).is(Items.RAW_COPPER),"Broken shell cannot finish a batch");h.succeed();
    }
}

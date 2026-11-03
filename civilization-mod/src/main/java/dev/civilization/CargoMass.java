package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Cargo holds what the hand carries, by the space each piece takes. Unknown goods cannot enter as zero volume. */
public final class CargoMass {
    private CargoMass(){}
    public static BlockState material(ItemStack stack){return CuttingContent.material(stack);}
    /**
     * Space in blocks: slabs and saw-cut pieces their share of a block, and every other block the hand carries one
     * block, such as stairs, doors, chests and machines. NaN for goods cargo refuses: anything the hand does not
     * carry, filled containers and saved block data, so nothing travels hidden inside a store.
     */
    public static double volume(ItemStack stack){
        if(!BlockCarry.block(stack)||BlockCarry.filledContainer(stack)||stack.has(DataComponents.BLOCK_ENTITY_DATA))return Double.NaN;
        if(stack.is(CuttingContent.ITEM.get())){
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            var id=ResourceLocation.tryParse(tag.getString("material"));int units=tag.getInt("units");
            if(id==null||!BuiltInRegistries.BLOCK.containsKey(id)||(units!=1&&units!=2&&units!=3))return Double.NaN;
            var state=material(stack);
            if(state.isAir()||state.hasBlockEntity()||!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,BlockPos.ZERO))return Double.NaN;
        }
        return CuttingContent.piece(stack)?CuttingContent.volume(CuttingContent.units(stack)):1;
    }
}

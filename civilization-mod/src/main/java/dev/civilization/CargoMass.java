package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Plain solid building blocks and audited cut pieces. Unknown goods cannot enter as zero mass. */
public final class CargoMass {
    private CargoMass(){}
    public static BlockState material(ItemStack stack){return CuttingContent.material(stack);}
    public static double unitKg(ItemStack stack){
        if(stack.isEmpty()||stack.has(DataComponents.BLOCK_ENTITY_DATA)||stack.has(DataComponents.CONTAINER))return Double.NaN;
        if(stack.is(CuttingContent.ITEM.get())){
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            var id=ResourceLocation.tryParse(tag.getString("material"));int units=tag.getInt("units");
            if(id==null||!BuiltInRegistries.BLOCK.containsKey(id)||(units!=1&&units!=2&&units!=3))return Double.NaN;
        }else if(!(stack.getItem() instanceof BlockItem))return Double.NaN;
        var state=material(stack);
        if(state.isAir()||state.hasBlockEntity()||state.getBlock() instanceof TntBlock
                ||!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,BlockPos.ZERO))return Double.NaN;
        double volume=CuttingContent.piece(stack)?CuttingContent.volume(CuttingContent.units(stack)):1;
        return PhysicalMaterials.of(state).density()*volume;
    }
}

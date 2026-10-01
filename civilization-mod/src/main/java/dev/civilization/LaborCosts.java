package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class LaborCosts {
    public static final TagKey<Block> LIGHT = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilization", "light_vegetation"));
    private LaborCosts() {}
    public static double breaking(BlockState state) {
        if (state.getBlock() instanceof CropBlock) return CalorieConfig.HARVEST.get();
        return state.is(LIGHT) ? CalorieConfig.LIGHT_BREAK.get() : CalorieConfig.BREAK.get();
    }
    public static double placing(BlockState state) {
        return state.getBlock() instanceof CropBlock ? CalorieConfig.PLANT.get() : CalorieConfig.PLACE.get();
    }
    public static double placing(BlockState placed,BlockState replaced) {
        if(placed.getBlock() instanceof SlabBlock slab) {
            if(placed.getValue(SlabBlock.TYPE)!=SlabType.DOUBLE || replaced.getBlock()==slab)
                return CalorieConfig.PLACE.get()*.5;
        }
        if(replaced.getBlock() instanceof SlabBlock slab && replaced.getValue(SlabBlock.TYPE)!=SlabType.DOUBLE
                && SlabIntegration.base(slab)==placed.getBlock())return CalorieConfig.PLACE.get()*.5;
        return placing(placed);
    }
    public static double sleeping(long dayTicks) {
        return Math.max(0,dayTicks)*CalorieConfig.SLEEP.get()/1000.0;
    }
}

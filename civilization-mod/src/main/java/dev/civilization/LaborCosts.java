package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

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
}

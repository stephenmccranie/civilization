package dev.civilization;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Tiny scattered finds; rarity and suitable surface biomes are handled by placement modifiers. */
public final class UraniumPocketFeature extends Feature<OreConfiguration> {
    public UraniumPocketFeature(){super(OreConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context){
        // The vanilla vein ellipsoids can contain no block centers at size two.
        // Scattered ore keeps the small pocket size but actually attempts host blocks.
        return Feature.SCATTERED_ORE.place(context);
    }
}

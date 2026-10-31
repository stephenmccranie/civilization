package dev.civilization;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Small recoverable veins occur together in rare districts, not uniformly across mineral country. */
public final class RegionalUraniumFeature extends Feature<OreConfiguration> {
    public RegionalUraniumFeature(){super(OreConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context){
        var p=context.origin();
        // The vanilla vein ellipsoids can contain no block centers at size two.
        // Scattered ore keeps the small pocket size but actually attempts host blocks.
        return DepositDistribution.uranium(context.level().getSeed(),p.getX(),p.getZ())&&Feature.SCATTERED_ORE.place(context);
    }
}

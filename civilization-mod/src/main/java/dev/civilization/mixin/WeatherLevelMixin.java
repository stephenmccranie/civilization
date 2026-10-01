package dev.civilization.mixin;
import dev.civilization.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class WeatherLevelMixin {
 @Inject(method="isRainingAt",at=@At("HEAD"),cancellable=true)
 private void localRain(BlockPos p,CallbackInfoReturnable<Boolean> ci){var l=(Level)(Object)this;if(!RegionalWeather.enabled(l))return;boolean wet=l instanceof ServerLevel s?RegionalWeather.raining(s,p):dev.civilization.client.LocalWeather.raining(l,p);ci.setReturnValue(wet&&l.hasChunkAt(p)&&l.canSeeSky(p)&&l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,p).getY()<=p.getY()&&l.getBiome(p).value().getPrecipitationAt(p)==net.minecraft.world.level.biome.Biome.Precipitation.RAIN);}
}

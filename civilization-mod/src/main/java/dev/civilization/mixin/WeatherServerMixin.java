package dev.civilization.mixin;
import dev.civilization.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class WeatherServerMixin {
 @Inject(method="advanceWeatherCycle",at=@At("HEAD"),cancellable=true)
 private void weather(CallbackInfo ci){var l=(ServerLevel)(Object)this;if(RegionalWeather.enabled(l)){l.setWeatherParameters(0,0,false,false);l.setRainLevel(0);l.setThunderLevel(0);ci.cancel();}}
 @Redirect(method="tickPrecipitation",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;isRaining()Z"))
 private boolean precipitation(ServerLevel l,BlockPos p){return RegionalWeather.enabled(l)?RegionalWeather.raining(l,p):l.isRaining();}
 @Inject(method="tickChunk",at=@At("TAIL"))
 private void soil(LevelChunk chunk,int randomTicks,CallbackInfo ci){if(randomTicks>0)SoilSystem.tick((ServerLevel)(Object)this,chunk);}
}

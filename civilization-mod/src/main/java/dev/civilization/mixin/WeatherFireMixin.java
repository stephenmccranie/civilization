package dev.civilization.mixin;
import dev.civilization.RegionalWeather;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FireBlock.class)
public abstract class WeatherFireMixin {
 // The following vanilla isNearRain checks actual source/target positions.
 @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;isRaining()Z"))
 private boolean allowLocalCheck(ServerLevel l){return RegionalWeather.enabled(l)||l.isRaining();}
}

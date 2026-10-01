package dev.civilization.mixin;
import dev.civilization.*;
import dev.civilization.client.LocalWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class WeatherClientMixin {
 @Inject(method="getRainLevel",at=@At("HEAD"),cancellable=true)
 private void sky(float partial,CallbackInfoReturnable<Float> ci){var l=(Level)(Object)this;if(l.isClientSide&&RegionalWeather.enabled(l))ci.setReturnValue(LocalWeather.sky(l));}
}

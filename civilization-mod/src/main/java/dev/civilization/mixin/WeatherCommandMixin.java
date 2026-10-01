package dev.civilization.mixin;
import dev.civilization.*;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WeatherCommand.class)
public abstract class WeatherCommandMixin {
 @Inject(method="setRain",at=@At("HEAD"),cancellable=true)
 private static void rain(CommandSourceStack s,int duration,CallbackInfoReturnable<Integer> ci){if(!RegionalWeather.enabled(s.getLevel()))return;var p=BlockPos.containing(s.getPosition());boolean ok=RegionalWeather.get(s.getLevel()).call(s.getLevel(),p);s.sendSuccess(()->Component.literal((ok?"Called 30-minute regional rain. ":"Already raining. ")+"Duration argument does not override regional rules."),false);WeatherEvents.sync(s.getLevel());ci.setReturnValue(ok?1:0);}
 @Inject(method="setClear",at=@At("HEAD"),cancellable=true)
 private static void clear(CommandSourceStack s,int duration,CallbackInfoReturnable<Integer> ci){if(!RegionalWeather.enabled(s.getLevel()))return;RegionalWeather.get(s.getLevel()).clear(s.getLevel(),BlockPos.containing(s.getPosition()));s.sendSuccess(()->Component.literal("Cleared this weather district; next natural rain is random. Duration argument ignored."),false);WeatherEvents.sync(s.getLevel());ci.setReturnValue(1);}
 @Inject(method="setThunder",at=@At("HEAD"),cancellable=true)
 private static void thunder(CommandSourceStack s,int duration,CallbackInfoReturnable<Integer> ci){if(!RegionalWeather.enabled(s.getLevel()))return;s.sendFailure(Component.literal("Regional thunderstorms are disabled. Use /civilization weather rain."));ci.setReturnValue(0);}
}

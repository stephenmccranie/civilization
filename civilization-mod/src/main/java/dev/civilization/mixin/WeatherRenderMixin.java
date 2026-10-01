package dev.civilization.mixin;
import dev.civilization.*;
import dev.civilization.client.LocalWeather;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LevelRenderer.class)
public abstract class WeatherRenderMixin {
 @Redirect(method={"renderSnowAndRain","tickRain"},at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
 private float visible(ClientLevel l,float partial){return RegionalWeather.enabled(l)?LocalWeather.nearby(l):l.getRainLevel(partial);}
 @Redirect(method={"renderSnowAndRain","tickRain"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
 private Biome.Precipitation precipitation(Biome b,BlockPos p){var l=Minecraft.getInstance().level;return RegionalWeather.enabled(l)&&LocalWeather.strength(l,p)<=0?Biome.Precipitation.NONE:b.getPrecipitationAt(p);}
}

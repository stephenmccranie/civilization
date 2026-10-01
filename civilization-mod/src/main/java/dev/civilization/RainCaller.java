package dev.civilization;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
public final class RainCaller extends Item {
 public RainCaller(Properties p){super(p);}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(l instanceof ServerLevel s){String message=!RegionalWeather.enabled(l)?"Rain Caller works in the Overworld":RegionalWeather.get(s).call(s,p.blockPosition())?"Rain called | "+RegionalWeather.describe(s,p.blockPosition()):"Already raining | "+RegionalWeather.describe(s,p.blockPosition());p.displayClientMessage(Component.literal(message),true);WeatherEvents.sync(s);}return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
 @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("30 minutes of rain · 512-block district"));lines.add(Component.literal("Creative prototype"));}
}

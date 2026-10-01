package dev.civilization;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid="civilization")
public final class WeatherEvents {
 @SubscribeEvent public static void tick(LevelTickEvent.Post e){if(e.getLevel() instanceof ServerLevel l&&RegionalWeather.enabled(l)){var w=RegionalWeather.get(l);if(l.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)){w.clock++;w.setDirty();}if(l.getGameTime()%20==0)sync(l);}}
 public static void sync(ServerLevel l){if(!RegionalWeather.enabled(l))return;var w=RegionalWeather.get(l);for(var p:l.players()){int x=Math.floorDiv(p.getBlockX(),512),z=Math.floorDiv(p.getBlockZ(),512),bits=0;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)if(w.district(l,net.minecraft.world.level.ChunkPos.asLong(x+dx,z+dz)).rain)bits|=1<<((dz+1)*3+dx+1);PacketDistributor.sendToPlayer(p,new WeatherPayload(x,z,bits));}}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("civilization").then(Commands.literal("weather").executes(c->{c.getSource().sendSuccess(()->Component.literal(RegionalWeather.describe(c.getSource().getLevel(),net.minecraft.core.BlockPos.containing(c.getSource().getPosition()))),false);return 1;}).then(Commands.literal("rain").requires(s->s.hasPermission(2)).executes(c->change(c.getSource(),true))).then(Commands.literal("clear").requires(s->s.hasPermission(2)).executes(c->change(c.getSource(),false)))));}
 private static int change(net.minecraft.commands.CommandSourceStack s,boolean rain){var l=s.getLevel();if(!RegionalWeather.enabled(l)){s.sendFailure(Component.literal("Regional weather is Overworld-only"));return 0;}var p=net.minecraft.core.BlockPos.containing(s.getPosition());boolean changed=true;if(rain)changed=RegionalWeather.get(l).call(l,p);else RegionalWeather.get(l).clear(l,p);s.sendSuccess(()->Component.literal(RegionalWeather.describe(l,p)),false);sync(l);return changed?1:0;}
}

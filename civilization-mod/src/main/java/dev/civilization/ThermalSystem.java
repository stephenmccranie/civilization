package dev.civilization;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid="civilization")
public final class ThermalSystem {
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){e.getDispatcher().register(net.minecraft.commands.Commands.literal("civilization").then(net.minecraft.commands.Commands.literal("heat").executes(c->{if(!ThermalConfig.enabled()){c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Heat simulation is disabled for this instance."),false);return 1;}var l=c.getSource().getLevel();var p=BlockPos.containing(c.getSource().getPosition());double air=airTemperature(l,c.getSource().getPosition());var player=c.getSource().getPlayer();double felt=player==null?air:currentPlayerTemperature(player,air);var field=ThermalField.get(l);c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"Air %.1f °F | Feels %.1f °F | Comfort %.0f%% | Thermal efficiency %.0f%% | Heat cells %,d | Solver age %.1fs | Queued sources %d",ThermalDisplay.fahrenheit(air),ThermalDisplay.fahrenheit(felt),ThermalRules.comfort(felt)*100,ThermalField.efficiency(l,p)*100,field.activeCells(),field.simulationAgeSeconds(l),field.queuedSources())),false);return 1;})));}
    private static final ResourceLocation SPEED=ResourceLocation.parse("civilization:thermal_comfort");
    private record PlayerHeat(ServerLevel level,long tick,double temperature){}
    private static final WeakHashMap<Player,PlayerHeat> PLAYER_HEAT=new WeakHashMap<>();
    public static double comfort(Player p){return Math.clamp(p.getPersistentData().getFloat("civilization_comfort"),0,1);}
    private static double currentPlayerTemperature(Player p,double fallback){var state=PLAYER_HEAT.get(p);return state==null?fallback:state.temperature();}
    static double updatePlayerTemperature(Player p,ServerLevel level,double air){
        var previous=PLAYER_HEAT.get(p);long now=level.getGameTime();
        double felt=previous==null||previous.level()!=level||now<=previous.tick()||now-previous.tick()>100
                ?air:ThermalRules.playerTemperature(previous.temperature(),air,(now-previous.tick())/20d);
        PLAYER_HEAT.put(p,new PlayerHeat(level,now,felt));return felt;
    }
    /** Sample the air around the player's torso, above slabs or other low ground blocks. */
    static double airTemperature(ServerLevel level,net.minecraft.world.phys.Vec3 feet){
        return ThermalField.temperature(level,BlockPos.containing(feet.add(0,1,0)));
    }
    static void chargeCold(Player p,double playerCelsius){
        if(!ThermalConfig.enabled()||!CalorieFoodData.active(p))return;
        var data=CalorieFoodData.of(p);
        if(data.reserve().calories()<=0)return;
        double cost=ThermalRules.coldKcalPerSecond(playerCelsius,CalorieConfig.COLD_EXPOSURE.get());
        if(cost>0)data.spendOther(p,cost,"cold_exposure",String.format(java.util.Locale.ROOT,"%.1f°F player",ThermalDisplay.fahrenheit(playerCelsius)));
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed e){var p=e.getEntity();if(!p.isCreative()&&!p.isSpectator()&&!CalorieFoodData.of(p).isDepleted())e.setNewSpeed((float)(e.getNewSpeed()*(1+.15*comfort(p))));}
    @SubscribeEvent public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof ServerLevel l))return;
        if(ThermalConfig.enabled()&&l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)){
            var field=ThermalField.get(l);
            BlockHeatSources.discover(l);
            BlockHeatSources.scheduledEmit(l,field);field.scheduledSources(l);
            field.scheduledStep(l,(int)(l.getGameTime()%5));
        }
        for(var p:l.players()) {
            // Spread player searches and survey packets across the same twenty-tick period.
            if(Math.floorMod(p.getId(),20)!=Math.floorMod(l.getGameTime(),20))continue;
            boolean supported=ThermalField.supported(l,p.blockPosition().above());
            float air=supported?(float)airTemperature(l,p.position()):18;
            float felt=supported?(float)updatePlayerTemperature(p,l,air):18;
            if(!supported)PLAYER_HEAT.remove(p);
            float c=supported?(float)ThermalRules.comfort(felt):0;
            if(supported)chargeCold(p,felt);
            p.getPersistentData().putFloat("civilization_comfort",c);
            var attr=p.getAttribute(Attributes.MOVEMENT_SPEED);attr.removeModifier(SPEED);
            if(!p.isCreative()&&!p.isSpectator()&&!CalorieFoodData.of(p).isDepleted()&&c>0)attr.addTransientModifier(new AttributeModifier(SPEED,.05*c,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            var list=new ArrayList<ThermalPayload.Sample>();
            // Survey updates are independent of the once-per-second comfort HUD.
            if(ThermalContent.wearing(p)&&supported&&Math.floorMod(p.getId(),40)==Math.floorMod(l.getGameTime(),40)){
                var base=p.blockPosition();
                // The view window follows the player; every sample is the actual integer world block cell.
                for(int x=-ThermalPayload.VIEW_RADIUS;x<=ThermalPayload.VIEW_RADIUS;x++)for(int y=-ThermalPayload.VIEW_RADIUS;y<=ThermalPayload.VIEW_RADIUS;y++)for(int z=-ThermalPayload.VIEW_RADIUS;z<=ThermalPayload.VIEW_RADIUS;z++){var at=base.offset(x,y,z);if(at.distSqr(base.above())<=100&&ThermalField.supported(l,at))list.add(new ThermalPayload.Sample(at,(float)ThermalField.temperature(l,at)));}
            }
            float solverAge=ThermalConfig.enabled()&&supported?(float)ThermalField.get(l).simulationAgeSeconds(l):0;
            PacketDistributor.sendToPlayer(p,new ThermalPayload(air,felt,c,solverAge,supported,List.copyOf(list)));
        }
    }
}

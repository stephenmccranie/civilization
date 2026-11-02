package dev.civilization;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Vanilla ping/pong probes and vanilla tab display names: no client extension required. */
@EventBusSubscriber(modid="civilization")
public final class TabLatency {
    private static final Map<ServerGamePacketListenerImpl,LatencyWindow> windows=new ConcurrentHashMap<>();
    private static long nextProbe;
    private TabLatency(){}
    public static int probe(ServerPlayer player,long now){
        var window=windows.computeIfAbsent(player.connection,key->new LatencyWindow(ThreadLocalRandom.current().nextInt()));
        int id=window.probe(now);player.connection.send(new ClientboundPingPacket(id));return id;
    }
    public static void pong(ServerGamePacketListenerImpl connection,int id,long received){
        var window=windows.get(connection);if(window!=null)window.reply(id,received);
    }
    public static LatencyWindow.Reading reading(ServerPlayer player,long now){
        var window=windows.get(player.connection);return window==null?new LatencyWindow.Reading(-1,false):window.reading(now);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        long now=Util.getMillis();if(now<nextProbe)return;nextProbe=now+LatencyWindow.PROBE_INTERVAL_MS;
        var active=new HashSet<ServerGamePacketListenerImpl>();
        for(var player:event.getServer().getPlayerList().getPlayers()){
            active.add(player.connection);probe(player,now);player.refreshTabListName();
        }
        windows.keySet().retainAll(active);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void name(PlayerEvent.TabListNameFormat event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var base=event.getDisplayName();
        if(base==null)base=PlayerTeam.formatNameForTeam(player.getTeam(),Component.literal(player.getGameProfile().getName()));
        event.setDisplayName(base.copy().append(Component.literal("  "+reading(player,Util.getMillis()).label()).withStyle(ChatFormatting.GRAY)));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player)windows.remove(player.connection);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){windows.clear();nextProbe=0;}
}

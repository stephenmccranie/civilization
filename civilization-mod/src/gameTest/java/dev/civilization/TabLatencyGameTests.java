package dev.civilization;

import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class TabLatencyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"latency-test"));p.setGameMode(GameType.SURVIVAL);return p;}
    @GameTest(template="industrial") public static void nativePongRecordsOnlyTheMatchingConnectionProbe(GameTestHelper h){
        var a=player(h);var b=player(h);long now=Util.getMillis();int id=TabLatency.probe(a,now);
        a.connection.handlePong(new ServerboundPongPacket(id));
        h.assertTrue(TabLatency.reading(a,Util.getMillis()).milliseconds()>=0&&TabLatency.reading(b,Util.getMillis()).milliseconds()==-1,"The native common-packet hook records a matched reply only for its connection");
        TabLatency.logout(new PlayerEvent.PlayerLoggedOutEvent(a));h.succeed();
    }
    @GameTest(template="industrial") public static void tabNamesPreserveCustomNamesAndShowUnknownUntilSampled(GameTestHelper h){
        var p=player(h);var event=new PlayerEvent.TabListNameFormat(p);event.setDisplayName(Component.literal("[Town] Crafter"));TabLatency.name(event);
        h.assertTrue(event.getDisplayName().getString().equals("[Town] Crafter  -- ms"),"Custom display text is preserved and no invented zero ping is shown");
        h.succeed();
    }
    @GameTest(template="industrial") public static void logoutAndReconnectDoNotReuseAnotherConnectionsLatency(GameTestHelper h){
        var p=player(h);int id=TabLatency.probe(p,Util.getMillis());p.connection.handlePong(new ServerboundPongPacket(id));
        TabLatency.logout(new PlayerEvent.PlayerLoggedOutEvent(p));
        h.assertTrue(TabLatency.reading(p,Util.getMillis()).milliseconds()==-1,"Logout removes the old connection measurement");
        var name=new PlayerEvent.TabListNameFormat(p);TabLatency.name(name);
        h.assertTrue(name.getDisplayName().getString().equals("latency-test  -- ms"),"The ordinary profile name and waiting state render through native display components");h.succeed();
    }
}

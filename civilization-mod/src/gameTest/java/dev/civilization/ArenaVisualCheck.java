package dev.civilization;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Two independent hidden clients; only scenery/setup is server-scripted. */
final class ArenaVisualCheck {
    static final BlockPos AT=new BlockPos(320,100,0);
    static int ticks,step,waitTicks,peerStep=-1,peerWait;
    static volatile boolean ready,configured,combatPrepared;
    static volatile Throwable failure;
    static boolean connecting;
    static boolean peerSent;
    static Path marker(Minecraft mc,String name){return mc.gameDirectory.toPath().toAbsolutePath().normalize().getParent().resolve("arena-peer/"+name+".txt");}
    static void write(Minecraft mc,String name,String value){try{Files.createDirectories(marker(mc,name).getParent());Files.writeString(marker(mc,name),value);}catch(Exception e){throw new RuntimeException(e);}}
    static String read(Minecraft mc,String name){try{return Files.readString(marker(mc,name));}catch(Exception e){return "";}}
    static void send(Minecraft mc,int action){if(mc.player.containerMenu instanceof ArenaMenu m)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArenaPayload(m.containerId,m.values.get(1),action,m.values.get(11)));else throw new IllegalStateException("Expected arena menu for action "+action);}
    static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"gladiator-"+name+".png",mc.getMainRenderTarget(),c->{});}
    static void next(Minecraft mc,int value){step=value;waitTicks=0;write(mc,"step",Integer.toString(value));}
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Arena fixture",failure);
        if(ticks==80){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.fov().set(65);mc.options.hideGui=true;write(mc,"done","");write(mc,"step","0");server.execute(()->{try{
            var level=server.overworld();var host=server.getPlayerList().getPlayers().getFirst();var pit=ArenaGameTests.build(level,AT,Direction.NORTH);pit.owner=host.getUUID();ArenaData.get(server).pits.put(pit.at,pit);
            level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);host.setGameMode(GameType.CREATIVE);host.getAbilities().flying=true;host.onUpdateAbilities();host.teleportTo(level,320,119,-27,Set.of(),0,31);
            // Minimal optional stands demonstrate their independence from the blueprint.
            for(int x=310;x<=330;x++)for(int row=0;row<3;row++)level.setBlockAndUpdate(new BlockPos(x,101+row,12+row),Blocks.STONE_BRICK_STAIRS.defaultBlockState());
            for(int x:new int[]{-7,7})for(int y=1;y<=2;y++)level.setBlockAndUpdate(AT.offset(x,y,3),Blocks.STONE.defaultBlockState());
            level.removeBlock(AT.offset(5,0,2),false);level.setBlockAndUpdate(AT.offset(5,-1,2),Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(AT.offset(17,1,1),Blocks.CHEST.defaultBlockState());
            if(!ArenaStructure.problem(level,pit).isEmpty())throw new IllegalStateException("Custom interior invalidated the arena");
            server.setUsesAuthentication(false);if(!server.publishServer(GameType.SURVIVAL,false,25570))throw new IllegalStateException("Cannot open arena test port");ready=true;write(mc,"ready","ready");
        }catch(Throwable t){failure=t;}});}
        if(!ready||ticks<150)return;
        if(ticks==150)shot(mc,"complete-oval");
        if(ticks==151)server.execute(()->{var host=server.getPlayerList().getPlayers().getFirst();host.teleportTo(server.overworld(),342.5,103,.5,Set.of(),90,10);});
        if(ticks==180)shot(mc,"rear-doorway");
        if(step==0&&ticks>185&&!configured)server.execute(()->{try{var players=server.getPlayerList().getPlayers();if(players.size()<2)return;var host=players.stream().filter(p->!p.getGameProfile().getName().equals("ArenaPeer")).findFirst().orElseThrow();var peer=players.stream().filter(p->p.getGameProfile().getName().equals("ArenaPeer")).findFirst().orElseThrow();
            for(var p:List.of(host,peer)){p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setHealth(p.getMaxHealth());p.removeAllEffects();p.teleportTo(server.overworld(),320.5,101,p==host?2.5:-2.5,Set.of(),p==host?180:0,30);}
            host.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));peer.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,6));ArenaMenu.open(host,AT);ArenaMenu.open(peer,AT);configured=true;
        }catch(Throwable t){failure=t;}});
        if(!configured){if(ticks>2400)throw new IllegalStateException("Independent arena peer did not connect");return;}
        mc.options.hideGui=false;waitTicks++;
        if(waitTicks<25)return;
        switch(step){
            case 0->{if(mc.player.containerMenu instanceof ArenaMenu){shot(mc,"controller-menu");send(mc,0);next(mc,1);}}
            case 1->{if(read(mc,"done").equals("1")&&mc.player.containerMenu instanceof ArenaMenu m&&m.values.get(4)!=0){send(mc,2);next(mc,2);}}
            case 2->{if(read(mc,"done").equals("2")&&mc.player.containerMenu instanceof ArenaMenu m&&m.getSlot(1).hasItem()){send(mc,4);next(mc,3);}}
            case 3->{if(read(mc,"done").equals("3")&&mc.player.containerMenu instanceof ArenaMenu m&&m.values.get(5)==1&&m.values.get(6)==1){shot(mc,"accepted-stakes");send(mc,5);next(mc,4);}}
            case 4->{if(read(mc,"done").equals("4"))next(mc,5);}
            case 5->{if(combatPrepared){next(mc,6);break;}server.execute(()->{try{if(combatPrepared)return;var d=ArenaData.get(server);var pit=d.pits.get(ArenaService.address(server.overworld(),AT));if(pit.phase!=ArenaData.LIVE)return;
                var host=server.getPlayerList().getPlayer(pit.fighters[0]);var peer=server.getPlayerList().getPlayer(pit.fighters[1]);if(host==null||peer==null)throw new IllegalStateException("Missing real fighter");
                if(pit.stakes[0].getCount()!=4||pit.stakes[1].getCount()!=6||!host.getMainHandItem().isEmpty()||!peer.getMainHandItem().isEmpty())throw new IllegalStateException("Actual player stake conservation");
                host.getInventory().setItem(0,Items.IRON_SWORD.getDefaultInstance());host.teleportTo(server.overworld(),320.5,101,2.5,Set.of(),180,0);peer.teleportTo(server.overworld(),320.5,101,.5,Set.of(),0,0);peer.setHealth(3);combatPrepared=true;
            }catch(Throwable t){failure=t;}});}
            case 6->{var target=mc.level.players().stream().filter(p->p.getGameProfile().getName().equals("ArenaPeer")).findFirst().orElse(null);if(target!=null&&mc.player.distanceTo(target)<4){mc.gameMode.attack(mc.player,target);mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);next(mc,7);}}
            case 7->{server.execute(()->{try{var d=ArenaData.get(server);var pit=d.pits.get(ArenaService.address(server.overworld(),AT));if(pit.phase!=ArenaData.RESULT||pit.winner!=0)throw new IllegalStateException("Real client melee did not resolve knockout");var host=server.getPlayerList().getPlayer(pit.fighters[0]);var peer=server.getPlayerList().getPlayer(pit.fighters[1]);if(!peer.isAlive()||peer.getHealth()<19||host.getHealth()<19)throw new IllegalStateException("Knockout recovery");if(d.credits.get(host.getUUID()).stream().mapToInt(ItemStack::getCount).sum()!=10)throw new IllegalStateException("Winner owns both fighter stakes");host.teleportTo(server.overworld(),320.5,101,2.5,Set.of(),180,35);ArenaMenu.open(host,AT);
                // Mark inspection results independently of the peer's client acknowledgement.
                System.out.println("ARENA REAL PEER KNOCKOUT AND ESCROW VERIFIED");
            }catch(Throwable t){failure=t;}});next(mc,8);}
            case 8->{if(mc.player.containerMenu instanceof ArenaMenu){shot(mc,"winner-collection");send(mc,12);next(mc,9);}}
            case 9->{server.execute(()->{try{var host=server.getPlayerList().getPlayers().stream().filter(p->!p.getGameProfile().getName().equals("ArenaPeer")).findFirst().orElseThrow();if(CivicItems.count(host,Items.DIAMOND.getDefaultInstance())!=4||CivicItems.count(host,Items.IRON_INGOT.getDefaultInstance())!=6||ArenaData.get(server).credits.containsKey(host.getUUID()))throw new IllegalStateException("Actual winner collection");System.out.println("ARENA CLIENT COLLECTION VERIFIED");}catch(Throwable t){failure=t;}});next(mc,10);}
            case 10->{if(read(mc,"done").equals("10")){shot(mc,"final");mc.stop();}}
        }
        if(ticks>3000)throw new IllegalStateException("Arena peer sequence timeout at "+step);
    }
    static void peer(Minecraft mc){
        if(!connecting&&mc.screen instanceof TitleScreen&&read(mc,"ready").equals("ready")){connecting=true;net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen,mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("127.0.0.1:25570"),new net.minecraft.client.multiplayer.ServerData("Arena test","127.0.0.1:25570",net.minecraft.client.multiplayer.ServerData.Type.OTHER),false,null);}
        if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)throw new IllegalStateException("Independent arena peer disconnected");
        if(mc.level==null||mc.player==null)return;mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);String text=read(mc,"step");if(text.isEmpty())return;int step=Integer.parseInt(text);if(step!=peerStep){peerStep=step;peerWait=0;peerSent=false;}peerWait++;if(peerWait<30||peerSent)return;
        int action=switch(step){case 1->1;case 2->2;case 3->4;case 4->5;default->-1;};
        if(action>=0){if(!(mc.player.containerMenu instanceof ArenaMenu m))return;
            boolean synced=switch(step){case 1->m.values.get(3)!=0;case 2->m.getSlot(0).hasItem();case 3->m.values.get(5)==1;case 4->m.values.get(5)==1&&m.values.get(6)==1;default->false;};
            if(!synced)return;send(mc,action);peerSent=true;write(mc,"done",Integer.toString(step));}
        if(step==10){if(mc.player.getHealth()<19)throw new IllegalStateException("Peer knockout recovery not synchronized");shot(mc,"peer-recovered");write(mc,"done","10");mc.stop();}
    }
}

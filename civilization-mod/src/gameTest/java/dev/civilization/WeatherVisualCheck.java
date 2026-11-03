package dev.civilization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import dev.civilization.client.LocalWeather;
final class WeatherVisualCheck {
 private static int ticks,peerTicks;private static boolean connecting,wet,peerVerified;private static volatile Throwable failure;
 private static final BlockPos WET=new BlockPos(500,101,32),DRY=new BlockPos(524,101,32);
 private static java.nio.file.Path marker(Minecraft mc){return PreviewVisualCheck.peerFile(mc,"verified");}
 private static boolean verified(Minecraft mc){try{return java.nio.file.Files.readString(marker(mc)).startsWith("Independent dry peer verified");}catch(Exception e){return false;}}
 static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Weather fixture",failure);
  if(ticks==80)server.execute(()->{try{var l=server.overworld();for(int x=488;x<=535;x++)for(int z=16;z<=48;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),(x==512?Blocks.COPPER_BLOCK:Blocks.STONE_BRICKS).defaultBlockState());for(int y=101;y<127;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
   var w=RegionalWeather.get(l);w.clear(l,WET);w.clear(l,DRY);w.call(l,WET);l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,server);
   var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,500,103,24,java.util.Set.of(),0,15);p.getInventory().setItem(0,FarmingContent.RAIN_CALLER.toStack());
   server.setUsesAuthentication(false); // Disposable local test accounts only.
   if(!server.publishServer(net.minecraft.world.level.GameType.CREATIVE,false,PreviewVisualCheck.PEER_PORT))throw new IllegalStateException("Cannot open the peer test port");java.nio.file.Files.writeString(PreviewVisualCheck.peerFile(mc,"ready"),"ready");
  }catch(Throwable t){failure=t;}});
  if(ticks==90){mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.fov().set(60);}
  if(ticks>100&&ticks%20==0)server.execute(()->{for(var p:server.getPlayerList().getPlayers())if(p.getGameProfile().getName().equals("Peer")&&p.getX()!=524){p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),524,103,24,java.util.Set.of(),0,15);}WeatherEvents.sync(server.overworld());});
  // The fresh world generates both districts first, so wait for the synchronized sky rather than a fixed tick.
  if(!wet&&ticks>=240&&LocalWeather.raining(mc.level,WET)&&!LocalWeather.raining(mc.level,DRY)&&mc.level.getRainLevel(1)>=.9){wet=true;shot(mc,"wet");}
  if(!wet&&ticks>1200)throw new IllegalStateException("Regional client snapshot/sky mismatch");
  if(wet&&verified(mc)){shot(mc,"host-final");com.mojang.logging.LogUtils.getLogger().info("REGIONAL WEATHER TWO CLIENTS VERIFIED");mc.stop();}
  if(ticks>2400)throw new IllegalStateException("Weather peer did not verify within two minutes");
 }
 static void peer(Minecraft mc){
  if(peerVerified){mc.stop();return;}
  if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen){shot(mc,"connection-failed");String reason=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget).map(c->((net.minecraft.client.gui.components.AbstractWidget)c).getMessage().getString()).toList().toString();throw new IllegalStateException("Weather peer disconnected: "+reason);}

  if(!connecting&&mc.screen instanceof TitleScreen){connecting=true;net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen,mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("127.0.0.1:"+PreviewVisualCheck.PEER_PORT),new net.minecraft.client.multiplayer.ServerData("Weather test","127.0.0.1:"+PreviewVisualCheck.PEER_PORT,net.minecraft.client.multiplayer.ServerData.Type.OTHER),false,null);}
  if(mc.level==null||mc.player==null)return;
  mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
  if(++peerTicks<120)return;
  if(LocalWeather.ready(mc.level)&&mc.player.getBlockX()==524&&!LocalWeather.raining(mc.level,DRY)&&LocalWeather.raining(mc.level,WET)&&mc.level.getRainLevel(1)<=.05){shot(mc,"dry-peer");try{java.nio.file.Files.writeString(marker(mc),"Independent dry peer verified");}catch(Exception e){throw new RuntimeException(e);}com.mojang.logging.LogUtils.getLogger().info("DRY WEATHER PEER VERIFIED");peerVerified=true;}
  else if(peerTicks>1200)throw new IllegalStateException("Peer is not independently dry");
 }
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"weather-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

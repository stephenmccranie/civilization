package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
final class CampusVisualCheck {
 private static int ticks;private static volatile Throwable failure;
 static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(mc.getSingleplayerServer().overworld(),x,y,z,java.util.Set.of(),yaw,pitch);});}
 static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"campus-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
 static void tick(Minecraft mc){if(failure!=null)throw new IllegalStateException("Campus validation failed",failure);ticks++;
  if(ticks==80){mc.options.fov().set(65);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{try{
   var s=mc.getSingleplayerServer();var l=s.overworld();
   if(!(l.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource))throw new IllegalStateException("World is not superflat");
   if(!Geography.canFarm(l,new BlockPos(-60,63,-25)))throw new IllegalStateException("River farm is not eligible");
   for(int x:new int[]{18,32,46}){var at=new BlockPos(x,64,-28);l.getChunk(at);if(!(((KilnBlockEntity)l.getBlockEntity(at)).checkStructure().status()==MachineStructure.COMPLETE))throw new IllegalStateException("Workshop incomplete at "+at);}
   for(int x:new int[]{30,42,56}){var at=new BlockPos(x,64,20);l.getChunk(at);if(!IndustrialStructure.bind((IndustrialBlockEntity)l.getBlockEntity(at)))throw new IllegalStateException("Refinery incomplete");}
   if(!SurveyTable.complete(l,new BlockPos(0,64,-25)))throw new IllegalStateException("Map incomplete");
   for(String name:new String[]{"coal","oil","forest"})if(s.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("civilization_test:"+name)))==null)throw new IllegalStateException("Missing annex "+name);
   var chest=(ChestBlockEntity)l.getBlockEntity(new BlockPos(0,64,-4));if(chest.getItem(0).isEmpty())throw new IllegalStateException("Missing guidebook");
   System.out.println("CAMPUS_CONTENT_PASS flat=true riverfarm=true workshop=true refinery=true annexes=true guide=true");
  }catch(Throwable t){failure=t;}});camera(mc,-112,118,-97,-47,29);}
  if(ticks==150)shot(mc,"overview");
  if(ticks==160)camera(mc,-4,70,-18,5,20);
  if(ticks==210)shot(mc,"plaza");
  if(ticks==220)camera(mc,87,80,-4,55,20);
  if(ticks==270)shot(mc,"industry");
  if(ticks==280)camera(mc,-9,84,13,50,25);
  if(ticks==340){shot(mc,"boats");System.out.println("CAMPUS_VISUAL_PASS");}
  if(ticks==350||ticks==440){final boolean coal=ticks==350;mc.getSingleplayerServer().execute(()->{try{
    var server=mc.getSingleplayerServer();var main=server.overworld();var command=((CommandBlockEntity)main.getBlockEntity(new BlockPos(coal?-4:4,63,-56))).getCommandBlock().getCommand();
    var m=java.util.regex.Pattern.compile("run tp @s ([^ ]+) ([^ ]+) ([^ ]+)").matcher(command);if(!m.find())throw new IllegalStateException("Bad annex button");
    int x=(int)Math.floor(Double.parseDouble(m.group(1))),z=(int)Math.floor(Double.parseDouble(m.group(3)))-14;
    var dim=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("civilization_test:"+(coal?"coal":"oil"))));
    var site=Deposits.at(dim,new BlockPos(x,64,z));if(site==null)throw new IllegalStateException("Annex resource site not persistent");
    for(int cx=(site.x()-20)>>4;cx<=(site.x()+20)>>4;cx++)for(int cz=(site.z()-20)>>4;cz<=(site.z()+20)>>4;cz++)dim.getChunk(cx,cz);
    var machine=(IndustrialBlockEntity)dim.getBlockEntity(new BlockPos(x,64,z));if(!IndustrialStructure.bind(machine))throw new IllegalStateException("Extraction structure incomplete");
    for(int i=0;i<20;i++)machine.process();
    if(coal?machine.getItem(1).isEmpty():machine.output.isEmpty())throw new IllegalStateException("Extraction failed after reload: "+machine.status);
    System.out.println("CAMPUS_EXTRACTION_PASS "+(coal?"coal":"oil"));
    var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(dim,site.x()+.5,site.bottom()+3,site.z()+site.radius()+4,java.util.Set.of(),180,6);
  }catch(Throwable t){failure=t;}});}
  if(ticks==430)shot(mc,"coal-gallery");
  if(ticks==520){shot(mc,"oil-gallery");System.out.println("CAMPUS_ANNEX_PASS");}
  if(ticks>530)mc.stop();
 }
}

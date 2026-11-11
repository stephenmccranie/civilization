package dev.civilization;
import dev.civilization.client.PhotonRivers;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
/**
 * Photon runs river waves downstream: a channel DEPTH blocks deep runs south down a straight reach, round a quarter
 * bend of radius BEND about the origin and out to the west, with its line handed to the client as the server would
 * send it. Each shot is a run of screenshots two ticks apart; looking straight down, screen up is south and screen
 * right is west, so the reach's waves run up the screen, the exit's run right and the bend's run up and right.
 * The still shots repeat views with no line, where Photon's own waves run one way everywhere.
 */
final class RiverVisualCheck {
    private static final int BEND=40,REACH=36,HALF=7,DEPTH=5,Y=200,START=120,STEP=110,SETTLE=70;
    private record Shot(String name,double x,double y,double z,float pitch,boolean lines,int frames){}
    private static final Shot[] SHOTS={
            new Shot("low",BEND+.5,Y+3,-REACH-3,20,true,16),
            new Shot("reach",BEND+.5,Y+7,-18.5,90,true,3),
            new Shot("bend",BEND*.71,Y+7,BEND*.71,90,true,3),
            new Shot("exit",-18.5,Y+7,BEND+.5,90,true,3),
            new Shot("reach-still",BEND+.5,Y+7,-18.5,90,false,3),
            new Shot("exit-still",-18.5,Y+7,BEND+.5,90,false,3),
            new Shot("low-still",BEND+.5,Y+3,-REACH-3,20,false,16)};
    private static int ticks;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    /** Distance across from the channel's centreline. */
    private static double across(double x,double z){
        double reach=z<0?Math.hypot(x-BEND,Math.max(0,-REACH-z)):Double.MAX_VALUE;
        double exit=x<0?Math.hypot(z-BEND,Math.max(0,-REACH-x)):Double.MAX_VALUE;
        double bend=x>=0&&z>=0?Math.abs(Math.hypot(x,z)-BEND):Double.MAX_VALUE;
        return Math.min(bend,Math.min(reach,exit));
    }
    /** The channel's line as the server sends one: points 12 blocks apart down the reaches and 15 degrees round the bend. */
    private static float[] line(){
        var points=new java.util.ArrayList<float[]>();
        for(int z=-REACH;z<0;z+=12)points.add(new float[]{BEND,z,z+REACH,HALF});
        for(int d=0;d<=90;d+=15){double a=Math.toRadians(d);points.add(new float[]{(float)(BEND*Math.cos(a)),(float)(BEND*Math.sin(a)),(float)(REACH+BEND*a),HALF});}
        for(int x=-12;x>=-REACH;x-=12)points.add(new float[]{x,BEND,(float)(REACH+BEND*Math.PI/2-x),HALF});
        float[] out=new float[points.size()*RiverFlow.POINT];
        for(int k=0;k<points.size();k++)System.arraycopy(points.get(k),0,out,k*RiverFlow.POINT,RiverFlow.POINT);
        return out;
    }
    private static final java.util.List<java.util.concurrent.CompletableFuture<Void>> WRITES=new java.util.ArrayList<>();
    /** Files are written off the render thread, as Minecraft's own screenshots are, so frames keep coming two ticks apart. */
    private static void grab(Minecraft mc,String name){
        var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());
        var file=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        WRITES.add(java.util.concurrent.CompletableFuture.runAsync(()->{
            try(image){java.nio.file.Files.createDirectories(file.getParent());image.writeToFile(file);}
            catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        },net.minecraft.Util.ioPool()));
    }
    private static void view(Minecraft mc,Shot shot){var server=mc.getSingleplayerServer();server.execute(()->{
        var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(server.overworld(),shot.x,shot.y,shot.z,java.util.Set.of(),0,shot.pitch);});}
    static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.hideGui=true;mc.options.fov().set(70);TemperatureWashConfig.ENABLED.set(false);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{
            var l=server.overworld();
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
            int lo=-REACH-HALF-6,hi=BEND+HALF+6;var pos=new BlockPos.MutableBlockPos();
            for(int x=lo;x<=hi;x++)for(int z=lo;z<=hi;z++){
                boolean water=across(x+.5,z+.5)<=HALF;
                for(int y=Y-DEPTH-2;y<=Y+16;y++){
                    var block=y>Y?Blocks.AIR:y==Y&&!water?Blocks.GRASS_BLOCK:y>=Y-DEPTH+1?(water?Blocks.WATER:Blocks.DIRT):y==Y-DEPTH&&water?Blocks.MUD:Blocks.STONE;
                    l.setBlock(pos.set(x,y,z),block.defaultBlockState(),2);
                }
            }
        });}
        int shot=(ticks-START)/STEP,at=(ticks-START)%STEP;
        if(ticks<START||shot>SHOTS.length)return;
        if(shot==SHOTS.length){
            if(at!=0)return;
            java.util.concurrent.CompletableFuture.allOf(WRITES.toArray(java.util.concurrent.CompletableFuture[]::new)).join();
            require(PhotonRivers.patched,"Photon's translucent program runs river waves downstream");
            com.mojang.logging.LogUtils.getLogger().info("RIVERS VISUAL VERIFIED");mc.stop();return;
        }
        var s=SHOTS[shot];
        if(at==0){PhotonRivers.accept(mc.level,s.lines?new float[][]{line()}:new float[0][]);view(mc,s);}
        int frame=(at-SETTLE)/2;
        if(at>=SETTLE&&at%2==0&&frame<s.frames)grab(mc,"rivers-"+s.name+"-"+frame+".png");
    }
}

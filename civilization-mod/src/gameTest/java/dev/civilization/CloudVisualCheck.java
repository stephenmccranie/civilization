package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
/** Photon draws the server's clouds: where the server says a cloud shades the player, the ground around them is darker. */
final class CloudVisualCheck {
    private static int ticks;
    private static long shadeClock=-1,sunClock=-1;
    private static double shadeLight,sunLight;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void districts(net.minecraft.server.level.ServerLevel l,double cloudiness){
        var w=RegionalWeather.get(l);
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            var d=w.district(l,net.minecraft.world.level.ChunkPos.asLong(dx,dz));
            d.rain=false;d.cooling=0;d.coolingAt=w.clock;d.end=w.clock+100_000_000L;
            d.cloudFrom=d.cloudTo=cloudiness;d.cloudStart=w.clock;d.cloudEnd=w.clock+100_000_000L;
        }
    }
    /** Saves the frame and returns the mean brightness of the ground in its lower middle. */
    private static double grab(Minecraft mc,String name){
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots");java.nio.file.Files.createDirectories(dir);image.writeToFile(dir.resolve(name));
            double sum=0;int n=0;
            for(int y=(int)(image.getHeight()*.6);y<image.getHeight()*.95;y+=4)for(int x=(int)(image.getWidth()*.3);x<image.getWidth()*.7;x+=4){
                int abgr=image.getPixelRGBA(x,y);sum+=.2126*(abgr&255)+.7152*(abgr>>8&255)+.0722*(abgr>>16&255);n++;
            }
            return sum/n;
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.hideGui=true;TemperatureWashConfig.ENABLED.set(false);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{
            var l=server.overworld();
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);l.setDayTime(6000);
            for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)for(int y=199;y<=210;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),y==199?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,.5,200,.5,java.util.Set.of(),0,50);
            districts(l,.5);
            // Pick a weather clock where the server puts a cloud on the line to the sun, and one where it is clear.
            var w=RegionalWeather.get(l);var torso=ThermalSystem.torso(new net.minecraft.world.phys.Vec3(.5,200,.5));long start=w.clock;
            for(long c=start;c<start+2_000_000&&(shadeClock<0||sunClock<0);c+=100){
                w.clock=c;double cover=ThermalExposure.Sky.at(l,torso).cover();
                if(cover>.98&&shadeClock<0)shadeClock=c;if(cover<.02&&sunClock<0)sunClock=c;
            }
            require(shadeClock>=0&&sunClock>=0,"Half cloudiness has both cloud and gaps over the player");
            w.clock=shadeClock;WeatherEvents.sync(l);
        });}
        if(ticks==240)shadeLight=grab(mc,"clouds-shaded.png");
        if(ticks==245)server.execute(()->{var l=server.overworld();RegionalWeather.get(l).clock=sunClock;WeatherEvents.sync(l);});
        if(ticks==385)sunLight=grab(mc,"clouds-sunlit.png");
        // The sky as a player on the ground sees it, looking up toward the horizon.
        if(ticks==390)server.execute(()->{var l=server.overworld();int ground=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,40,40);
            server.getPlayerList().getPlayers().getFirst().teleportTo(l,40.5,ground,40.5,java.util.Set.of(),0,-25);});
        if(ticks==470)grab(mc,"clouds-partly.png");
        if(ticks==475)server.execute(()->{var l=server.overworld();districts(l,1);WeatherEvents.sync(l);});
        if(ticks==595)grab(mc,"clouds-overcast.png");
        if(ticks==600){
            require(dev.civilization.client.PhotonClouds.patched.containsAll(java.util.Set.of("coverage","shadows")),"Photon draws the shared cloud field");
            require(sunLight>shadeLight*1.15,"Ground is darker where the server puts a cloud over the player: shaded "+shadeLight+", sunlit "+sunLight);
            com.mojang.logging.LogUtils.getLogger().info("CLOUDS VISUAL VERIFIED shaded="+shadeLight+" sunlit="+sunLight);mc.stop();
        }
    }
}

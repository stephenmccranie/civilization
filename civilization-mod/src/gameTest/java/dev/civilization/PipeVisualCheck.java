package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;

/** Pipe art fixture: every shape, each liquid both ways, the ports of a modeled and a block-built machine, night, and the items. */
final class PipeVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    /** A run from one end to the other through its corners; the ends are what it runs between, not pipes. */
    private record Run(int fluid,List<BlockPos> path){
        Run(int fluid,int... corners){this(fluid,along(corners));}
        List<BlockPos> pipes(){return path.subList(1,path.size()-1);}
        void flow(ServerLevel l,boolean reversed){
            for(int i=1;i<path.size()-1;i++){
                Direction in=side(path.get(i),path.get(reversed?i+1:i-1)),out=side(path.get(i),path.get(reversed?i-1:i+1));
                l.blockEvent(path.get(i),IndustrialContent.PIPE.get(),fluid,in.ordinal()|out.ordinal()<<3);
            }
        }
    }
    private static Direction side(BlockPos from,BlockPos to){return Direction.fromDelta(to.getX()-from.getX(),to.getY()-from.getY(),to.getZ()-from.getZ());}
    private static List<BlockPos> along(int... corners){
        var path=new ArrayList<BlockPos>();var at=new BlockPos(corners[0],corners[1],corners[2]);path.add(at);
        for(int i=3;i<corners.length;i+=3){
            var to=new BlockPos(corners[i],corners[i+1],corners[i+2]);var step=Direction.fromDelta(Integer.signum(to.getX()-at.getX()),Integer.signum(to.getY()-at.getY()),Integer.signum(to.getZ()-at.getZ()));
            while(!at.equals(to)){at=at.relative(step);path.add(at);}
        }
        return path;
    }
    // Shapes against the wall: a length with both ends blanked, a bend, a tee, a cross with six arms, a riser, a lone pipe and a run into a port.
    private static final List<Run> SHAPES=List.of(new Run(1,91,102,0,95,102,0),new Run(2,91,104,0,93,104,0,93,106,0),
        new Run(5,94,104,0,98,104,0),new Run(4,96,106,0,96,104,0,97,104,0),
        new Run(3,98,105,0,102,105,0),new Run(3,100,107,0,100,103,0),new Run(3,100,105,-2,100,105,2),
        new Run(4,103,100,0,103,106,0),new Run(3,105,104,0,107,104,0,107,104,3));
    private static final BlockPos LONE=new BlockPos(105,102,0),PORT=new BlockPos(107,104,3);
    // One run for each liquid, alternate rows set back so they stay apart.
    private static final List<Run> LIQUIDS=List.of(new Run(1,110,102,0,116,102,0),new Run(2,110,103,2,116,103,2),new Run(3,110,104,0,116,104,0),
        new Run(4,110,105,2,116,105,2),new Run(5,110,106,0,116,106,0));
    // Crude into the Fired Heater's intake, and hot oil from its outlet to the Distillation Column's port.
    private static final BlockPos HEATER=new BlockPos(122,101,-8),COLUMN=new BlockPos(130,101,-8);
    private static final List<Run> MACHINES=List.of(new Run(1,117,102,-6,121,102,-6),new Run(3,123,102,-6,128,102,-6));
    private static void machine(ServerLevel l,IndustrialBlock b,BlockPos at){
        l.setBlockAndUpdate(at,b.defaultBlockState());var m=(IndustrialBlockEntity)l.getBlockEntity(at);
        if(m.model()!=null)MachineFixture.build(l,at);else for(var p:IndustrialStructure.parts(b.kind))MachineStructure.placePart(l,at,Direction.NORTH,p);
        if(!IndustrialStructure.bind(m))throw new IllegalStateException("Incomplete fixture: "+b.kind);
    }
    private static void view(Minecraft mc,double x,double y,double z,float yaw,float pitch){
        var server=mc.getSingleplayerServer();server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),x,y,z,java.util.Set.of(),yaw,pitch));
    }
    private static boolean flowing(Minecraft mc,BlockPos at,Direction side){
        return mc.level.getBlockEntity(at) instanceof PipeFlowEntity flow&&flow.until[side.ordinal()]>mc.level.getGameTime();
    }
    static void tick(Minecraft mc) {
        ticks++;
        var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Pipe fixture failed",failure);
        if(ticks==100){
            mc.options.hideGui=true;mc.options.fov().set(45);mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{try{
                var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                for(int x=86;x<=138;x++)for(int z=-24;z<=6;z++)for(int y=100;y<=114;y++)
                    l.setBlockAndUpdate(new BlockPos(x,y,z),y==100?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                for(int x=89;x<=118;x++)for(int y=101;y<=108;y++)l.setBlockAndUpdate(new BlockPos(x,y,3),(x>=105&&x<=109?Blocks.BRICKS:Blocks.SMOOTH_STONE).defaultBlockState());
                l.setBlockAndUpdate(PORT,IndustrialContent.PORT.get().defaultBlockState());
                machine(l,IndustrialContent.HEATER.get(),HEATER);machine(l,IndustrialContent.COLUMN.get(),COLUMN);
                l.setBlockAndUpdate(LONE,IndustrialContent.PIPE.get().defaultBlockState());
                for(var runs:List.of(SHAPES,LIQUIDS,MACHINES))for(var run:runs)for(var at:run.pipes())l.setBlockAndUpdate(at,IndustrialContent.PIPE.get().defaultBlockState());
                for(int x:new int[]{93,99,105,113})l.setBlockAndUpdate(new BlockPos(x,101,-2),Blocks.LANTERN.defaultBlockState());
                for(int x:new int[]{119,126})l.setBlockAndUpdate(new BlockPos(x,101,-9),Blocks.LANTERN.defaultBlockState());
                p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
                p.getInventory().clearContent();p.getInventory().setItem(0,IndustrialContent.PIPE.toStack());p.getInventory().setItem(1,IndustrialContent.PORT.toStack());
                p.teleportTo(l,99.5,103,-13,java.util.Set.of(),0,0);
                l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);ready=true;
            }catch(Throwable t){failure=t;}});
        }
        if(ticks>100&&!ready){ticks=101;return;}
        // Transfers run except for the stretch that shows them clearing; the liquid runs turn round halfway.
        boolean reversed=ticks>=290;
        if(ticks>=140&&ticks%6==0&&(ticks<=430||ticks>=500))server.execute(()->{
            var l=server.overworld();
            for(var run:SHAPES)run.flow(l,false);
            for(var run:MACHINES)run.flow(l,false);
            for(var run:LIQUIDS)run.flow(l,reversed);
        });
        if(ticks==180){
            if(!flowing(mc,new BlockPos(93,102,0),Direction.EAST))throw new IllegalStateException("Pipe flow telemetry missing");
            shot(mc,"shapes");
        }
        if(ticks==184)view(mc,97.5,105.5,-4.5,0,12);
        if(ticks==214)shot(mc,"joints");
        if(ticks==216)view(mc,104.3,103.5,-2.2,-32,7);
        if(ticks==246)shot(mc,"port");
        if(ticks==248)view(mc,113.5,102.9,-6.5,0,0);
        if(ticks==278)shot(mc,"liquids-east");
        if(ticks==282)shot(mc,"liquids-east-later");
        if(ticks==320)shot(mc,"liquids-west");
        if(ticks==324)shot(mc,"liquids-west-later");
        if(ticks==326)view(mc,125.5,103.5,-14.5,0,14);
        if(ticks==356)shot(mc,"machines");
        if(ticks==358)view(mc,117.5,102.3,-10.5,-34,14);
        if(ticks==388)shot(mc,"heater-intake");
        if(ticks==390)view(mc,125.5,102.3,-10.5,-24,14);
        if(ticks==420)shot(mc,"column-port");
        if(ticks==432)view(mc,99.5,103,-13,0,0);
        if(ticks==470){
            if(flowing(mc,new BlockPos(93,102,0),Direction.EAST))throw new IllegalStateException("Pipe flow did not clear");
            shot(mc,"empty");
        }
        if(ticks==472)server.execute(()->server.overworld().setDayTime(18000));
        if(ticks==540)shot(mc,"night");
        if(ticks==542)view(mc,125.5,103.5,-14.5,0,14);
        if(ticks==572)shot(mc,"machines-night");
        if(ticks==574){server.execute(()->server.overworld().setDayTime(6000));view(mc,104.5,102.5,-3.5,0,0);mc.options.hideGui=false;}
        if(ticks==620)shot(mc,"held");
        if(ticks==624)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==640)shot(mc,"item");
        if(ticks>645)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"pipes-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

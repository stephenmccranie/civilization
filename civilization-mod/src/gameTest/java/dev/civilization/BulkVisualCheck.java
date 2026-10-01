package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
final class BulkVisualCheck {
    static int ticks;static volatile boolean ready;static volatile Throwable failure;
    static final BlockPos COAL=new BlockPos(80,101,0),TANK=new BlockPos(86,101,0);
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Bulk fixture",failure);
        if(ticks==80){mc.options.guiScale().set(3);mc.options.fov().set(55);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
            for(int x=74;x<93;x++)for(int z=-10;z<10;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<118;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            var b=BulkStorageGameTests.build(l,COAL,false,Direction.NORTH);b.insertCoal(6144,false);var t=BulkStorageGameTests.build(l,TANK,true,Direction.NORTH);t.fluids(Direction.NORTH).fill(new FluidStack(IndustrialContent.FUEL.get(),64000),EXECUTE);
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,77,106,-10,java.util.Set.of(),-29,20);l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);p.getInventory().clearContent();p.getInventory().setItem(0,BulkContent.BUNKER.toStack());p.getInventory().setItem(1,BulkContent.TANK.toStack());ready=true;
        }catch(Throwable t){failure=t;}});}
        if(ticks>80&&!ready){ticks=81;return;}
        if(ticks==130){if(!(mc.level.getBlockEntity(COAL) instanceof BulkEntity b)||b.amount()!=6144||!b.formed)throw new IllegalStateException("Visible coal synchronization");shot(mc,"assemblies");}
        if(ticks==135){mc.options.hideGui=false;server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,86.5,101,-2,java.util.Set.of(),0,0);p.openMenu((BulkEntity)l.getBlockEntity(TANK));});}
        if(ticks==165){if(!(mc.player.containerMenu instanceof BulkMenu m)||m.amount()!=64000||!m.liquid()||m.data.get(3)!=1)throw new IllegalStateException("64000 mB actual menu sync");shot(mc,"tank-menu");}
        if(ticks==170)server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(l,80.5,101,-2,java.util.Set.of(),0,0);p.openMenu((BulkEntity)l.getBlockEntity(COAL));});
        if(ticks==200){shot(mc,"bunker-menu");minecraftTake(mc);}
        if(ticks==215){if(!(mc.player.containerMenu instanceof BulkMenu m)||m.amount()!=6112)throw new IllegalStateException("Real menu coal take");}
        if(ticks==220){mc.options.hideGui=true;server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();((BulkEntity)l.getBlockEntity(COAL)).extractCoal(8192,false);((BulkEntity)l.getBlockEntity(TANK)).fluids(Direction.NORTH).drain(32000,EXECUTE);p.teleportTo(l,90,105,-6,java.util.Set.of(),43,18);});}
        if(ticks==255)shot(mc,"empty-and-half");
        if(ticks==260){mc.options.hideGui=false;server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();l.removeBlock(COAL.offset(-1,2,0),false);p.teleportTo(l,80.5,101,-4,java.util.Set.of(),0,15);});}
        if(ticks==280)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),80.5,102,-4,java.util.Set.of(),12,5);});
        if(ticks==295)shot(mc,"guide");
        if(ticks>300)mc.stop();
    }
    static void minecraftTake(Minecraft mc){mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,1);}
    static void shot(Minecraft mc,String n){Screenshot.grab(mc.gameDirectory,"bulk-"+n+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}

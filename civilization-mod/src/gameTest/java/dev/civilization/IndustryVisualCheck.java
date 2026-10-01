package dev.civilization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
final class IndustryVisualCheck {
    private static int ticks;private static volatile boolean ready;private static volatile Throwable failure;
    private static final BlockPos REF=new BlockPos(34,101,0),COL=new BlockPos(46,101,0);
    private static IndustrialBlockEntity machine(ServerLevel l,IndustrialBlock b,BlockPos at){l.setBlockAndUpdate(at,b.defaultBlockState());for(var p:IndustrialStructure.parts(b.kind))MachineStructure.placePart(l,at,Direction.NORTH,p);var m=(IndustrialBlockEntity)l.getBlockEntity(at);if(!IndustrialStructure.bind(m))throw new IllegalStateException("Incomplete fixture: "+b.kind);return m;}
    private static void pipe(ServerLevel l,int x,int y,int z){l.setBlockAndUpdate(new BlockPos(x,y,z),IndustrialContent.PIPE.get().defaultBlockState());}
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Industry fixture failed",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(45);mc.options.hideGui=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{try{
                var l=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                for(int x=15;x<80;x++)for(int z=-30;z<12;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<140;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                var ref=machine(l,IndustrialContent.REFINERY.get(),REF);CoalFireFixture.light(ref);var col=machine(l,IndustrialContent.COLUMN.get(),COL);machine(l,IndustrialContent.CONDENSER.get(),new BlockPos(60,101,0));
                var crude=machine(l,IndustrialContent.TANK.get(),new BlockPos(28,102,1));machine(l,IndustrialContent.TANK.get(),new BlockPos(68,103,2));machine(l,IndustrialContent.TANK.get(),new BlockPos(54,102,5));
                for(int x=29;x<=31;x++)pipe(l,x,102,1);
                for(int x=37;x<=43;x++)pipe(l,x,102,2);
                for(int x=49;x<=56;x++)pipe(l,x,110,2);for(int y=103;y<110;y++)pipe(l,56,y,2);
                for(int z=2;z<=5;z++)pipe(l,49,102,z);for(int x=50;x<54;x++)pipe(l,x,102,5);
                for(int x=64;x<68;x++)pipe(l,x,103,2);
                crude.input.fill(new FluidStack(IndustrialContent.CRUDE.get(),10000),EXECUTE);ref.output.fill(new FluidStack(IndustrialContent.HEATED.get(),2000),EXECUTE);ref.setItem(0,KilnContent.MINERAL_COAL.toStack(8));col.input.fill(new FluidStack(IndustrialContent.HEATED.get(),1000),EXECUTE);col.progress=col.duration()-IndustrialRates.STEP_TICKS;var condenser=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(60,101,0));condenser.input.fill(new FluidStack(IndustrialContent.VAPOR.get(),200),EXECUTE);condenser.progress=condenser.duration()-IndustrialRates.STEP_TICKS;
                player.getInventory().clearContent();int i=0;for(var item:java.util.List.of(IndustrialContent.PIPE.get().asItem(),IndustrialContent.PORT.get().asItem(),IndustrialContent.CRUDE_CAN.get(),IndustrialContent.FUEL_CAN.get(),IndustrialContent.LUBE_CAN.get(),IndustrialContent.SULFUR.get()))player.getInventory().setItem(i++,new net.minecraft.world.item.ItemStack(item));
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(l,34.5,101,-2,java.util.Set.of(),0,0);l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);player.openMenu(ref);ready=true;
            }catch(Throwable t){failure=t;}});
        }
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==150){if(!(mc.player.containerMenu instanceof IndustrialMenu m)||m.kind()!=IndustrialBlock.Kind.REFINERY)throw new IllegalStateException("Heater menu did not synchronize");shot(mc,"heater");}
        if(ticks==155)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),46.5,101,-2,java.util.Set.of(),0,0);p.openMenu((IndustrialBlockEntity)server.overworld().getBlockEntity(COL));});
        if(ticks==180){if(!(mc.player.containerMenu instanceof IndustrialMenu m)||m.kind()!=IndustrialBlock.Kind.COLUMN)throw new IllegalStateException("Column menu did not synchronize");shot(mc,"column");}
        if(ticks==185)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),64,110,-18,java.util.Set.of(),35,10);});
        if(ticks==190)mc.options.hideGui=true;
        if(ticks==230)shot(mc,"machines");
        if(ticks==235)server.execute(()->{var l=server.overworld();var output=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(68,103,2));if(output.input.getFluidAmount()==0){failure=new IllegalStateException("Connected refinery did not deliver engine fuel");return;}l.removeBlock(COL.offset(-1,0,0),false);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,46.5,101,-3,java.util.Set.of(),0,20);});
        if(ticks==240)mc.options.hideGui=false;
        if(ticks==270){shot(mc,"guide");com.mojang.logging.LogUtils.getLogger().info("INDUSTRY VISUAL VERIFIED");}
        if(ticks==275)server.execute(()->{try {
            var l=server.overworld();var at=new BlockPos(28,101,0);
            // Reuse the clear foreground after refinery checks; this fixture has no bearing on production saves.
            for(int x=26;x<=30;x++)for(int z=-1;z<=3;z++)for(int y=101;y<=107;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(at,KilnContent.FOUNDRY.get().defaultBlockState());
            for(var part:MachineStructure.parts(l.getBlockState(at)))MachineStructure.placePart(l,at,Direction.NORTH,part);
            var f=(FoundryBlockEntity)l.getBlockEntity(at);if(f.checkStructure().status()!=MachineStructure.COMPLETE)throw new IllegalStateException("Foundry fixture incomplete");
            f.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,4));f.setItem(1,KilnContent.MINERAL_COAL.toStack(4));CoalFireFixture.light(f);
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,28.5,101,-3,java.util.Set.of(),0,0);p.getInventory().setItem(0,KilnContent.STEEL.toStack());p.getInventory().setItem(1,KilnContent.MACHINE_PARTS.toStack());p.openMenu(f);
        }catch(Throwable t){failure=t;}});
        if(ticks==310){if(!(mc.player.containerMenu instanceof KilnMenu m)||m.getType()!=KilnContent.FOUNDRY_MENU.get()||m.operatingStatus()!=1)throw new IllegalStateException("Foundry menu did not synchronize working state");shot(mc,"foundry-menu");}
        if(ticks==315)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),34,105,-7,java.util.Set.of(),35,15);});
        if(ticks==320)mc.options.hideGui=true;
        if(ticks==345){shot(mc,"foundry");com.mojang.logging.LogUtils.getLogger().info("FOUNDRY VISUAL VERIFIED");}
        if(ticks==350)server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,39,103,-1,java.util.Set.of(),0,23);var ref=(IndustrialBlockEntity)l.getBlockEntity(REF);ref.output.fill(new FluidStack(IndustrialContent.HEATED.get(),4000),EXECUTE);l.setBlockAndUpdate(COL.offset(-1,0,0),IndustrialContent.CASING.get().defaultBlockState());var col=(IndustrialBlockEntity)l.getBlockEntity(COL);col.input.drain(4000,EXECUTE);col.output.drain(4000,EXECUTE);col.lubricant.drain(4000,EXECUTE);});
        if(ticks==375){var f=(PipeFlowEntity)mc.level.getBlockEntity(new BlockPos(39,102,2));if(f==null||f.fluids[Direction.EAST.ordinal()]!=3||f.until[Direction.EAST.ordinal()]<=mc.level.getGameTime())throw new IllegalStateException("Heated crude animation did not reach client");shot(mc,"pipe-glass");}
        if(ticks==379)shot(mc,"pipe-flow-next");
        if(ticks==385)server.execute(()->{try{var l=server.overworld();var pump=machine(l,IndustrialContent.PUMP.get(),new BlockPos(23,101,-12));
            var site=new Deposits.Site(23,101,-12,Deposits.Kind.OIL,2);l.setBlockAndUpdate(new BlockPos(site.x(),site.top(),site.z()),IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());
            var field=IndustrialBlockEntity.class.getDeclaredField("site");field.setAccessible(true);field.set(pump,site);field=IndustrialBlockEntity.class.getDeclaredField("surveyed");field.setAccessible(true);field.setBoolean(pump,true);
            pump.output.fill(new FluidStack(IndustrialContent.CRUDE.get(),2000),EXECUTE);pump.setItem(0,KilnContent.MINERAL_COAL.toStack(8));CoalFireFixture.light(pump);
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,35,117,-60,java.util.Set.of(),12,0);
        }catch(Throwable t){failure=t;}});
        if(ticks==390)mc.options.fov().set(65);
        if(ticks==420)shot(mc,"pump-front");
        if(ticks==425)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),23,117,40,java.util.Set.of(),180,0);});
        if(ticks==495)shot(mc,"pump-rear");
        if(ticks==500){mc.options.hideGui=false;server.execute(()->{var l=server.overworld();var at=new BlockPos(23,101,-12);var missing=IndustrialStructure.parts(IndustrialBlock.Kind.PUMP).stream().filter(part->part.material().equals("planks")&&part.y()>=2&&part.y()<=4).findFirst().orElseThrow();l.removeBlock(MachineStructure.position(at,Direction.NORTH,missing),false);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,23.5,102,-15,java.util.Set.of(),0,32);p.getInventory().setItem(0,IndustrialContent.PUMP.toStack());p.getInventory().setItem(1,CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),1,16));});}
        if(ticks==550)shot(mc,"pump-select");
        if(ticks==560)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),23.5,103,-17,java.util.Set.of(),0,0);});
        if(ticks==610)shot(mc,"pump-guide");
        if(ticks==615){mc.options.hideGui=true;server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();int i=0;for(var block:java.util.List.of(IndustrialContent.PUMP.get(),IndustrialContent.REFINERY.get(),IndustrialContent.COLUMN.get(),IndustrialContent.CONDENSER.get(),IndustrialContent.TANK.get(),IndustrialContent.DRILL.get())){l.setBlockAndUpdate(new BlockPos(22+i*2,101,-22),block.defaultBlockState());p.getInventory().setItem(i++,new net.minecraft.world.item.ItemStack(block));}p.teleportTo(l,27.5,102,-32,java.util.Set.of(),0,4);});}
        if(ticks==650)shot(mc,"controller-family");
        if(ticks==655){mc.options.hideGui=false;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==670)shot(mc,"controller-items");
        if(ticks>675)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"industry-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

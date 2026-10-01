package dev.civilization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
final class PhysicalDepositVisualCheck {
    private static int ticks;private static volatile boolean ready,oilReady;private static volatile Throwable failure;
    private static Deposits.Site oil,coal;private static IndustrialBlockEntity pump,drill;
    private static int stock(ServerLevel l,Deposits.Site s){int total=0;for(int i=0;i<s.cells();i++){var p=s.cell(i);if(!s.body(p))continue;var state=l.getBlockState(p);if(state.is(IndustrialContent.COAL_SEAM.get()))total+=16;else if(state.is(IndustrialContent.RESERVOIR_OIL.get()))total+=(8-state.getValue(LiquidBlock.LEVEL))*125;}return total;}
    private static void camera(Minecraft mc,Deposits.Site s){var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),s.x()+9,s.top()+5,s.z()-10,java.util.Set.of(),40,28);}
    private static IndustrialBlockEntity prepare(ServerLevel l,Deposits.Site s){
        for(int cx=(s.x()-s.radius())>>4;cx<=(s.x()+s.radius())>>4;cx++)for(int cz=(s.z()-s.radius())>>4;cz<=(s.z()+s.radius())>>4;cz++)l.getChunk(cx,cz);
        int expected=0;for(int i=0;i<s.cells();i++){var cell=s.cell(i);if(s.body(cell)&&(s.kind()==Deposits.Kind.COAL||cell.getY()<=s.top()))expected+=s.kind()==Deposits.Kind.COAL?16:1000;}
        int generated=stock(l,s);
        if(generated<=0||generated>expected)throw new IllegalStateException("Generated physical volume invalid: "+generated+" / "+expected+" "+s);
        if(s.kind()==Deposits.Kind.COAL){
            int exposed=0;
            for(int i=0;i<s.cells();i++){
                var p=s.cell(i);if(!s.body(p)||!l.getBlockState(p).is(IndustrialContent.COAL_SEAM.get()))continue;
                for(var side:Direction.values()){
                    var neighbor=l.getBlockState(p.relative(side));
                    if(neighbor.isAir()||!neighbor.getFluidState().isEmpty()){exposed++;break;}
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("NATURAL COAL EXPOSURE {} blocks, depth {}",exposed,s.depth());
            if(s.depth()<24 && exposed==0)throw new IllegalStateException("Selected outcrop is hidden by final terrain");
        }
        var clue=new BlockPos(s.x(),s.y()-1,s.z());
        if(s.kind()==Deposits.Kind.COAL && l.getBlockState(clue).is(IndustrialContent.COAL_SEAM.get()))throw new IllegalStateException("Coal has a separate surface marker");
        if(s.kind()==Deposits.Kind.OIL){
            if(!l.getBlockState(clue).is(IndustrialContent.OIL_SEEP.get()))throw new IllegalStateException("Missing oil seep");
            if(!l.getBlockState(clue.above()).is(IndustrialContent.SURFACE_OIL.get()))throw new IllegalStateException("Missing flowing surface oil over reservoir center");
            if(!l.getFluidTicks().hasScheduledTick(clue.above(),IndustrialContent.CRUDE.get()))throw new IllegalStateException("Generated oil seep has no initial flow tick");
        }
        // Expose a small center cutaway; clearing a quarter of a 150-block field is excessive.
        for(int x=s.x();x<=s.x()+12;x++)for(int z=s.z()-12;z<=s.z();z++)for(int y=s.kind()==Deposits.Kind.COAL?s.bottom():s.top()+1;y<s.y()+12;y++){var clear=new BlockPos(x,y,z);if(!l.getBlockState(clear).is(IndustrialContent.COAL_SEAM.get()))l.setBlock(clear,Blocks.AIR.defaultBlockState(),2);}
        var at=new BlockPos(s.x(),s.y(),s.z());var block=s.kind()==Deposits.Kind.COAL?IndustrialContent.DRILL.get():IndustrialContent.PUMP.get();l.setBlockAndUpdate(at,block.defaultBlockState());for(var part:IndustrialStructure.parts(block.kind))MachineStructure.placePart(l,at,Direction.NORTH,part);
        if(s.kind()==Deposits.Kind.OIL){
            var pool=new BlockPos(s.x()+s.radius()/2,s.y(),s.z()+s.radius()/2);
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
                l.setBlockAndUpdate(pool.offset(dx,-1,dz),Blocks.STONE.defaultBlockState());
                for(int dy=0;dy<=3;dy++)l.setBlockAndUpdate(pool.offset(dx,dy,dz),Blocks.AIR.defaultBlockState());
            }
            l.setBlockAndUpdate(pool,IndustrialContent.SURFACE_OIL.get().defaultBlockState());
        }
        com.mojang.logging.LogUtils.getLogger().info("PHYSICAL GENERATION VERIFIED {} {} / {} possible units across chunk boundaries",s.kind(),generated,expected);
        return (IndustrialBlockEntity)l.getBlockEntity(at);
    }
    static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Physical fixture",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.renderDistance().set(4);server.execute(()->{try{
            var l=server.overworld();int base=300+Math.floorMod((int)System.nanoTime(),5000);
            for(int x=base;x<base+30&&(oil==null||coal==null);x++)for(int z=base;z<base+30&&(oil==null||coal==null);z++){var s=Deposits.candidate(l,x,z);if(s==null)continue;if(s.kind()==Deposits.Kind.OIL&&oil==null)oil=s;else if(s.kind()==Deposits.Kind.COAL&&coal==null)coal=s;}
            if(oil==null||coal==null)throw new IllegalStateException("Missing candidate");pump=prepare(l,oil);drill=prepare(l,coal);
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,20000,0,false,false));l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);camera(mc,coal);ready=true;
        }catch(Throwable t){failure=t;}});}
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==230)shot(mc,"coal-before");
        if(ticks==235)server.execute(()->{try{var l=server.overworld();int before=stock(l,coal);drill.input.fill(new FluidStack(IndustrialContent.FUEL.get(),20*IndustrialRates.DRILL_FUEL_MB),EXECUTE);int produced=0;for(int i=0;i<250;i++){drill.process();produced+=drill.removeItem(1,32).getCount();}if(produced!=320||stock(l,coal)!=before-320)throw new IllegalStateException("Coal physical depletion/conservation failed");com.mojang.logging.LogUtils.getLogger().info("PHYSICAL DRILL VERIFIED 20 blocks removed, 320 coal");}catch(Throwable t){failure=t;}});
        if(ticks==270)shot(mc,"coal-after");
        if(ticks==275)server.execute(()->camera(mc,oil));
        if(ticks==320)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),oil.x()+oil.radius()/2+8,oil.y()+3,oil.z()+oil.radius()/2+8,java.util.Set.of(),135,25);});
        if(ticks==365)shot(mc,"oil-surface");
        if(ticks==375)server.execute(()->camera(mc,oil));
        if(ticks==405){if(net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(IndustrialContent.CRUDE.get()).getStillTexture()==null)throw new IllegalStateException("No oil texture registered");shot(mc,"oil-before");}
        if(ticks==410)server.execute(()->{try{
            var l=server.overworld();
            for(int cx=(oil.x()-oil.radius())>>4;cx<=(oil.x()+oil.radius())>>4;cx++)for(int cz=(oil.z()-oil.radius())>>4;cz<=(oil.z()+oil.radius())>>4;cz++)l.getChunk(cx,cz);
            pump=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(oil.x(),oil.y(),oil.z()));
            if(pump==null)throw new IllegalStateException("Oil pump controller missing after chunk reload");
            if(!IndustrialStructure.bind(pump)){
                for(var part:IndustrialStructure.parts(pump.kind)){var pos=MachineStructure.position(pump.getBlockPos(),pump.front(),part);if(!MachineStructure.matches(l,pos,part,pump.front()))throw new IllegalStateException("Oil pump part missing after chunk reload: "+part+" at "+pos+" found "+l.getBlockState(pos));}
                throw new IllegalStateException("Oil pump port binding failed after chunk reload");
            }
            int before=stock(l,oil);
            pump.setItem(0,KilnContent.MINERAL_COAL.toStack(32));
            CoalFireFixture.light(pump);
            int wanted=64_000,produced=0;
            for(int step=0;step<12000&&produced<wanted;step++){if(pump.getItem(0).isEmpty())pump.setItem(0,KilnContent.MINERAL_COAL.toStack(32));pump.process();produced+=pump.output.drain(4000,EXECUTE).getAmount();}
            if(produced!=wanted||stock(l,oil)!=before-produced)throw new IllegalStateException("Oil physical depletion/conservation failed "+produced+" / "+wanted+" (status "+pump.status+", formed "+pump.formed+", lit "+pump.fire.lit()+", loaded "+oil.loaded(l)+")");
            com.mojang.logging.LogUtils.getLogger().info("PHYSICAL PUMP VERIFIED {} mB removed from the enlarged reservoir",produced);
        }catch(Throwable t){failure=t;}finally{oilReady=true;}});
        if(ticks>410&&!oilReady){ticks=410;return;}
        if(ticks==470){shot(mc,"oil-after");com.mojang.logging.LogUtils.getLogger().info("PHYSICAL VISUAL VERIFIED");}
        if(ticks>480)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"physical-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

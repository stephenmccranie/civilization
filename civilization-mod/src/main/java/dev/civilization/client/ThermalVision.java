package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.civilization.*;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Fixed world-cell survey: thermal-camera surfaces and sparse exchange vectors. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class ThermalVision {
    private record Surface(BlockPos pos,Direction face,AABB bounds,float temperature) {}
    private record Flow(BlockPos pos,Vec3 direction,double strength) {}
    private record View(float air,Long2FloatOpenHashMap temperatures,
                        List<Surface> surfaces,List<Flow> flows) {}
    private static ThermalPayload data;
    private static Level world;
    private static long received;
    private static View view;

    @SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e){
        if(e.getItemStack().is(ThermalContent.HELMET.get()))
            e.getToolTip().add(net.minecraft.network.chat.Component.literal("Wear to see surface temperatures and heat-transfer arrows."));
    }

    public static void accept(ThermalPayload packet){
        var mc=Minecraft.getInstance();
        boolean sameWorld=world==mc.level;
        data=packet;world=mc.level;received=world==null?0:world.getGameTime();
        if(world==null||mc.player==null||!packet.supported()||!ThermalContent.wearing(mc.player))view=null;
        else if(!packet.samples().isEmpty())view=prepare(world,mc.player.blockPosition(),packet);
        else if(!sameWorld)view=null;
    }
    static ThermalPayload current(){var mc=Minecraft.getInstance();return mc.level!=null&&mc.level==world&&data!=null&&mc.level.getGameTime()-received<60?data:null;}
    private static boolean visible(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level==world&&data!=null&&mc.level.getGameTime()-received<60&&ThermalContent.wearing(mc.player)&&!mc.options.hideGui;}

    private static View prepare(Level level,BlockPos player,ThermalPayload packet){
        var readings=new Long2FloatOpenHashMap(packet.samples().size());
        for(var sample:packet.samples())readings.put(sample.pos().asLong(),sample.temperature());
        var eye=player.above();
        float air=readings.containsKey(eye.asLong())?readings.get(eye.asLong()):packet.temperature();
        var surfaces=new ArrayList<Surface>();var flows=new ArrayList<Flow>();
        for(var sample:packet.samples()){
            var pos=sample.pos();if(pos.distSqr(eye)>100||!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);float temperature=sample.temperature();
            if(state.isAir()){
                if(Math.floorMod(pos.getX(),2)!=0||Math.floorMod(pos.getY(),2)!=0||Math.floorMod(pos.getZ(),2)!=0)continue;
                var flow=flowAt(level,pos,temperature,readings);
                if(flow!=null&&pos.distSqr(eye)<=30)flows.add(flow);
            }else if(state.getRenderShape()==RenderShape.MODEL){
                var shape=state.getShape(level,pos);
                for(var bounds:shape.toAabbs())for(var face:Direction.values()){
                    var neighbor=pos.relative(face);
                    boolean boundary=switch(face){
                        case WEST -> bounds.minX<.001; case EAST -> bounds.maxX>.999;
                        case DOWN -> bounds.minY<.001; case UP -> bounds.maxY>.999;
                        case NORTH -> bounds.minZ<.001; case SOUTH -> bounds.maxZ>.999;
                    };
                    if(boundary&&level.hasChunkAt(neighbor)&&level.getBlockState(neighbor).isSolidRender(level,neighbor))continue;
                    surfaces.add(new Surface(pos,face,bounds,temperature));
                }
            }
        }
        flows.sort(Comparator.comparingDouble(Flow::strength).reversed());
        var strongest=new ArrayList<Flow>();
        for(var flow:flows){
            if(strongest.size()==14)break;
            boolean near=false;
            for(var kept:strongest)if(flow.pos().distSqr(kept.pos())<9){near=true;break;}
            if(!near)strongest.add(flow);
        }
        return new View(air,readings,List.copyOf(surfaces),List.copyOf(strongest));
    }

    private static Flow flowAt(Level level,BlockPos pos,double temperature,Long2FloatOpenHashMap readings){
        var state=level.getBlockState(pos);
        if(!state.isAir())return null;
        double x=0,y=0,z=0;
        for(var direction:Direction.values()){
            var neighbor=pos.relative(direction);long key=neighbor.asLong();
            if(!readings.containsKey(key)||!level.hasChunkAt(neighbor))continue;
            var other=level.getBlockState(neighbor);
            // Lava's displayed reservoir temperature is not the solver's stored cell temperature.
            if(other.getFluidState().is(FluidTags.LAVA))continue;
            double adjacent=readings.get(key);
            double rate=ThermalRules.exchangeRate(true,other.isAir(),direction,temperature,adjacent,
                    ThermalField.conductance(state),ThermalField.conductance(other));
            double transfer=(temperature-adjacent)*rate;
            x+=direction.getStepX()*transfer;y+=direction.getStepY()*transfer;z+=direction.getStepZ()*transfer;
        }
        double magnitude=Math.sqrt(x*x+y*y+z*z);
        return magnitude<.08?null:new Flow(pos,new Vec3(x/magnitude,y/magnitude,z/magnitude),magnitude);
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!visible()||view==null)return;
        var mc=Minecraft.getInstance();var graphics=event.getGuiGraphics();
        graphics.fill(6,6,302,78,0xc0182028);
        graphics.drawString(mc.font,String.format(Locale.ROOT,"THERMAL  Air %.0f°F  |  Feels %.0f°F",
                ThermalDisplay.fahrenheit(data.temperature()),ThermalDisplay.fahrenheit(data.playerTemperature())),12,12,0xffeadcb9);
        for(int x=0;x<274;x++)graphics.fill(12+x,28,13+x,36,
                ThermalDisplay.color((ThermalDisplay.legendFahrenheit(x/273.0)-32)/1.8));
        int marker=12+(int)Math.round(ThermalDisplay.legendPosition(view.air())*273);
        graphics.fill(marker-1,26,marker+2,38,0xffffffff);
        graphics.drawString(mc.font,"0°F",12,41,0xffeeeeee);
        graphics.drawString(mc.font,"70°F",106,41,0xffeeeeee);
        graphics.drawString(mc.font,"130°F",207,41,0xffeeeeee);
        graphics.drawString(mc.font,"300°F",257,41,0xffeeeeee);
        String target="Look at a block for its exact temperature";
        if(mc.hitResult instanceof BlockHitResult hit&&view.temperatures().containsKey(hit.getBlockPos().asLong())){
            double value=view.temperatures().get(hit.getBlockPos().asLong());
            target=String.format(Locale.ROOT,"LOOK  %.0f°F %s  |  %+.0f°F vs air",
                    ThermalDisplay.fahrenheit(value),heatLabel(value),(value-view.air())*1.8);
        }
        graphics.drawString(mc.font,target,12,54,0xffeadcb9);
        String age=Float.isFinite(data.solverAgeSeconds())?String.format(Locale.ROOT,"%.1fs",data.solverAgeSeconds()):"waiting";
        graphics.drawString(mc.font,"ARROWS  Strongest transfer  |  Field age "+age,12,66,
                data.solverAgeSeconds()>3?0xffff9977:0xffd9e2dc);
    }

    private static String heatLabel(double celsius){
        double f=ThermalDisplay.fahrenheit(celsius);
        return f<45?"COLD":f<60?"COOL":f<80?"MILD":f<110?"WARM":f<180?"HOT":"EXTREME";
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||!visible()||view==null)return;
        var mc=Minecraft.getInstance();if(mc.screen!=null)return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();
        var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        matrix.pushMatrix();matrix.mul(event.getModelViewMatrix());com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);pose.pushPose();
        try{
            pose.translate(-camera.x,-camera.y,-camera.z);
            var buffers=mc.renderBuffers().bufferSource();
            var surfaceVertices=buffers.getBuffer(PreviewRenderTypes.THERMAL_SURFACE);
            for(var surface:view.surfaces()){
                var box=surface.bounds();var face=surface.face();
                double x=(box.minX+box.maxX)*.5,y=(box.minY+box.maxY)*.5,z=(box.minZ+box.maxZ)*.5;
                switch(face){
                    case WEST -> x=box.minX; case EAST -> x=box.maxX;
                    case DOWN -> y=box.minY; case UP -> y=box.maxY;
                    case NORTH -> z=box.minZ; case SOUTH -> z=box.maxZ;
                }
                var center=Vec3.atLowerCornerOf(surface.pos()).add(x,y,z);
                var normal=new Vec3(surface.face().getStepX(),surface.face().getStepY(),surface.face().getStepZ());
                if(normal.dot(camera.subtract(center))<=.1)continue;
                var right=face.getAxis()==Direction.Axis.X?new Vec3(0,0,box.getZsize()):new Vec3(box.getXsize(),0,0);
                var up=face.getAxis()==Direction.Axis.Y?new Vec3(0,0,box.getZsize()):new Vec3(0,box.getYsize(),0);
                faceTint(pose,surfaceVertices,center.add(normal.scale(.008)),right,up,
                        .82f,ThermalDisplay.color(surface.temperature()));
            }
            buffers.endBatch(PreviewRenderTypes.THERMAL_SURFACE);
            var arrows=buffers.getBuffer(PreviewRenderTypes.THERMAL_FLOW);
            for(var flow:view.flows()){
                drawArrow(pose,arrows,flow);
            }
            buffers.endBatch(PreviewRenderTypes.THERMAL_FLOW);
            if(mc.hitResult instanceof BlockHitResult hit&&view.temperatures().containsKey(hit.getBlockPos().asLong())){
                var pos=hit.getBlockPos();int color=ThermalDisplay.color(view.temperatures().get(pos.asLong()));
                var lines=buffers.getBuffer(PreviewRenderTypes.OUTLINE);
                LevelRenderer.renderLineBox(pose,lines,new AABB(pos).inflate(.003),
                        ((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,.7f);
                buffers.endBatch(PreviewRenderTypes.OUTLINE);
            }
        }finally{pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }

    private static void faceTint(PoseStack pose,VertexConsumer vertices,Vec3 center,Vec3 right,Vec3 up,
                                 float alpha,int color){
        float red=((color>>16)&255)/255f,green=((color>>8)&255)/255f,blue=(color&255)/255f;
        var a=center.add(right.scale(-.5)).add(up.scale(-.5));
        var b=center.add(right.scale(.5)).add(up.scale(-.5));
        var c=center.add(right.scale(.5)).add(up.scale(.5));
        var d=center.add(right.scale(-.5)).add(up.scale(.5));
        vertex(pose,vertices,a,red,green,blue,alpha);
        vertex(pose,vertices,b,red,green,blue,alpha);
        vertex(pose,vertices,c,red,green,blue,alpha);
        vertex(pose,vertices,a,red,green,blue,alpha);
        vertex(pose,vertices,c,red,green,blue,alpha);
        vertex(pose,vertices,d,red,green,blue,alpha);
    }

    private static void drawArrow(PoseStack pose,VertexConsumer lines,Flow flow){
        var direction=flow.direction();var center=Vec3.atCenterOf(flow.pos());
        double length=.75+Math.min(.35,flow.strength()*.10);
        var start=center.add(direction.scale(-length*.5));
        var end=center.add(direction.scale(length*.5));
        var side=direction.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<.01)side=direction.cross(new Vec3(1,0,0));
        side=side.normalize().scale(.20);
        var other=direction.cross(side).normalize().scale(.20);
        line(pose,lines,start,end);
        for(var wing:List.of(side,side.scale(-1),other,other.scale(-1)))
            line(pose,lines,end,end.add(direction.scale(-.28)).add(wing));
    }

    private static void line(PoseStack pose,VertexConsumer lines,Vec3 a,Vec3 b){
        var normal=b.subtract(a).normalize();var transform=pose.last();
        lines.addVertex(transform,(float)a.x,(float)a.y,(float)a.z).setColor(1f,.96f,.72f,.92f)
                .setNormal(transform,(float)normal.x,(float)normal.y,(float)normal.z);
        lines.addVertex(transform,(float)b.x,(float)b.y,(float)b.z).setColor(1f,.96f,.72f,.92f)
                .setNormal(transform,(float)normal.x,(float)normal.y,(float)normal.z);
    }

    private static void vertex(PoseStack pose,VertexConsumer vertices,Vec3 at,float red,float green,float blue,float alpha){
        vertices.addVertex(pose.last().pose(),(float)at.x,(float)at.y,(float)at.z).setColor(red,green,blue,alpha);
    }
}

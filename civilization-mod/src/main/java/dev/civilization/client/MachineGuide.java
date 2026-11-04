package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/**
 * The construction guide of a machine built as a native model, for a selected controller or where a held one would go:
 * the missing sections as translucent model, red outlines on cells something else occupies, and the materials still due.
 * It shares sections with construction and damage, and follows the guide performance contract.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class MachineGuide {
    private record Site(BlockPos at,BlockState controller,MachineModel model,int built,boolean placed) {
        Direction front(){return controller.getValue(CivicBlock.FACING);}
    }
    private record UpdateKey(Site site,PlacementPlan plan) {}
    /** Immutable snapshot of a site; combine bounds, never voxel unions, for outlines. */
    public record Geometry(AABB area,List<Cell> cells,Map<String,Integer> materials) {}
    public record Cell(int index,BlockPos pos,AABB box) {}
    private static ModeledController selected;
    private static PlacementPlan plan;
    private static Site site;
    private static Geometry geometry;
    private static final GuideFocus focus=new GuideFocus();
    private static final GuidePerformance.Refresh refresh=new GuidePerformance.Refresh();
    private static ModeledMachineRenderer renderer;
    private static boolean visible;
    private static List<Cell> blocked=List.of(),outlineCells=List.of();
    private MachineGuide() {}
    private static void reset(){selected=null;plan=null;site=null;geometry=null;blocked=List.of();outlineCells=List.of();visible=false;focus.reset();refresh.reset();}
    private static MachineModel model(BlockState state){return state.getBlock() instanceof ModeledBlock b?b.model():null;}

    private static Geometry geometry(Site site){return geometry(site.at(),site.front(),site.model(),site.built());}
    public static Geometry geometry(BlockPos at,Direction front,MachineModel model,int built){
        var area=new AABB(at);var cells=new ArrayList<Cell>();int missing=model.all^built;
        for(int i=0;i<model.cells.size();i++){
            var cell=model.cells.get(i);var pos=ModeledMachine.position(at,front,cell);
            area=area.minmax(new AABB(pos));AABB bounds=null;
            for(var entry:cell.pieces().entrySet())if((missing&(1<<entry.getKey()))!=0){
                var box=entry.getValue()[front.get2DDataValue()].bounds();bounds=bounds==null?box:bounds.minmax(box);
            }
            if(bounds!=null)cells.add(new Cell(i,pos,bounds.move(pos)));
        }
        for(var port:model.ports)if((missing&(1<<port.section()))!=0){var pos=MachineStructure.position(at,front,port.part());cells.add(new Cell(-1,pos,new AABB(pos)));}
        var materials=new LinkedHashMap<String,Integer>();
        for(int part=0;part<model.sections.size();part++)if((missing&(1<<part))!=0)materials.merge(model.section(part).material(),model.section(part).count(),Integer::sum);
        return new Geometry(area,List.copyOf(cells),materials);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){reset();return;}
        plan=PlacementPlan.of(mc);if(plan!=null&&model(plan.state())==null)plan=null;
        if(mc.screen==null&&mc.hitResult instanceof BlockHitResult hit){
            var state=mc.level.getBlockState(hit.getBlockPos());
            var c=state.getBlock() instanceof MachinePartBlock?MachinePartBlock.owner(mc.level,hit.getBlockPos(),state):ModeledMachine.of(mc.level.getBlockEntity(hit.getBlockPos()));
            if(c!=null)selected=c;
        }
        if(selected!=null){var e=selected.entity();if(e.getLevel()!=mc.level||e.isRemoved()||e.getBlockPos().distToCenterSqr(mc.player.position())>32*32)selected=null;}
        var next=plan!=null?new Site(plan.at(),plan.state(),model(plan.state()),0,false)
                :selected!=null?new Site(selected.entity().getBlockPos(),selected.entity().getBlockState(),selected.model(),selected.build().sections,true):null;
        if(next==null){reset();return;}
        if(!next.equals(site)){site=next;geometry=geometry(site);focus.reset();outlineCells=geometry.cells();}
        if(refresh.due(new UpdateKey(site,plan))){
            var cells=new ArrayList<Cell>();
            for(var cell:geometry.cells()){
                if(cell.index()<0||!mc.level.hasChunkAt(cell.pos()))continue;
                var state=mc.level.getBlockState(cell.pos());
                if(!state.isAir()&&!state.equals(ModeledMachine.cellState(site.model(),cell.index(),site.front())))cells.add(cell);
            }
            for(var cell:geometry.cells())if(cell.index()<0)cells.add(cell);
            blocked=List.copyOf(cells);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();visible=false;
        if(site==null||geometry==null||mc.screen!=null||mc.options.hideGui||site.built()==site.model().all)return;
        var at=site.at();var area=geometry.area();
        var camera=event.getCamera();var eye=camera.getPosition();var look=camera.getLookVector();
        var end=eye.add(look.x()*8,look.y()*8,look.z()*8);
        // A held controller's placement shows wherever it is aimed.
        visible=plan!=null||focus.update(area.contains(eye)||area.clip(eye,end).isPresent(),net.minecraft.Util.getMillis());
        if(!visible||event.getFrustum()!=null&&!event.getFrustum().isVisible(area))return;
        if(renderer==null)renderer=new ModeledMachineRenderer(null);
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();matrix.pushMatrix();matrix.mul(event.getModelViewMatrix());com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);pose.pushPose();
        try{
            pose.translate(at.getX()-eye.x,at.getY()-eye.y,at.getZ()-eye.z);
            boolean textured=PreviewConfig.MODE.get()==PreviewConfig.Mode.TEXTURED;
            // A refused placement spot is outlined in red only.
            if(textured&&(plan==null||plan.valid())){
                var type=PreviewRenderTypes.model(site.model().geo);
                var ghost=new TranslucentVertexConsumer(buffers.getBuffer(type),PreviewConfig.OPACITY.get().floatValue());
                renderer.renderGuide(mc.level,at,site.controller(),site.model(),site.built(),site.placed(),event.getPartialTick().getGameTimeDeltaPartialTick(false),pose,unused->ghost);
                buffers.endBatch(type);
            }
            pose.popPose();pose.pushPose();pose.translate(-eye.x,-eye.y,-eye.z);
            // Ports share the same visible-outline budget as the cells.
            var outlined=GuidePerformance.outlines(textured?blocked:outlineCells,cell->event.getFrustum()==null||event.getFrustum().isVisible(cell.box()),cell->cell.box().getCenter().distanceToSqr(eye),null);
            for(var cell:outlined){boolean wrong=cell.index()>=0&&(textured||blocked.contains(cell));net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),cell.box(),wrong?1:.5f,wrong?.2f:1,wrong?.2f:1,cell.index()<0?.7f:.5f);}
            if(plan!=null)net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),new AABB(at).inflate(.002),
                    plan.valid()?.5f:1,plan.valid()?1:.2f,plan.valid()?1:.2f,plan.valid()?.65f:.8f);
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        }finally{pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!visible||site==null||geometry==null)return;var mc=Minecraft.getInstance();var g=event.getGuiGraphics();
        g.drawString(mc.font,Component.empty().append(site.controller().getBlock().getName()).append(" · "+Integer.bitCount(site.built())+" / "+site.model().sections.size()+" sections"),8,8,0xffffd180);
        if(plan!=null)g.drawString(mc.font,Component.translatable(plan.valid()?"preview.civilization.place":"preview.civilization.blocked"),8,20,plan.valid()?0xffffffff:0xffff8888);
        else g.drawString(mc.font,Component.literal("Right-click the machine with held materials"),8,20,0xffffffff);
        int y=34;for(var e:geometry.materials().entrySet()){g.drawString(mc.font,e.getValue()+"× "+e.getKey(),8,y,0xffdddddd);y+=10;}
    }
}

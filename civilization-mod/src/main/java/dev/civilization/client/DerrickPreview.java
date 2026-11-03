package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** The contextual guide uses the same native sections as construction and damage, for a selected pump or where a held one would go. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class DerrickPreview {
    private record Site(BlockPos at,Direction front,int built) {}
    private record UpdateKey(Site site,PlacementPlan plan) {}
    private static IndustrialBlockEntity selected;
    private static PlacementPlan plan;
    private static Site site;
    private static final GuideFocus focus=new GuideFocus();
    private static OilDerrickRenderer renderer;
    private static boolean visible;
    private static final GuidePerformance.Refresh refresh=new GuidePerformance.Refresh();
    private static DerrickGuideGeometry geometry;
    private static java.util.List<DerrickGuideGeometry.Cell> blocked=java.util.List.of(),outlineCells=java.util.List.of();
    private static DerrickGuideGeometry.Cell portCell;
    private static void reset(){selected=null;plan=null;site=null;geometry=null;blocked=java.util.List.of();outlineCells=java.util.List.of();portCell=null;visible=false;focus.reset();refresh.reset();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){reset();return;}
        plan=PlacementPlan.of(mc);if(plan!=null&&!plan.state().is(IndustrialContent.PUMP.get()))plan=null;
        if(mc.screen==null&&mc.hitResult instanceof BlockHitResult hit){
            var state=mc.level.getBlockState(hit.getBlockPos());
            if(state.is(IndustrialContent.PUMP.get())&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof IndustrialBlockEntity m)selected=m;
            else if(state.is(IndustrialContent.DERRICK_PART.get())){var m=DerrickPartBlock.owner(mc.level,hit.getBlockPos(),state);if(m!=null)selected=m;}
        }
        if(selected!=null&&(selected.getLevel()!=mc.level||selected.isRemoved()||selected.getBlockPos().distToCenterSqr(mc.player.position())>32*32))selected=null;
        var next=plan!=null?new Site(plan.at(),plan.state().getValue(CivicBlock.FACING),0)
                :selected!=null?new Site(selected.getBlockPos(),selected.front(),selected.derrickSections):null;
        if(next==null){reset();return;}
        if(!next.equals(site)){
            site=next;geometry=DerrickGuideGeometry.build(site.at(),site.front(),site.built());focus.reset();
            var port=MachineStructure.position(site.at(),site.front(),ModeledDerrick.PORT);
            portCell=(site.built()&(1<<24))!=0?null:new DerrickGuideGeometry.Cell(-1,port,new AABB(port));
            var cells=new java.util.ArrayList<>(geometry.cells());if(portCell!=null)cells.add(portCell);outlineCells=java.util.List.copyOf(cells);
        }
        if(refresh.due(new UpdateKey(site,plan))){
            var cells=new java.util.ArrayList<DerrickGuideGeometry.Cell>();
            for(var cell:geometry.cells())if(mc.level.hasChunkAt(cell.pos())){var state=mc.level.getBlockState(cell.pos());if(!state.isAir()&&!state.equals(ModeledDerrick.state(cell.index(),site.front())))cells.add(cell);}
            if(portCell!=null)cells.add(portCell);blocked=java.util.List.copyOf(cells);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();visible=false;
        if(site==null||geometry==null||mc.screen!=null||mc.options.hideGui||site.built()==ModeledDerrick.ALL)return;
        var at=site.at();var area=geometry.area();
        var camera=event.getCamera();var eye=camera.getPosition();var look=camera.getLookVector();
        var end=eye.add(look.x()*8,look.y()*8,look.z()*8);
        // A held pump's placement shows wherever it is aimed.
        visible=plan!=null||focus.update(area.contains(eye)||area.clip(eye,end).isPresent(),net.minecraft.Util.getMillis());if(!visible||event.getFrustum()!=null&&!event.getFrustum().isVisible(area))return;
        if(renderer==null)renderer=new OilDerrickRenderer(null);
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();matrix.pushMatrix();matrix.mul(event.getModelViewMatrix());com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);pose.pushPose();
        try{
            pose.translate(at.getX()-eye.x,at.getY()-eye.y,at.getZ()-eye.z);
            boolean textured=PreviewConfig.MODE.get()==PreviewConfig.Mode.TEXTURED;
            if(textured){
                var ghost=new TranslucentVertexConsumer(buffers.getBuffer(PreviewRenderTypes.DERRICK),PreviewConfig.OPACITY.get().floatValue());
                renderer.renderGuide(mc.level,at,site.front(),site.built(),event.getPartialTick().getGameTimeDeltaPartialTick(false),pose,type->ghost);
                buffers.endBatch(PreviewRenderTypes.DERRICK);
                // The pump itself, as a right-click would place it; a refused spot is outlined in red only.
                if(plan!=null&&plan.valid()){
                    var pump=new TranslucentVertexConsumer(buffers.getBuffer(PreviewRenderTypes.TEXTURED),PreviewConfig.OPACITY.get().floatValue());
                    mc.getBlockRenderer().renderSingleBlock(plan.state(),pose,type->pump,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,net.neoforged.neoforge.client.model.data.ModelData.EMPTY,PreviewRenderTypes.TEXTURED);
                    buffers.endBatch(PreviewRenderTypes.TEXTURED);
                }
            }
            pose.popPose();pose.pushPose();pose.translate(-eye.x,-eye.y,-eye.z);
            // The port shares the same visible-outline budget as the frame.
            var outlined=GuidePerformance.outlines(textured?blocked:outlineCells,cell->event.getFrustum()==null||event.getFrustum().isVisible(cell.box()),cell->cell.box().getCenter().distanceToSqr(eye),null);
            for(var cell:outlined){boolean wrong=cell.index()>=0&&(textured||blocked.contains(cell));net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),cell.box(),wrong?1:.5f,wrong?.2f:1,wrong?.2f:1,cell.index()<0?.7f:.5f);}
            if(plan!=null)net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),new AABB(at).inflate(.002),
                    plan.valid()?.5f:1,plan.valid()?1:.2f,plan.valid()?1:.2f,plan.valid()?.65f:.8f);
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        }finally{pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!visible||site==null||geometry==null)return;var mc=Minecraft.getInstance();var g=event.getGuiGraphics();int[] need=geometry.materials();
        g.drawString(mc.font,Component.literal("Oil Derrick · "+Integer.bitCount(site.built())+" / 25 sections"),8,8,0xffffd180);
        if(plan!=null)g.drawString(mc.font,Component.translatable(plan.valid()?"preview.civilization.place":"preview.civilization.blocked"),8,20,plan.valid()?0xffffffff:0xffff8888);
        else g.drawString(mc.font,Component.literal("Right-click controller with held materials"),8,20,0xffffffff);
        String[] names={"planks","iron blocks","stone/cobblestone","Refinery Port"};int y=34;
        for(int i=0;i<4;i++)if(need[i]>0){g.drawString(mc.font,need[i]+"× "+names[i],8,y,0xffdddddd);y+=10;}
    }
}

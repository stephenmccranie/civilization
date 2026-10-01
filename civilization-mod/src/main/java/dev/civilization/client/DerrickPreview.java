package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** The contextual guide uses the same native sections as construction and damage. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class DerrickPreview {
    private static IndustrialBlockEntity selected;
    private static final GuideFocus focus=new GuideFocus();
    private static OilDerrickRenderer renderer;
    private static boolean visible;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){selected=null;focus.reset();return;}
        if(mc.screen==null&&mc.hitResult instanceof BlockHitResult hit){
            var state=mc.level.getBlockState(hit.getBlockPos());
            if(state.is(IndustrialContent.PUMP.get())&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof IndustrialBlockEntity m)selected=m;
            else if(state.is(IndustrialContent.DERRICK_PART.get())){var m=DerrickPartBlock.owner(mc.level,hit.getBlockPos(),state);if(m!=null)selected=m;}
        }
        if(selected!=null&&(selected.getLevel()!=mc.level||selected.isRemoved()||selected.getBlockPos().distToCenterSqr(mc.player.position())>32*32)){selected=null;focus.reset();}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();visible=false;
        if(selected==null||mc.screen!=null||mc.options.hideGui||selected.derrickSections==ModeledDerrick.ALL)return;
        var at=selected.getBlockPos();var area=new AABB(at);
        for(var cell:ModeledDerrick.CELLS)area=area.minmax(new AABB(ModeledDerrick.position(at,selected.front(),cell)));
        var camera=event.getCamera();var eye=camera.getPosition();var look=camera.getLookVector();
        var end=eye.add(look.x()*8,look.y()*8,look.z()*8);
        visible=focus.update(area.contains(eye)||area.clip(eye,end).isPresent(),net.minecraft.Util.getMillis());if(!visible)return;
        if(renderer==null)renderer=new OilDerrickRenderer(null);
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();matrix.pushMatrix();matrix.mul(event.getModelViewMatrix());com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);pose.pushPose();
        try{
            pose.translate(at.getX()-eye.x,at.getY()-eye.y,at.getZ()-eye.z);
            if(PreviewConfig.MODE.get()==PreviewConfig.Mode.TEXTURED){
                var ghost=new TranslucentVertexConsumer(buffers.getBuffer(PreviewRenderTypes.DERRICK),PreviewConfig.OPACITY.get().floatValue());
                renderer.renderGuide(selected,event.getPartialTick().getGameTimeDeltaPartialTick(false),pose,type->ghost);
                buffers.endBatch(PreviewRenderTypes.DERRICK);
            }
            pose.popPose();pose.pushPose();pose.translate(-eye.x,-eye.y,-eye.z);
            // Outline blocked cells in red; outline mode shows all missing collision cells.
            for(int i=0;i<ModeledDerrick.CELLS.size();i++){
                var cell=ModeledDerrick.CELLS.get(i);var missing=cell.shape(selected.front(),ModeledDerrick.ALL^selected.derrickSections);if(missing.isEmpty())continue;
                var p=ModeledDerrick.position(at,selected.front(),cell);var state=mc.level.getBlockState(p);boolean blocked=!state.isAir()&&!state.equals(ModeledDerrick.state(i,selected.front()));
                if(blocked||PreviewConfig.MODE.get()!=PreviewConfig.Mode.TEXTURED)net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),missing.bounds().move(p),blocked?1:.5f,blocked?.2f:1,blocked?.2f:1,.5f);
            }
            var port=MachineStructure.position(at,selected.front(),ModeledDerrick.PORT);
            if(!ModeledDerrick.has(selected,24))net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),new AABB(port),.5f,1,1,.7f);
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        }finally{pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!visible||selected==null)return;var mc=Minecraft.getInstance();var g=event.getGuiGraphics();int[] need=new int[4];
        for(int part=0;part<ModeledDerrick.PARTS;part++)if(!ModeledDerrick.has(selected,part))need[ModeledDerrick.material(part)]+=ModeledDerrick.cost(part);
        g.drawString(mc.font,Component.literal("Oil Derrick · "+Integer.bitCount(selected.derrickSections)+" / 25 sections"),8,8,0xffffd180);
        g.drawString(mc.font,Component.literal("Right-click controller with held materials"),8,20,0xffffffff);
        String[] names={"planks","iron blocks","stone/cobblestone","Refinery Port"};int y=34;
        for(int i=0;i<4;i++)if(need[i]>0){g.drawString(mc.font,need[i]+"× "+names[i],8,y,0xffdddddd);y+=10;}
    }
}

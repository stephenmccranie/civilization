package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class BuilderLineClient {
    private static BuilderLinePayload.State state;
    private static boolean pressed,valid;
    private static List<BlockPos> cells=List.of();
    private static List<AABB> boxes=List.of();
    private static Set<AABB> blocked=Set.of();
    private static Vec3 cordA=Vec3.ZERO,cordB=Vec3.ZERO;
    private static final GuidePerformance.Refresh REFRESH=new GuidePerformance.Refresh();
    public static void accept(BuilderLinePayload.State s){state=s.selected()?s:null;REFRESH.reset();}
    public static boolean selected(){return state!=null;}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||!BuilderLineSystem.equipped(mc.player)||!BuilderLineSystem.supported(mc.player.getMainHandItem()))return;
        if(!e.isUseItem())return;e.setCanceled(true);e.setSwingHand(false);
        if(e.getHand()!=InteractionHand.MAIN_HAND||pressed)return;
        pressed=true;PacketDistributor.sendToServer(new BuilderLinePayload.Mark(mc.options.keyShift.isDown(),mc.player.getYRot(),mc.player.getXRot()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(!mc.options.keyUse.isDown())pressed=false;
        if(mc.player==null||mc.level==null||!BuilderLineSystem.equipped(mc.player)||!BuilderLineSystem.supported(mc.player.getMainHandItem())){state=null;cells=List.of();boxes=List.of();REFRESH.reset();return;}
        if(state==null){cells=List.of();boxes=List.of();return;}
        BlockPos b=state.active()?state.b():mc.hitResult instanceof BlockHitResult h&&h.getType()==HitResult.Type.BLOCK?BuilderLineSystem.target(mc.player,h):state.a();
        if(!REFRESH.due(List.of(state,b,mc.player.getMainHandItem().getItem())))return;
        cells=BuilderLineSystem.line(state.a(),b);valid=!cells.isEmpty();
        if(!valid)cells=List.of(state.a(),b);
        if(state.active()&&valid)cells=cells.subList(Math.min(state.completed(),cells.size()),cells.size());
        boxes=cells.stream().map(p->new AABB(p).inflate(.002)).toList();
        var bad=new HashSet<AABB>();boolean building=mc.player.getMainHandItem().getItem() instanceof BlockItem;
        for(int i=0;i<cells.size();i++){
            var pos=cells.get(i);var s=mc.level.getBlockState(pos);
            if(building?(!s.isAir()||!mc.level.getFluidState(pos).isEmpty()||!mc.level.isUnobstructed(null,net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ()))):(!BuilderLineSystem.plain(s)||!mc.player.hasCorrectToolForDrops(s)))bad.add(boxes.get(i));
        }
        blocked=Set.copyOf(bad);cordA=state.a().getCenter();cordB=b.getCenter();
        if(!state.active())mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(valid?cells.size()+" blocks — right-click to "+(mc.player.getMainHandItem().getItem() instanceof BlockItem?"build":"mine"):"Align endpoints on one axis; maximum 16 blocks"),true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||boxes.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui)return;
        var camera=e.getCamera().getPosition();var visible=GuidePerformance.outlines(boxes,e.getFrustum()::isVisible,b->b.getCenter().distanceToSqr(camera),null);
        var pose=e.getPoseStack();var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();matrix.pushMatrix();matrix.mul(e.getModelViewMatrix());com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();mc.getMainRenderTarget().bindWrite(false);pose.pushPose();
        try{
            pose.translate(-camera.x,-camera.y,-camera.z);var buffers=mc.renderBuffers().bufferSource();boolean building=mc.player.getMainHandItem().getItem() instanceof BlockItem;
            for(var box:visible){
                boolean clear=valid&&!blocked.contains(box);
                if(clear&&building&&PreviewConfig.MODE.get()==PreviewConfig.Mode.TEXTURED){
                    pose.pushPose();pose.translate(box.minX+.002,box.minY+.002,box.minZ+.002);
                    var consumer=new TranslucentVertexConsumer(buffers.getBuffer(PreviewRenderTypes.TEXTURED),PreviewConfig.OPACITY.get().floatValue());
                    mc.getBlockRenderer().renderSingleBlock(((BlockItem)mc.player.getMainHandItem().getItem()).getBlock().defaultBlockState(),pose,type->consumer,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,net.neoforged.neoforge.client.model.data.ModelData.EMPTY,PreviewRenderTypes.TEXTURED);pose.popPose();
                }
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),box,clear?(building?.5f:1):1,clear?(building?1:.65f):.2f,clear?(building?1:.2f):.2f,.8f);
            }
            var lines=buffers.getBuffer(PreviewRenderTypes.OUTLINE);var normal=cordB.subtract(cordA).normalize();var entry=pose.last();
            if(normal.lengthSqr()>0){lines.addVertex(entry,(float)cordA.x,(float)cordA.y,(float)cordA.z).setColor(.86f,.81f,.64f,.9f).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);lines.addVertex(entry,(float)cordB.x,(float)cordB.y,(float)cordB.z).setColor(.86f,.81f,.64f,.9f).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);}
            buffers.endBatch(PreviewRenderTypes.TEXTURED);buffers.endBatch(PreviewRenderTypes.OUTLINE);
        }finally{pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }
}

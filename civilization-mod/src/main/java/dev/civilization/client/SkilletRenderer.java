package dev.civilization.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;

/** One shared vessel model and food rendering in world, hand and inventory. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SkilletRenderer implements BlockEntityRenderer<RestingSkilletEntity> {
    private static final ResourceLocation FOOD=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    private static final java.util.Map<BlockPos,java.util.List<StoveSound>> SOUNDS=new java.util.HashMap<>();
    public SkilletRenderer(BlockEntityRendererProvider.Context c) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) { e.registerBlockEntityRenderer(PrototypeStoveContent.RESTING_ENTITY.get(),SkilletRenderer::new); }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent e) {
        e.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer==null) { var mc=Minecraft.getInstance(); renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()) {
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack p,MultiBufferSource buffers,int light,int overlay) {
                        p.pushPose();p.translate(0,.35,0);vessel(p,buffers,light,overlay);food(SkilletItem.contents(stack,mc.level),p,buffers,light,overlay);p.popPose();
                    }
                }; } return renderer;
            }
        },PrototypeStoveContent.SKILLET.get());
    }
    public static void rotation(BlockEntity b,PoseStack p) {
        p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(b.getBlockState().getValue(CivicBlock.FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));p.translate(-.5,0,-.5);
    }
    public static <T extends BlockEntity & SkilletHolder> void sound(T b) {
        var manager=Minecraft.getInstance().getSoundManager();SOUNDS.values().removeIf(list->list.getFirst().ended());
        if (!b.skillet().batch()||b.skillet().warmth()==0) return;
        var loops=SOUNDS.get(b.getBlockPos());
        if(loops!=null&&!loops.getFirst().owns(b)){for(var s:loops)s.end();SOUNDS.remove(b.getBlockPos());loops=null;}
        if(loops==null){loops=java.util.List.of(new StoveSound(b,0),new StoveSound(b,1),new StoveSound(b,2));SOUNDS.put(b.getBlockPos(),loops);}
        for(var s:loops)if(!manager.isActive(s))manager.play(s);
    }
    public static void vessel(PoseStack p,MultiBufferSource buffers,int light,int overlay) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(PrototypeStoveContent.RESTING_SKILLET.get().defaultBlockState(),p,buffers,light,overlay);
    }
    public static void food(SkilletContents s,PoseStack p,MultiBufferSource buffers,int light,int overlay) {
        if(!s.batch())return;
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(FOOD));
        for(int i=0;i<24;i++){
            float x=(4.2f+(i%6)*1.45f+((i/6)%2)*.3f)/16,z=(3.2f+(i/6)*2.1f+(i%3)*.2f)/16,y=(1.3f+(i%4)*.25f)/16;
            int color=StoveCooking.color(s.work()+(i%5-2)*14,i%3==0);if(i%4==1)color=StoveCooking.color(s.work()+220,false);
            PrototypeStoveRenderer.box(v,p,x,y,z,x+(.0625f+(i%3)*.008f),y+.06f+(i%2)*.025f,z+.085f+(i%3)*.01f,color,light,overlay);
        }
    }
    @Override public void render(RestingSkilletEntity b,float dt,PoseStack p,MultiBufferSource buffers,int light,int overlay) {
        sound(b);p.pushPose();rotation(b,p);food(b.skillet(),p,buffers,light,overlay);p.popPose();
    }
}

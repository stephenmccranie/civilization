package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Complete native tower, backed by invisible collision cells. */
@EventBusSubscriber(modid="civilization", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class OilDerrickRenderer implements BlockEntityRenderer<IndustrialBlockEntity> {
    private final GeoBlockRenderer<DerrickAnimation> renderer=new GeoBlockRenderer<>(new MachineGeoModel<>("oil_derrick"));
    private final java.util.Map<IndustrialBlockEntity,DerrickAnimation> instances=new java.util.WeakHashMap<>();
    public OilDerrickRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),OilDerrickRenderer::new);}
    @Override public AABB getRenderBoundingBox(IndustrialBlockEntity m){return new AABB(m.getBlockPos()).inflate(10,34,10);}
    @Override public int getViewDistance(){return 128;}

    @Override public void render(IndustrialBlockEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.kind!=IndustrialBlock.Kind.PUMP || !m.derrickBuilt || m.getLevel()==null)return;
        var a=instances.computeIfAbsent(m,DerrickAnimation::new);a.setLevel(m.getLevel());a.setBlockState(m.getBlockState());a.running=m.formed&&m.getBlockState().getValue(MachineFeedback.WORKING);
        renderer.render(a,partial,pose,buffers,light,overlay);
    }

    private static final class DerrickAnimation extends BlockEntity implements GeoBlockEntity {
        private static final RawAnimation CYCLE=RawAnimation.begin().thenLoop("animation.oil_derrick.cycle");
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);private boolean running;
        DerrickAnimation(IndustrialBlockEntity m){super(IndustrialContent.ENTITY.get(),m.getBlockPos(),m.getBlockState());}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<DerrickAnimation>(this,"work",0,state->{state.setControllerSpeed(running?1:0);return state.setAndContinue(CYCLE);}));}
    }
}

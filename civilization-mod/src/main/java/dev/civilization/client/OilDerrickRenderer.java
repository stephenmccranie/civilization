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
    private final GeoBlockRenderer<DerrickAnimation> renderer=new GeoBlockRenderer<>(new MachineGeoModel<>("oil_derrick")){
        @Override public void preRender(PoseStack pose,DerrickAnimation a,software.bernie.geckolib.cache.object.BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            super.preRender(pose,a,model,buffers,vertices,reRender,partial,light,overlay,color);
            if(visibilityModel!=model){visibilityModel=model;visibilityBones.clear();for(var bone:model.topLevelBones())indexVisibility(bone);}
            for(var entry:visibilityBones)entry.bone().setHidden(entry.part()>=0?(a.mask&(1<<entry.part()))==0:a.guide&&entry.controller());
        }
    };
    private record Visibility(software.bernie.geckolib.cache.object.GeoBone bone,int part,boolean controller) {}
    private software.bernie.geckolib.cache.object.BakedGeoModel visibilityModel;
    private final java.util.List<Visibility> visibilityBones=new java.util.ArrayList<>();
    private void indexVisibility(software.bernie.geckolib.cache.object.GeoBone bone){
        String name=bone.getName();int part=name.startsWith("section_")?Integer.parseInt(name.substring(8,name.indexOf('_',8))):-1;
        visibilityBones.add(new Visibility(bone,part,name.equals("controller")));for(var child:bone.getChildBones())indexVisibility(child);
    }
    private final java.util.Map<IndustrialBlockEntity,DerrickAnimation> instances=new java.util.WeakHashMap<>();
    public OilDerrickRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),OilDerrickRenderer::new);}
    @Override public AABB getRenderBoundingBox(IndustrialBlockEntity m){return new AABB(m.getBlockPos()).inflate(10,34,10);}
    @Override public int getViewDistance(){return 128;}

    @Override public void render(IndustrialBlockEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.kind!=IndustrialBlock.Kind.PUMP || m.getLevel()==null)return;
        var a=instances.computeIfAbsent(m,DerrickAnimation::new);a.setLevel(m.getLevel());a.setBlockState(m.getBlockState());a.mask=m.derrickSections;a.guide=false;a.running=m.formed&&m.getBlockState().getValue(MachineFeedback.WORKING);
        renderer.render(a,partial,pose,buffers,light,overlay);
    }

    public void renderGuide(IndustrialBlockEntity m,float partial,PoseStack pose,MultiBufferSource buffers){
        var a=instances.computeIfAbsent(m,DerrickAnimation::new);a.setLevel(m.getLevel());a.setBlockState(m.getBlockState());a.running=false;a.guide=true;a.mask=ModeledDerrick.ALL^m.derrickSections;
        renderer.render(a,partial,pose,buffers,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }
    private static final class DerrickAnimation extends BlockEntity implements GeoBlockEntity {
        private static final RawAnimation CYCLE=RawAnimation.begin().thenLoop("animation.oil_derrick.cycle");
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);private boolean running,guide;private int mask;
        DerrickAnimation(IndustrialBlockEntity m){super(IndustrialContent.ENTITY.get(),m.getBlockPos(),m.getBlockState());}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<DerrickAnimation>(this,"work",0,state->{state.setControllerSpeed(running?1:0);return state.setAndContinue(CYCLE);}));}
    }
}

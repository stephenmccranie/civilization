package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import java.util.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Draws any machine built as a native model: its controller and built sections, its lit or cold bones and its work
 * animation while running. The same renderer draws the guide of missing sections. Bones carry the meaning in their
 * names: `section_<n>_…` belongs to section n, `controller` is the controller, `lit_…` and `cold_…` follow the fire.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ModeledMachineRenderer implements BlockEntityRenderer<BlockEntity> {
    private record Visibility(GeoBone bone,int section,boolean controller,int fire) {}
    private static final class Renderer extends GeoBlockRenderer<Look> {
        private BakedGeoModel indexed;
        private final List<Visibility> bones=new ArrayList<>();
        Renderer(MachineModel model){super(new MachineGeoModel<>(model.geo));}
        @Override public void preRender(PoseStack pose,Look look,BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            super.preRender(pose,look,model,buffers,vertices,reRender,partial,light,overlay,color);
            if(indexed!=model){indexed=model;bones.clear();for(var bone:model.topLevelBones())index(bone);}
            for(var v:bones)v.bone().setHidden(v.section()>=0?(look.sections&(1<<v.section()))==0
                    :v.controller()?look.hideController:v.fire()!=0&&(v.fire()>0)!=look.lit);
        }
        private void index(GeoBone bone){
            String name=bone.getName();
            int section=name.startsWith("section_")?Integer.parseInt(name.substring(8,name.indexOf('_',8))):-1;
            bones.add(new Visibility(bone,section,name.equals("controller"),name.startsWith("lit_")?1:name.startsWith("cold_")?-1:0));
            for(var child:bone.getChildBones())index(child);
        }
    }
    private static final Map<MachineModel,Renderer> RENDERERS=new HashMap<>();
    private static Renderer renderer(MachineModel model){return RENDERERS.computeIfAbsent(model,Renderer::new);}
    private final Map<BlockEntity,Look> looks=new WeakHashMap<>();
    private Look guide;

    public ModeledMachineRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),ModeledMachineRenderer::new);}
    @Override public AABB getRenderBoundingBox(BlockEntity entity){var c=ModeledMachine.of(entity);return c==null?new AABB(entity.getBlockPos()):c.model().area(entity.getBlockPos());}
    // A tall model spans sections beyond its controller's; it must not vanish when that section leaves view.
    @Override public boolean shouldRenderOffScreen(BlockEntity entity){return ModeledMachine.of(entity)!=null;}
    @Override public int getViewDistance(){return 128;}
    @Override public boolean shouldRender(BlockEntity entity,net.minecraft.world.phys.Vec3 camera){
        if(ModeledMachine.of(entity)==null||!BlockEntityRenderer.super.shouldRender(entity,camera))return false;
        var frustum=net.minecraft.client.Minecraft.getInstance().levelRenderer.getFrustum();
        return frustum==null||frustum.isVisible(getRenderBoundingBox(entity));
    }
    @Override public void render(BlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var c=ModeledMachine.of(entity);if(c==null||entity.getLevel()==null)return;
        var look=looks.computeIfAbsent(entity,e->new Look(e.getType(),e.getBlockPos(),e.getBlockState(),c.model()));
        look.setLevel(entity.getLevel());look.setBlockState(entity.getBlockState());
        look.sections=c.build().sections;look.hideController=false;look.running=c.working();look.lit=c.working();
        renderer(c.model()).render(look,partial,pose,buffers,look.light(light),overlay);
    }
    /** The sections of `model` not in `built`, for a controller at `at`; a controller not yet placed is drawn too. */
    public void renderGuide(Level level,BlockPos at,BlockState controller,MachineModel model,int built,boolean placed,float partial,PoseStack pose,MultiBufferSource buffers){
        if(guide==null||guide.model!=model||!guide.getBlockPos().equals(at))
            guide=new Look(((EntityBlock)controller.getBlock()).newBlockEntity(at,controller).getType(),at,controller,model);
        guide.setLevel(level);guide.setBlockState(controller);guide.sections=model.all^built;guide.hideController=placed;guide.running=false;guide.lit=false;
        renderer(model).render(guide,partial,pose,buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    /** The solid controller is no fair light sample for a model reaching beyond it: take the brightest real light just
     *  outside the model on each side and above it, so a dark enclosure stays dark and night remains night. */
    public static int light(Level level,BlockPos controller,MachineModel model,int controllerLight){
        int sky=LightTexture.sky(controllerLight),block=LightTexture.block(controllerLight);
        for(var side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP}){
            var at=controller.relative(side,side==Direction.UP?model.top:model.reach+1);
            if(!level.hasChunkAt(at))continue;
            int nearby=LevelRenderer.getLightColor(level,at);
            sky=Math.max(sky,LightTexture.sky(nearby));block=Math.max(block,LightTexture.block(nearby));
        }
        return LightTexture.pack(block,sky);
    }
    /** The animatable stand-in for one controller, or for the guide. */
    private static final class Look extends BlockEntity implements GeoBlockEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        private final MachineModel model;
        private boolean running,lit,hideController;private int sections;
        private long lightTick=Long.MIN_VALUE;private int sampledLight;
        Look(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state,MachineModel model){super(type,pos,state);this.model=model;}
        private int light(int controllerLight){
            long tick=level.getGameTime()/5;if(tick!=lightTick){lightTick=tick;sampledLight=ModeledMachineRenderer.light(level,worldPosition,model,controllerLight);}
            return sampledLight;
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
            if(model.animation==null)return;
            var cycle=RawAnimation.begin().thenLoop(model.animation);
            controllers.add(new AnimationController<Look>(this,"work",0,state->{state.setControllerSpeed(running?1:0);return state.setAndContinue(cycle);}));
        }
    }
}

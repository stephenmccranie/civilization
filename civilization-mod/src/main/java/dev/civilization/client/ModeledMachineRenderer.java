package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
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
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Draws the moving parts of any machine built as a native model, the bones its work animation moves, while the still
 * rest is drawn with the terrain by {@link MachineBlockModel}; and draws the whole guide of missing sections. Bones carry
 * the meaning in their names: `section_<n>_…` belongs to section n, `controller` is the controller, `lit_…` and `cold_…`
 * follow the fire.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ModeledMachineRenderer implements BlockEntityRenderer<BlockEntity> {
    /** A bone's section, whether it is the controller or a fire bone, and whether it moves or holds moving bones. */
    private record Visibility(GeoBone bone,int section,boolean controller,int fire,boolean moves,boolean holdsMoving) {}
    private static final class Renderer extends GeoBlockRenderer<Look> {
        private BakedGeoModel indexed;
        private final List<Visibility> bones=new ArrayList<>();
        private final Map<GeoCube,Vector3f> centres=new IdentityHashMap<>();
        private final Vector3f at=new Vector3f();
        private Look drawing;
        Renderer(MachineModel model){super(new MachineGeoModel<>(model.geo));}
        @Override public void preRender(PoseStack pose,Look look,BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            super.preRender(pose,look,model,buffers,vertices,reRender,partial,light,overlay,color);
            drawing=look;
            if(indexed!=model){indexed=model;bones.clear();var moving=MachineMesh.of(look.model).animated;for(var bone:model.topLevelBones())index(bone,moving);}
            for(var v:bones){
                boolean hidden=v.section()>=0?(look.sections&(1<<v.section()))==0:v.controller()?look.hideController:v.fire()!=0&&(v.fire()>0)!=look.lit;
                // In the world only moving bones are drawn here; a still bone holding moving ones hides just its own cubes.
                v.bone().setHidden(hidden||!look.guide&&!v.moves());
                if(!hidden&&!look.guide&&v.holdsMoving())v.bone().setChildrenHidden(false);
            }
        }
        // The fire's own bones glow whatever light falls on the masonry around them.
        @Override public void renderRecursively(PoseStack pose,Look look,GeoBone bone,net.minecraft.client.renderer.RenderType type,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            super.renderRecursively(pose,look,bone,type,buffers,vertices,reRender,partial,bone.getName().startsWith("lit_")?LightTexture.FULL_BRIGHT:light,overlay,color);
        }
        /** A model reaching over many blocks is lit like those blocks: each cube takes the light of the cell it is drawn
         *  in, found from its centre through the render transform, so one side of a machine can stand in a neighbour's
         *  firelight while the other stays in the dark. A cube already drawn at full brightness keeps it. */
        @Override public void renderCube(PoseStack pose,GeoCube cube,com.mojang.blaze3d.vertex.VertexConsumer vertices,int light,int overlay,int color){
            if(light!=LightTexture.FULL_BRIGHT){
                pose.last().pose().transformPosition(centres.computeIfAbsent(cube,Renderer::centre),at);
                var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                light=drawing.light(BlockPos.asLong(net.minecraft.util.Mth.floor(camera.x+at.x),net.minecraft.util.Mth.floor(camera.y+at.y),net.minecraft.util.Mth.floor(camera.z+at.z)));
            }
            super.renderCube(pose,cube,vertices,light,overlay,color);
        }
        private static Vector3f centre(GeoCube cube){
            var sum=new Vector3f();int corners=0;
            for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()){sum.add(vertex.position());corners++;}
            return sum.div(Math.max(1,corners));
        }
        /** Indexes a bone and those below it; returns whether any of them moves. */
        private boolean index(GeoBone bone,Set<String> moving){
            String name=bone.getName();
            int section=name.startsWith("section_")?Integer.parseInt(name.substring(8,name.indexOf('_',8))):-1;
            int at=bones.size();bones.add(null);boolean below=false;
            for(var child:bone.getChildBones())below|=index(child,moving);
            boolean moves=moving.contains(name);
            bones.set(at,new Visibility(bone,section,name.equals("controller"),name.startsWith("lit_")?1:name.startsWith("cold_")?-1:0,moves,below));
            return moves||below;
        }
    }
    private static final Map<MachineModel,Renderer> RENDERERS=new HashMap<>();
    private static Renderer renderer(MachineModel model){return RENDERERS.computeIfAbsent(model,Renderer::new);}
    private final Map<BlockEntity,Look> looks=new WeakHashMap<>();
    private Look guide;

    public ModeledMachineRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.FOUNDRY_ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.RETORT_ENTITY.get(),ModeledMachineRenderer::new);}
    @Override public AABB getRenderBoundingBox(BlockEntity entity){var c=ModeledMachine.of(entity);return c==null?new AABB(entity.getBlockPos()):c.model().area(entity.getBlockPos());}
    // A tall model spans sections beyond its controller's; it must not vanish when that section leaves view.
    @Override public boolean shouldRenderOffScreen(BlockEntity entity){return ModeledMachine.of(entity)!=null;}
    @Override public int getViewDistance(){return 128;}
    @Override public boolean shouldRender(BlockEntity entity,net.minecraft.world.phys.Vec3 camera){
        var c=ModeledMachine.of(entity);
        if(c==null||MachineMesh.of(c.model()).animated.isEmpty()||!BlockEntityRenderer.super.shouldRender(entity,camera))return false;
        var frustum=net.minecraft.client.Minecraft.getInstance().levelRenderer.getFrustum();
        return frustum==null||frustum.isVisible(getRenderBoundingBox(entity));
    }
    @Override public void render(BlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var c=ModeledMachine.of(entity);if(c==null||entity.getLevel()==null)return;
        var look=looks.computeIfAbsent(entity,e->new Look(e.getType(),e.getBlockPos(),e.getBlockState(),c.model()));
        look.setLevel(entity.getLevel());look.setBlockState(entity.getBlockState());
        look.sections=c.build().sections;look.hideController=false;look.running=c.working();look.lit=c.working();look.guide=false;
        renderer(c.model()).render(look,partial,pose,buffers,light,overlay);
    }
    /** The sections of `model` not in `built`, for a controller at `at`; a controller not yet placed is drawn too. */
    public void renderGuide(Level level,BlockPos at,BlockState controller,MachineModel model,int built,boolean placed,float partial,PoseStack pose,MultiBufferSource buffers){
        if(guide==null||guide.model!=model||!guide.getBlockPos().equals(at))
            guide=new Look(((EntityBlock)controller.getBlock()).newBlockEntity(at,controller).getType(),at,controller,model);
        guide.setLevel(level);guide.setBlockState(controller);guide.sections=model.all^built;guide.hideController=placed;guide.running=false;guide.lit=false;guide.guide=true;
        renderer(model).render(guide,partial,pose,buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    /** The light a model's cube takes from the cell it is drawn in: the cell's own light or, where a solid block such as
     *  the controller fills it, the brightest light on any of its faces, as Minecraft lights a block's faces. */
    public static int cellLight(Level level,BlockPos cell){
        int light=LevelRenderer.getLightColor(level,cell);
        if(!level.getBlockState(cell).isSolidRender(level,cell))return light;
        int sky=LightTexture.sky(light),block=LightTexture.block(light);
        for(var side:Direction.values()){int face=LevelRenderer.getLightColor(level,cell.relative(side));sky=Math.max(sky,LightTexture.sky(face));block=Math.max(block,LightTexture.block(face));}
        return LightTexture.pack(block,sky);
    }
    /** The animatable stand-in for one controller, or for the guide. */
    private static final class Look extends BlockEntity implements GeoBlockEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        private final MachineModel model;
        private boolean running,lit,hideController,guide;private int sections;
        private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap lights=new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();private long lightTick=Long.MIN_VALUE;
        Look(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state,MachineModel model){super(type,pos,state);this.model=model;}
        /** Cell light, sampled afresh every quarter second. */
        private int light(long cell){
            long tick=level.getGameTime()/5;if(tick!=lightTick){lightTick=tick;lights.clear();}
            return lights.computeIfAbsent(cell,key->cellLight(level,BlockPos.of(key)));
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
            if(model.animation==null)return;
            var cycle=RawAnimation.begin().thenLoop(model.animation);
            controllers.add(new AnimationController<Look>(this,"work",0,state->{state.setControllerSpeed(running?1:0);return state.setAndContinue(cycle);}));
        }
    }
}

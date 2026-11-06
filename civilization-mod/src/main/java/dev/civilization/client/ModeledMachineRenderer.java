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
import net.minecraft.util.Mth;
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
 * follow its lit state: a fire, which glows, or a Tannery's steeping batch. The renderer drives two kinds of bone itself:
 * `float_<n>` is the n-th piece afloat on the model's `…water`, shown while the machine has that many afloat, and
 * `sway_…` hangs in the wind. Each bone below either bends about its own pivot's x axis, a little behind the one above
 * it, so a hide undulates on the ripples or flutters on its rail.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ModeledMachineRenderer implements BlockEntityRenderer<BlockEntity> {
    /** A bone's section, whether it is the controller or a fire bone, the floating piece it is, and whether it moves or
     *  holds moving bones. */
    private record Visibility(GeoBone bone,int section,boolean controller,int fire,int piece,boolean moves,boolean holdsMoving) {}
    /** How the renderer drives a bone: the floating piece it belongs to or whether it sways, how many bones down from
     *  that chain's root it hangs, and its pivot's place along the model's z, in blocks, which sets its ripple's phase. */
    private record Motion(int piece,boolean sway,int link,float along) {}
    private static final class Renderer extends GeoBlockRenderer<Look> {
        private BakedGeoModel indexed;
        private final List<Visibility> bones=new ArrayList<>();
        private final Map<GeoBone,Motion> motions=new IdentityHashMap<>();
        private final List<Drift.Piece> pieces=new ArrayList<>();
        private Drift.Pool pool;
        private final Map<GeoCube,Vector3f> centres=new IdentityHashMap<>();
        private final Vector3f at=new Vector3f();
        private Look drawing;
        Renderer(MachineModel model){super(new MachineGeoModel<>(model.geo));}
        @Override public void preRender(PoseStack pose,Look look,BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            super.preRender(pose,look,model,buffers,vertices,reRender,partial,light,overlay,color);
            drawing=look;
            if(indexed!=model){
                indexed=model;bones.clear();motions.clear();pieces.clear();pool=null;var moving=MachineMesh.of(look.model).animated;
                for(var bone:model.topLevelBones())index(bone,moving,null);
                if(pieces.contains(null)||!pieces.isEmpty()&&pool==null)throw new IllegalStateException(look.model.id+": float bones must be numbered from 0 and float on water");
            }
            look.time=(look.getLevel().getGameTime()+partial)/20.0;
            if(pool!=null)look.drift.step(pool,pieces,Math.min(look.afloat,pieces.size()),look.time,look.getBlockPos().asLong());
            wind(look);
            for(var v:bones){
                boolean hidden=v.section()>=0?(look.sections&(1<<v.section()))==0:v.controller()?look.hideController:v.fire()!=0&&(v.fire()>0)!=look.lit||v.piece()>=look.drift.count();
                // In the world only moving bones are drawn here; a still bone holding moving ones hides just its own cubes.
                v.bone().setHidden(hidden||!look.guide&&!v.moves());
                if(!hidden&&!look.guide&&v.holdsMoving())v.bone().setChildrenHidden(false);
            }
        }
        // A fire's own bones glow whatever light falls on the masonry around them.
        @Override public void renderRecursively(PoseStack pose,Look look,GeoBone bone,net.minecraft.client.renderer.RenderType type,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
            var motion=motions.get(bone);if(motion!=null)move(bone,motion,look);
            super.renderRecursively(pose,look,bone,type,buffers,vertices,reRender,partial,look.model.heated&&bone.getName().startsWith("lit_")?LightTexture.FULL_BRIGHT:light,overlay,color);
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
        /** Places a driven bone: a floating piece where the drift has carried it, tipping on the ripples; a bone below a
         *  piece bent by the ripple passing along it; a swaying bone leant by the wind and fluttering. */
        private void move(GeoBone bone,Motion motion,Look look){
            float t=(float)look.time;
            if(motion.sway()){bone.setRotX((motion.link()==0?look.lean:.25f*look.lean)+look.flutter*Mth.sin(t*2.3f-1.1f*motion.link()+motion.along()*3));return;}
            int i=motion.piece();
            if(motion.link()>0){bone.setRotX(.12f*Mth.sin(t*1.3f+i*1.9f-motion.along()*4));return;}
            if(i>=look.drift.count())return;
            var rest=pieces.get(i);
            bone.setPosX(-(look.drift.x(i)-rest.x())*16);bone.setPosY((look.drift.y(pool,i,look.time)-rest.y())*16);bone.setPosZ((look.drift.z(i)-rest.z())*16);
            bone.setRotY(look.drift.yaw(i));bone.setRotX(.03f*Mth.sin(t*.8f+i));bone.setRotZ(.03f*Mth.sin(t*.6f+2*i));
        }
        /** Indexes a bone and those below it, within the chain of a driven bone `above` if any; returns whether any of
         *  them moves. */
        private boolean index(GeoBone bone,Set<String> moving,Motion above){
            String name=bone.getName();
            int section=name.startsWith("section_")?Integer.parseInt(name.substring(8,name.indexOf('_',8))):-1;
            float along=bone.getPivotZ()/16;
            var motion=above!=null?new Motion(above.piece(),above.sway(),above.link()+1,along):name.startsWith("sway_")?new Motion(-1,true,0,along)
                    :name.startsWith("float_")?new Motion(Integer.parseInt(name.substring(6)),false,0,along):null;
            if(motion!=null)motions.put(bone,motion);
            int piece=above==null&&motion!=null&&!motion.sway()?motion.piece():-1;
            if(piece>=0){
                var bounds=bounds(bone,new float[]{Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE});
                float x=bone.getPivotX()/16,z=bone.getPivotZ()/16;
                while(pieces.size()<=piece)pieces.add(null);
                pieces.set(piece,new Drift.Piece(x,bone.getPivotY()/16,z,Math.max(x-bounds[0],bounds[3]-x),Math.max(z-bounds[2],bounds[5]-z)));
            }
            if(name.endsWith("water")){var b=bounds(bone,new float[]{Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE});pool=new Drift.Pool(b[0],b[3],b[2],b[5],b[4]);}
            int at=bones.size();bones.add(null);boolean below=false;
            for(var child:bone.getChildBones())below|=index(child,moving,motion);
            boolean moves=moving.contains(name);
            bones.set(at,new Visibility(bone,section,name.equals("controller"),name.startsWith("lit_")?1:name.startsWith("cold_")?-1:0,piece,moves,below));
            return moves||below;
        }
    }
    /** The rest bounds of a bone's cubes and those below it, min x y z then max x y z, in blocks. */
    private static float[] bounds(GeoBone bone,float[] into){
        for(var cube:bone.getCubes())for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()){
            var p=vertex.position();
            into[0]=Math.min(into[0],p.x());into[1]=Math.min(into[1],p.y());into[2]=Math.min(into[2],p.z());
            into[3]=Math.max(into[3],p.x());into[4]=Math.max(into[4],p.y());into[5]=Math.max(into[5],p.z());
        }
        for(var child:bone.getChildBones())bounds(child,into);
        return into;
    }
    /** The wind on a machine: one breeze over the whole world that veers and gusts slowly, a gust reaching a machine a
     *  moment after those east of it, and blowing harder in rain and storms. It leans sway bones toward the machine's
     *  front by how squarely it blows there, and flutters them; under cover from the sky they hang still. */
    private static void wind(Look look){
        var level=look.getLevel();var pos=look.getBlockPos();var front=look.getBlockState().getValue(CivicBlock.FACING);
        double t=look.time-pos.getX()*.02,heading=1.3+.9*Math.sin(look.time/170)+.5*Math.sin(look.time/61);
        float strength=(float)(.55+.25*Math.sin(t*.37)+.2*Math.sin(t*.91+1))*(1+level.getRainLevel(1)+level.getThunderLevel(1))
                *level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos.above(2))/15f;
        look.lean=.2f*strength*(float)(Math.cos(heading)*front.getStepX()+Math.sin(heading)*front.getStepZ());look.flutter=.06f*strength;
    }
    private static final Map<MachineModel,Renderer> RENDERERS=new HashMap<>();
    private static Renderer renderer(MachineModel model){return RENDERERS.computeIfAbsent(model,Renderer::new);}
    private final Map<BlockEntity,Look> looks=new WeakHashMap<>();
    private Look guide;

    public ModeledMachineRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.FOUNDRY_ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(KilnContent.RETORT_ENTITY.get(),ModeledMachineRenderer::new);e.registerBlockEntityRenderer(WorkshopContent.ENTITY.get(),ModeledMachineRenderer::new);}
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
        look.sections=c.build().sections;look.hideController=false;look.running=c.working();look.lit=c.working();look.guide=false;look.afloat=c.afloat();
        renderer(c.model()).render(look,partial,pose,buffers,light,overlay);
    }
    /** The sections of `model` not in `built`, for a controller at `at`; a controller not yet placed is drawn too. */
    public void renderGuide(Level level,BlockPos at,BlockState controller,MachineModel model,int built,boolean placed,float partial,PoseStack pose,MultiBufferSource buffers){
        if(guide==null||guide.model!=model||!guide.getBlockPos().equals(at))
            guide=new Look(((EntityBlock)controller.getBlock()).newBlockEntity(at,controller).getType(),at,controller,model);
        guide.setLevel(level);guide.setBlockState(controller);guide.sections=model.all^built;guide.hideController=placed;guide.running=false;guide.lit=false;guide.guide=true;guide.afloat=0;
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
        private boolean running,lit,hideController,guide;private int sections,afloat;
        private final Drift drift=new Drift();private double time;private float lean,flutter;
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

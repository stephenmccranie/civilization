package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import java.util.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A pallet's stack, laid out by its {@link CargoMesh} from the top of the 2-pixel timber base, up to eight layers into
 * the block above. The stack's block quads belong to the pallet's block model, so they are drawn with the terrain and
 * rebuilt only when the pallet's goods or grid change; this renderer draws only the goods that have no block quads.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class StockpileRenderer implements BlockEntityRenderer<StockpileEntity> {
    private static final float BASE=2f/16;
    public StockpileRenderer(BlockEntityRendererProvider.Context c){}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(StockpileEntity e){return new net.minecraft.world.phys.AABB(e.getBlockPos()).expandTowards(0,1,0);}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CargoContent.ENTITY.get(),StockpileRenderer::new);}
    @Override public boolean shouldRender(StockpileEntity e,Vec3 camera){return !mesh(e).drawn.isEmpty()&&BlockEntityRenderer.super.shouldRender(e,camera);}
    @Override public void render(StockpileEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(0,BASE,0);mesh(e).renderDrawn(pose,buffers,light,overlay);pose.popPose();
    }
    private static CargoMesh mesh(StockpileEntity e){return CargoMesh.of(e.stack().layout(e.grid()));}

    @SubscribeEvent static void bake(ModelEvent.ModifyBakingResult event){
        for(var state:CargoContent.STOCKPILE.get().getStateDefinition().getPossibleStates())
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),(key,original)->new Model(original));
    }

    /** The pallet's own model with its stack's block quads, from the layout its block entity gives the chunk mesher. */
    private static final class Model extends BakedModelWrapper<BakedModel> {
        Model(BakedModel original){super(original);}
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource rand,ModelData data){
            var layout=data.get(StockpileEntity.CARGO);var own=super.getRenderTypes(state,rand,data);
            return layout==null?own:ChunkRenderTypeSet.union(own,ChunkRenderTypeSet.of(CargoMesh.of(layout).renderTypes()));
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource rand,ModelData data,RenderType type){
            var own=super.getQuads(state,side,rand,data,type);var layout=data.get(StockpileEntity.CARGO);
            if(side!=null||type==null||layout==null)return own;
            var cargo=CargoMesh.of(layout).quads(type);
            var all=new ArrayList<BakedQuad>(own.size()+cargo.size());all.addAll(own);
            for(var quad:cargo)all.add(CargoMesh.place(quad,0,BASE,0,1,1,-1));
            return all;
        }
    }
}

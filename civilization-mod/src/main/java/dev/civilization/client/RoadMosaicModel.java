package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.CutBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Selects one square of a world-aligned 8×8 road sprite during chunk baking. */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RoadMosaicModel extends BakedModelWrapper<BakedModel> {
    private static final int GRID = 8;
    private static final ResourceLocation MOSAIC = ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "block/street_pavers_mosaic");
    private static final Set<String> ROAD_BLOCKS = Set.of("street_pavers", "street_pavers_slab", "street_pavers_stairs");
    private final ConcurrentHashMap<BakedQuad, BakedQuad[]> cachedTopQuads = new ConcurrentHashMap<>();

    private RoadMosaicModel(BakedModel original) {
        super(original);
    }

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) ->
                id.id().getNamespace().equals(Civilization.MOD_ID) && ROAD_BLOCKS.contains(id.id().getPath())
                        ? new RoadMosaicModel(model) : model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        return ModelData.builder().with(CutBlockEntity.POSITION, pos.immutable()).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, random, data, type);
        BlockPos pos = data.has(CutBlockEntity.POSITION) ? data.get(CutBlockEntity.POSITION) : BlockPos.ZERO;
        int tileX = Math.floorMod(pos.getX(), GRID);
        int tileZ = Math.floorMod(pos.getZ(), GRID);
        int tile = tileZ * GRID + tileX;
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            if (quad.getDirection() != Direction.UP || !MOSAIC.equals(quad.getSprite().contents().name())) {
                if (result != null) result.add(quad);
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(quads.size());
                result.addAll(quads.subList(0, i));
            }
            BakedQuad[] tiles = cachedTopQuads.computeIfAbsent(quad, ignored -> new BakedQuad[GRID * GRID]);
            BakedQuad mapped = tiles[tile];
            if (mapped == null) {
                synchronized (tiles) {
                    mapped = tiles[tile];
                    if (mapped == null) tiles[tile] = mapped = slice(quad, tileX, tileZ);
                }
            }
            result.add(mapped);
        }
        return result == null ? quads : result;
    }

    private static BakedQuad slice(BakedQuad quad, int tileX, int tileZ) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        var sprite = quad.getSprite();
        float u0 = sprite.getU0(), du = sprite.getU1() - u0;
        float v0 = sprite.getV0(), dv = sprite.getV1() - v0;
        for (int vertex = 0; vertex < 4; vertex++) {
            int u = vertex * stride + 4, v = vertex * stride + 5;
            float localU = (Float.intBitsToFloat(vertices[u]) - u0) / du;
            float localV = (Float.intBitsToFloat(vertices[v]) - v0) / dv;
            vertices[u] = Float.floatToRawIntBits(u0 + (tileX + localU) * du / GRID);
            vertices[v] = Float.floatToRawIntBits(v0 + (tileZ + localV) * dv / GRID);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade());
    }
}

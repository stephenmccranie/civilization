package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Machines drawn with the terrain: the block model of every controller and part cell of a machine built as a native
 * model. Each cell gives the still geometry of its own built sections from {@link MachineMesh}, and the controller its
 * door and its lit or cold bones by its lit state, so the chunk mesher builds a machine once, as it builds the
 * blocks around it, instead of every frame. Which sections are built is read from the controller as the chunk is meshed;
 * the controller redraws its cells when that changes. Quads are cut out, so a hide's ragged outline shows against
 * what lies behind it. Water is drawn translucent in vanilla's animated still-water
 * texture, one tile to a block as on a pool, and takes its biome's water colour.
 */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class MachineBlockModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> SECTIONS = new ModelProperty<>();
    private static final ChunkRenderTypeSet CUT_OUT = ChunkRenderTypeSet.of(RenderType.cutoutMipped()), WITH_WATER = ChunkRenderTypeSet.of(RenderType.cutoutMipped(), RenderType.translucent());
    private final MachineMesh mesh;
    private final TextureAtlasSprite sprite, water;
    private final ChunkRenderTypeSet types;
    private final Map<Long, List<BakedQuad>> baked = new ConcurrentHashMap<>();
    /** A cell's cut-out or water quads for its facing and built sections; the controller's cell is -1, its `built` its lit state. */
    private record CellKey(Direction front, int cell, int built, boolean water) {}
    private final Map<CellKey, List<BakedQuad>> cells = new ConcurrentHashMap<>();

    private MachineBlockModel(BakedModel original, MachineMesh mesh, TextureAtlasSprite sprite, TextureAtlasSprite water) {
        super(original); this.mesh = mesh; this.sprite = sprite; this.water = water; types = mesh.hasWater() ? WITH_WATER : CUT_OUT;
    }

    @SubscribeEvent static void bake(ModelEvent.ModifyBakingResult event) {
        var resources = Minecraft.getInstance().getResourceManager();
        var water = event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.withDefaultNamespace("block/water_still")));
        for (var model : MachineModel.all()) {
            var sprite = event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath("civilization", "entity/" + model.geo)));
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                throw new IllegalStateException("Machine texture entity/" + model.geo + " is missing from the block atlas (minecraft:atlases/blocks.json)");
            var mesh = MachineMesh.load(model, resources);
            for (var block : BuiltInRegistries.BLOCK)
                if (block == MachineParts.block(model) || block instanceof ModeledBlock controller && controller.model() == model)
                    for (var state : block.getStateDefinition().getPossibleStates())
                        event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (key, original) -> new MachineBlockModel(original, mesh, sprite, water));
        }
    }

    /** Tint 0 is the biome's water colour, as a pool's. */
    @SubscribeEvent static void tint(RegisterColorHandlersEvent.Block event) {
        for (var block : BuiltInRegistries.BLOCK)
            if (block instanceof ModeledBlock || block instanceof MachinePartBlock)
                event.register((state, level, pos, tint) -> level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : -1, block);
    }

    @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        var at = state.getBlock() instanceof MachinePartBlock part ? part.controller(pos, state) : pos;
        var c = ModeledMachine.of(level.getBlockEntity(at));
        int sections = c != null && c.model() == mesh.model && c.front() == state.getValue(CivicBlock.FACING) ? c.build().sections : 0;
        return data.derive().with(SECTIONS, sections).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) { return getQuads(state, side, rand, ModelData.EMPTY, null); }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType type) {
        if (side != null || state == null) return List.of();
        if (type != null) return quads(state, data, type == RenderType.translucent());
        var all = new ArrayList<>(quads(state, data, false)); all.addAll(quads(state, data, true));
        return all;
    }

    private List<BakedQuad> quads(BlockState state, ModelData data, boolean water) {
        var front = state.getValue(CivicBlock.FACING);
        if (!(state.getBlock() instanceof MachinePartBlock)) {
            boolean lit = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
            return cells.computeIfAbsent(new CellKey(front, MachineMesh.CONTROLLER_CELL, lit ? 1 : 0, water), k -> {
                var all = new ArrayList<>(group(front, MachineMesh.CONTROLLER_CELL, MachineMesh.CONTROLLER, water));
                all.addAll(group(front, MachineMesh.CONTROLLER_CELL, lit ? MachineMesh.LIT : MachineMesh.COLD, water));
                return List.copyOf(all);
            });
        }
        int cell = state.getValue(mesh.model.cell);
        var sections = data.get(SECTIONS);
        int built = (sections == null ? 0 : sections) & mesh.model.cells.get(cell).sectionMask();
        return cells.computeIfAbsent(new CellKey(front, cell, built, water), k -> {
            var all = new ArrayList<BakedQuad>();
            for (int part = 0; part < mesh.model.sections.size(); part++) if ((built & (1 << part)) != 0) all.addAll(group(front, cell, part, water));
            return List.copyOf(all);
        });
    }

    /** One group's cut-out or water quads in the block atlas, baked on first use. */
    private List<BakedQuad> group(Direction front, int cell, int group, boolean water) {
        return baked.computeIfAbsent(MachineMesh.key(front, cell, group) << 1 | (water ? 1 : 0),
                k -> mesh.quads(front, cell, group).stream().filter(q -> q.water() == water).map(this::bake).toList());
    }

    private BakedQuad bake(MachineMesh.Quad quad) {
        var builder = new QuadBakingVertexConsumer();
        var n = quad.normal(); var corners = quad.corners();
        var texture = quad.water() ? water : sprite;
        builder.setSprite(texture); builder.setDirection(Direction.getNearest(n.x(), n.y(), n.z())); builder.setShade(!quad.glow()); builder.setHasAmbientOcclusion(false);
        builder.setTintIndex(quad.water() ? 0 : -1);
        // A water surface takes one tile of its block, so it must lie flat within one block's column.
        float x0 = (float) Math.floor(Math.min(Math.min(corners[0].x(), corners[1].x()), Math.min(corners[2].x(), corners[3].x())) + 1e-4f);
        float z0 = (float) Math.floor(Math.min(Math.min(corners[0].z(), corners[1].z()), Math.min(corners[2].z(), corners[3].z())) + 1e-4f);
        int light = quad.glow() ? 0xF0 : 0;
        for (int i = 0; i < 4; i++) {
            var p = corners[i];
            float u = quad.water() ? p.x() - x0 : quad.uv()[2 * i], v = quad.water() ? p.z() - z0 : quad.uv()[2 * i + 1];
            if (quad.water() && (n.y() < .999f || u > 1.001f || v > 1.001f))
                throw new IllegalStateException(mesh.model.id + ": water must be upward faces inside one block column each");
            builder.addVertex(p.x(), p.y(), p.z()).setColor(255, 255, 255, 255).setUv(texture.getU(u), texture.getV(v))
                    .setUv2(light, light).setNormal(n.x(), n.y(), n.z());
        }
        return builder.bakeQuad();
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) { return types; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType type) { return TriState.FALSE; }
}

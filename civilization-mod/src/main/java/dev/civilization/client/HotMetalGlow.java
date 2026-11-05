package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.Civilization;
import dev.civilization.HotMetal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Hot metal glows in its temperature's colour wherever it is drawn: in slots, in hand, dropped and in frames. Each
 * pass of its model is drawn tinted by the glow and lit from within by its strength, then again as light added over
 * it, so it shows at night. The glow's colour is the items' tint each frame; its strength lights the quads in four
 * steps, kept per model.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HotMetalGlow {
    /** Tint indices of the two passes, clear of the small indices models use for their own tints. */
    private static final int TINT = 0x484F54, LIGHT = TINT + 1;
    /** Each glowing model pass's quads, by side, tint and light. */
    private record Quads(BakedModel model, Direction side, int tint, int light) {}
    private static final Map<Quads, List<BakedQuad>> QUADS = new ConcurrentHashMap<>();
    private HotMetalGlow() {}

    private static List<Item> metals() { return BuiltInRegistries.ITEM.stream().filter(HotMetal::forgeable).toList(); }
    private static int glow(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : HotMetal.glow(stack, level.getGameTime());
    }

    /** The tint fades from white to the glow's colour, and the added light from black to it, with its strength. */
    @SubscribeEvent public static void colours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint != TINT && tint != LIGHT) return -1;
            int glow = glow(stack), colour = 0xFF000000;
            float strength = (glow >>> 24) / 255f;
            for (int shift = 0; shift < 24; shift += 8) {
                int channel = glow >>> shift & 0xFF;
                colour |= Math.round(tint == TINT ? 255 + (channel - 255) * strength : channel * strength) << shift;
            }
            return colour;
        }, metals().toArray(Item[]::new));
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        QUADS.clear();
        for (var item : metals())
            event.getModels().computeIfPresent(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)), (location, model) -> new Metal(model));
    }

    /** A metal's model, which draws its stacks glowing while they glow. */
    private static final class Metal extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                var resolved = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                int strength = glow(stack) >>> 24;
                return resolved == null || strength == 0 ? resolved : new Hot(resolved, (strength * 4 + 254) / 255 * 15 / 4);
            }
        };
        Metal(BakedModel model) { super(model); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }
    /** A glowing stack's model, lit from within at block light `light`: each of its passes, tinted, then its light. */
    private static final class Hot extends BakedModelWrapper<BakedModel> {
        private final int light;
        Hot(BakedModel model, int light) { super(model); this.light = light; }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            return new Hot(originalModel.applyTransform(context, pose, leftHand), light);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return originalModel.getRenderPasses(stack, fabulous).stream()
                    .<BakedModel>mapMulti((pass, passes) -> { passes.accept(new Pass(pass, TINT, light)); passes.accept(new Pass(pass, LIGHT, light)); }).toList();
        }
    }
    /** One pass with every quad tinted `tint` and lit at least to `light`; the light pass is added over what is drawn. */
    private static final class Pass extends BakedModelWrapper<BakedModel> {
        private final int tint, light;
        Pass(BakedModel model, int tint, int light) { super(model); this.tint = tint; this.light = light; }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return QUADS.computeIfAbsent(new Quads(originalModel, side, tint, light), key -> {
                var lit = QuadTransformers.applyingLightmap(light, 0);
                return originalModel.getQuads(state, side, random).stream().map(lit::process)
                        .map(quad -> new BakedQuad(quad.getVertices(), tint, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion())).toList();
            });
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return tint == TINT ? originalModel.getRenderTypes(stack, fabulous) : List.of(RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}

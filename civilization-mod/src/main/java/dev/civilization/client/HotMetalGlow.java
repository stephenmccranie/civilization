package dev.civilization.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.Civilization;
import dev.civilization.HotMetal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Hot metal glows in its temperature's colour wherever it is drawn: in slots, in hand, dropped and in frames. Each
 * pass of its model is drawn from its sprite lifted toward white by its heat, tinted by the glow and lit from within
 * by its strength, then again as light added over it, so it shows at night. The heat is in the texture, so white-hot
 * metal outshines its cold texture whatever draws it, shaders included. The glow's colour is the items' tint each
 * frame; its strength lights the quads in four steps and its heat lifts them in eight, kept per model.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HotMetalGlow {
    /** Tint indices of the two passes, clear of the small indices models use for their own tints. */
    private static final int TINT = 0x484F54, LIGHT = TINT + 1;
    /**
     * Heat lifts the metal toward white in steps, as far as the glow's least channel rises above `HEAT_FROM`: orange
     * not at all, pure white all the way.
     */
    private static final int HEAT_STEPS = 8, HEAT_FROM = 0x40;
    /**
     * Every metal sprite at every heat step: one column per sprite, its steps stacked from cold down. Its metal pass has
     * a buffer of its own, drawn with the other item sheets, so its added light falls under it as before.
     */
    private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "hot_metal");
    private static final RenderType METAL = RenderType.entityTranslucentCull(SHEET), GLOW = RenderType.eyes(SHEET);
    /** Each sprite's column on the sheet, as fractions of its size: its left and right edges and each step's height. */
    private record Column(float u0, float u1, float height) {}
    private static Map<TextureAtlasSprite, Column> columns = Map.of();
    /** Each glowing model pass's quads, by side, tint, light and heat. */
    private record Quads(BakedModel model, Direction side, int tint, int light, int heat) {}
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
    @SubscribeEvent public static void buffers(RegisterRenderBuffersEvent event) { event.registerRenderBuffer(METAL); }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        QUADS.clear();
        for (var item : metals())
            event.getModels().computeIfPresent(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)), (location, model) -> new Metal(model));
    }
    /** Lays every metal sprite out on the sheet at each heat step, its metal lifted toward white and its empty space kept. */
    @SubscribeEvent public static void sheet(ModelEvent.BakingCompleted event) {
        var sprites = metals().stream().map(item -> event.getModels().get(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item))).getParticleIcon()).distinct().toList();
        int width = sprites.stream().mapToInt(sprite -> sprite.contents().width()).sum();
        int height = sprites.stream().mapToInt(sprite -> sprite.contents().height()).max().orElseThrow();
        var image = new NativeImage(width, height * HEAT_STEPS, true);
        var laid = new HashMap<TextureAtlasSprite, Column>();
        int x0 = 0;
        for (var sprite : sprites) {
            var source = sprite.contents().getOriginalImage();
            int w = sprite.contents().width(), h = sprite.contents().height();
            for (int step = 0; step < HEAT_STEPS; step++) {
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int pixel = source.getPixelRGBA(x, y), lifted = pixel & 0xFF000000;
                    float lift = (pixel >>> 24) / 255f * step / (HEAT_STEPS - 1);
                    for (int shift = 0; shift < 24; shift += 8) {
                        int channel = pixel >>> shift & 0xFF;
                        lifted |= Math.round(channel + (255 - channel) * lift) << shift;
                    }
                    image.setPixelRGBA(x0 + x, step * height + y, lifted);
                }
            }
            laid.put(sprite, new Column(x0 / (float)width, (x0 + w) / (float)width, h / (float)(height * HEAT_STEPS)));
            x0 += w;
        }
        Minecraft.getInstance().getTextureManager().register(SHEET, new DynamicTexture(image));
        columns = laid;
    }

    /** The heat step at the glow. */
    private static int heat(int glow) {
        int least = Math.min(glow & 0xFF, Math.min(glow >>> 8 & 0xFF, glow >>> 16 & 0xFF));
        return Math.round(Math.clamp((least - HEAT_FROM) / (float)(255 - HEAT_FROM), 0, 1) * (HEAT_STEPS - 1));
    }
    /** The quad drawn from its sprite's column on the sheet, at heat step `heat`. */
    private static BakedQuad onSheet(BakedQuad quad, int tint, int heat) {
        var sprite = quad.getSprite();
        var column = Objects.requireNonNull(columns.get(sprite), () -> "Hot metal sprite missing from the sheet: " + sprite.contents().name());
        int[] vertices = quad.getVertices().clone();
        for (int at = IQuadTransformer.UV0; at < vertices.length; at += IQuadTransformer.STRIDE) {
            float u = (Float.intBitsToFloat(vertices[at]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
            float v = (Float.intBitsToFloat(vertices[at + 1]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
            vertices[at] = Float.floatToRawIntBits(column.u0() + u * (column.u1() - column.u0()));
            vertices[at + 1] = Float.floatToRawIntBits(heat / (float)HEAT_STEPS + v * column.height());
        }
        return new BakedQuad(vertices, tint, quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** A metal's model, which draws its stacks glowing while they glow. */
    private static final class Metal extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                var resolved = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                int glow = glow(stack), strength = glow >>> 24;
                return resolved == null || strength == 0 ? resolved : new Hot(resolved, (strength * 4 + 254) / 255 * 15 / 4, heat(glow));
            }
        };
        Metal(BakedModel model) { super(model); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }
    /** A glowing stack's model, lit from within at block light `light` and lifted to heat step `heat`: each of its passes, tinted, then its light. */
    private static final class Hot extends BakedModelWrapper<BakedModel> {
        private final int light, heat;
        Hot(BakedModel model, int light, int heat) { super(model); this.light = light; this.heat = heat; }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            return new Hot(originalModel.applyTransform(context, pose, leftHand), light, heat);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return originalModel.getRenderPasses(stack, fabulous).stream()
                    .<BakedModel>mapMulti((pass, passes) -> { passes.accept(new Pass(pass, TINT, light, heat)); passes.accept(new Pass(pass, LIGHT, light, heat)); }).toList();
        }
    }
    /** One pass drawn from the sheet with every quad tinted `tint` and lit at least to `light`; the light pass is added over what is drawn. */
    private static final class Pass extends BakedModelWrapper<BakedModel> {
        private final int tint, light, heat;
        Pass(BakedModel model, int tint, int light, int heat) { super(model); this.tint = tint; this.light = light; this.heat = heat; }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return QUADS.computeIfAbsent(new Quads(originalModel, side, tint, light, heat), key -> {
                var lit = QuadTransformers.applyingLightmap(light, 0);
                return originalModel.getQuads(state, side, random).stream().map(lit::process).map(quad -> onSheet(quad, tint, heat)).toList();
            });
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) { return List.of(tint == TINT ? METAL : GLOW); }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}

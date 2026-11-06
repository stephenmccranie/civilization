package dev.civilization.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.Civilization;
import dev.civilization.HotMetal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.loader.PBRTextureLoaderRegistry;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
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
import net.neoforged.fml.ModList;
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
 * frame; its strength lights the quads in four steps and its heat lifts them in eight, kept per model. Outside a slot
 * a halo of its light surrounds its outline.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HotMetalGlow {
    /** Tint indices of the metal, its light and its halo, clear of the small indices models use for their own tints. */
    private static final int TINT = 0x484F54, LIGHT = TINT + 1, HALO_LIGHT = TINT + 2;
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
    private static final RenderType METAL = RenderType.entityTranslucentCull(SHEET);
    /** Each sprite's column on the sheet, as fractions of its size: its left and right edges and each step's height. */
    private record Column(float u0, float u1, float height) {}
    private static Map<TextureAtlasSprite, Column> columns = Map.of();
    /** Each glowing model pass's quads, by side, tint, light and heat. */
    private record Quads(BakedModel model, Direction side, int tint, int light, int heat) {}
    private static final Map<Quads, List<BakedQuad>> QUADS = new ConcurrentHashMap<>();
    /**
     * Every metal sprite's halo: white light blurred out from its solid texels, `HALO_REACH` of its width past them, in a
     * cell that much wider on every side. Its texels are at least a tenth opaque wherever they shine, the least a shader
     * pack blends, and more where they shine brighter, where the pack draws more of their light.
     */
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "hot_metal_halo");
    private static final float HALO_REACH = 0.75f;
    /** Each sprite's cell on the halo sheet, as fractions of its size: its left and right edges and its height. */
    private record Cell(float u0, float u1, float height) {}
    private static Map<TextureAtlasSprite, Cell> cells = Map.of();
    /** Each glowing model pass's halo quads. */
    private static final Map<BakedModel, List<BakedQuad>> HALOS = new ConcurrentHashMap<>();
    private static final boolean IRIS = ModList.get().isLoaded("iris");
    private HotMetalGlow() {}

    /** Whether a shader pack is drawing what is being drawn now. */
    private static boolean shaders() { return IRIS && Shaders.drawing(); }
    private static List<Item> metals() { return BuiltInRegistries.ITEM.stream().filter(HotMetal::forgeable).toList(); }
    private static int glow(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : HotMetal.glow(stack, level.getGameTime());
    }

    /**
     * The tint fades from white to the glow's colour, and the added light from black to it, with its strength. The halo's
     * light is also only as opaque as it is strong, so a shader pack leaves out a faint halo, too faint for it to blend,
     * rather than shade what lies behind it.
     */
    @SubscribeEvent public static void colours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint != TINT && tint != LIGHT && tint != HALO_LIGHT) return -1;
            int glow = glow(stack), colour = tint == HALO_LIGHT ? glow & 0xFF000000 : 0xFF000000;
            float strength = (glow >>> 24) / 255f;
            for (int shift = 0; shift < 24; shift += 8) {
                int channel = glow >>> shift & 0xFF;
                colour |= Math.round(tint == TINT ? 255 + (channel - 255) * strength : channel * strength) << shift;
            }
            return colour;
        }, metals().toArray(Item[]::new));
    }
    @SubscribeEvent public static void buffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(METAL);
        event.registerRenderBuffer(Glow.HALO);
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        QUADS.clear();
        HALOS.clear();
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
        if (IRIS) Shaders.emit();
        var textures = Minecraft.getInstance().getTextureManager();
        textures.register(SHEET, new Sheet(image));
        textures.register(HALO, new Sheet(halos(sprites)));
        columns = laid;
    }
    /** Lays every metal sprite's halo out on the halo sheet in a row. */
    private static NativeImage halos(List<TextureAtlasSprite> sprites) {
        int width = sprites.stream().mapToInt(sprite -> sprite.contents().width() + 2 * reach(sprite)).sum();
        int height = sprites.stream().mapToInt(sprite -> sprite.contents().height() + 2 * reach(sprite)).max().orElseThrow();
        var image = new NativeImage(width, height, true);
        var laid = new HashMap<TextureAtlasSprite, Cell>();
        int x0 = 0;
        for (var sprite : sprites) {
            var source = sprite.contents().getOriginalImage();
            int reach = reach(sprite), w = sprite.contents().width() + 2 * reach, h = sprite.contents().height() + 2 * reach;
            float[] solid = new float[w * h];
            for (int y = reach; y < h - reach; y++) for (int x = reach; x < w - reach; x++)
                solid[y * w + x] = (source.getPixelRGBA(x - reach, y - reach) >>> 24) / 255f;
            float[] light = blur(blur(solid, w, h, 1, reach / 3f), w, h, w, reach / 3f);
            // An edge keeps half its texels' light after blurring; it shines in full, and the light beyond fades slowly.
            for (int at = 0; at < light.length; at++) {
                float shine = (float)Math.pow(Math.min(1, 2 * light[at]), 0.6);
                int white = Math.round(255 * shine), alpha = white == 0 ? 0 : Math.round(26 + 128 * shine);
                image.setPixelRGBA(x0 + at % w, at / w, alpha << 24 | white << 16 | white << 8 | white);
            }
            laid.put(sprite, new Cell(x0 / (float)width, (x0 + w) / (float)width, h / (float)height));
            x0 += w;
        }
        cells = laid;
        return image;
    }
    private static int reach(TextureAtlasSprite sprite) { return Math.round(sprite.contents().width() * HALO_REACH); }
    /** `values`, `width` by `height`, blurred along one axis, whose neighbours lie `step` apart, by a Gaussian of `sigma`. */
    private static float[] blur(float[] values, int width, int height, int step, float sigma) {
        int radius = (int)Math.ceil(3 * sigma);
        float[] kernel = new float[2 * radius + 1];
        float sum = 0;
        for (int i = -radius; i <= radius; i++) sum += kernel[i + radius] = (float)Math.exp(-i * i / (2 * sigma * sigma));
        float[] blurred = new float[values.length];
        for (int at = 0; at < values.length; at++) {
            int x = at % width, y = at / width;
            for (int i = -radius; i <= radius; i++) {
                int along = (step == 1 ? x : y) + i;
                if (along >= 0 && along < (step == 1 ? width : height)) blurred[at] += values[at + i * step] * kernel[i + radius] / sum;
            }
        }
        return blurred;
    }
    /**
     * The glow: light added over what is drawn, writing depth because under a shader pack it is the item's only surface,
     * so block outlines and whatever is drawn after it stay behind it. In the world it lights only the metal; in a slot
     * Minecraft's eyes shader also lights the sprite's empty texels, which hold the metal's colour, so the slot glows.
     * The halo adds its light over whatever is behind it and hides nothing; a shader pack draws it among its translucent
     * surfaces, emitting.
     */
    private static final class Glow extends RenderType {
        static final RenderType WORLD = create("civilization_hot_metal_glow", RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER);
        static final RenderType SLOT = create("civilization_hot_metal_slot_glow", RENDERTYPE_EYES_SHADER);
        static final RenderType HALO = create("civilization_hot_metal_halo", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(HotMetalGlow.HALO, true, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private static RenderType create(String name, ShaderStateShard shader) {
            return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(shader).setTextureState(new TextureStateShard(SHEET, false, false)).setOverlayState(OVERLAY)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
        }
        private Glow() { super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false, () -> {}, () -> {}); }
    }
    /** Hot metal's textures, a class of their own so shaders can find their emission. */
    private static final class Sheet extends DynamicTexture {
        Sheet(NativeImage image) { super(image); }
    }
    /** What hot metal asks of Iris, loaded only with it. */
    private static final class Shaders {
        static boolean drawing() { return Iris.getPipelineManager().getPipelineNullable() instanceof ShaderRenderingPipeline pipeline && pipeline.shouldOverrideShaders(); }
        static boolean shadows() { return ShadowRenderingState.areShadowsCurrentlyBeingRendered(); }
        /** Gives each texture a LabPBR specular map in which every texel emits its own colour in full. */
        static void emit() {
            PBRTextureLoaderRegistry.INSTANCE.register(Sheet.class, (sheet, resources, maps) -> {
                var emissive = new NativeImage(1, 1, false);
                emissive.setPixelRGBA(0, 0, 0xFE000000);
                maps.acceptSpecularTexture(new DynamicTexture(emissive));
            });
        }
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
    /**
     * A front quad's halo: the quad grown by its sprite's reach on every side, drawn from the sprite's halo cell through
     * the middle of the item, where the metal's front and back hide what lies under them and the light shows around them.
     */
    private static BakedQuad halo(BakedQuad quad) {
        var sprite = quad.getSprite();
        var cell = Objects.requireNonNull(cells.get(sprite), () -> "Hot metal sprite missing from the halo sheet: " + sprite.contents().name());
        float wide = 1 + 2 * reach(sprite) / (float)sprite.contents().width(), tall = 1 + 2 * reach(sprite) / (float)sprite.contents().height();
        int[] vertices = quad.getVertices().clone();
        float middleX = 0, middleY = 0;
        for (int at = 0; at < vertices.length; at += IQuadTransformer.STRIDE) {
            middleX += Float.intBitsToFloat(vertices[at]) / 4;
            middleY += Float.intBitsToFloat(vertices[at + 1]) / 4;
        }
        for (int at = 0; at < vertices.length; at += IQuadTransformer.STRIDE) {
            float u = (Float.intBitsToFloat(vertices[at + IQuadTransformer.UV0]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
            float v = (Float.intBitsToFloat(vertices[at + IQuadTransformer.UV0 + 1]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
            vertices[at] = Float.floatToRawIntBits(middleX + (Float.intBitsToFloat(vertices[at]) - middleX) * wide);
            vertices[at + 1] = Float.floatToRawIntBits(middleY + (Float.intBitsToFloat(vertices[at + 1]) - middleY) * tall);
            vertices[at + 2] = Float.floatToRawIntBits(0.5f);
            vertices[at + IQuadTransformer.UV0] = Float.floatToRawIntBits(cell.u0() + u * (cell.u1() - cell.u0()));
            vertices[at + IQuadTransformer.UV0 + 1] = Float.floatToRawIntBits(v * cell.height());
        }
        return new BakedQuad(vertices, HALO_LIGHT, quad.getDirection(), sprite, false, false);
    }

    /** A metal's model, which draws its stacks glowing while they glow. */
    private static final class Metal extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                var resolved = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                int glow = glow(stack), strength = glow >>> 24;
                return resolved == null || strength == 0 ? resolved : new Hot(resolved, (strength * 4 + 254) / 255 * 15 / 4, heat(glow), Glow.WORLD, false);
            }
        };
        Metal(BakedModel model) { super(model); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }
    /**
     * A glowing stack's model, lit from within at block light `light` and lifted to heat step `heat`: each of its passes,
     * tinted, then its light, drawn as `glow`, then its halo once placed outside a slot. A shader pack draws the light
     * alone, emitting its colour through the sheet's emission map: it would show the light over the metal in the world
     * anyway, and in hand Iris would draw the metal over the light. Its shadow pass draws no halo, which only shines.
     */
    private static final class Hot extends BakedModelWrapper<BakedModel> {
        private final int light, heat;
        private final RenderType glow;
        private final boolean halo;
        Hot(BakedModel model, int light, int heat, RenderType glow, boolean halo) {
            super(model);
            this.light = light;
            this.heat = heat;
            this.glow = glow;
            this.halo = halo;
        }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            boolean slot = context == ItemDisplayContext.GUI;
            return new Hot(originalModel.applyTransform(context, pose, leftHand), light, heat, slot ? Glow.SLOT : Glow.WORLD, !slot && !(IRIS && Shaders.shadows()));
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            boolean shaders = shaders();
            return originalModel.getRenderPasses(stack, fabulous).stream().<BakedModel>mapMulti((pass, passes) -> {
                if (!shaders) passes.accept(new Pass(pass, TINT, light, heat, METAL));
                passes.accept(new Pass(pass, LIGHT, light, heat, glow));
                if (halo) passes.accept(new Halo(pass));
            }).toList();
        }
    }
    /** One pass drawn as `type` from the sheet with every quad tinted `tint` and lit at least to `light`. */
    private static final class Pass extends BakedModelWrapper<BakedModel> {
        private final int tint, light, heat;
        private final RenderType type;
        Pass(BakedModel model, int tint, int light, int heat, RenderType type) { super(model); this.tint = tint; this.light = light; this.heat = heat; this.type = type; }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return QUADS.computeIfAbsent(new Quads(originalModel, side, tint, light, heat), key -> {
                var lit = QuadTransformers.applyingLightmap(light, 0);
                return originalModel.getQuads(state, side, random).stream().map(lit::process).map(quad -> onSheet(quad, tint, heat)).toList();
            });
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) { return List.of(type); }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
    /** A pass's halo, from its quads facing front, tinted with its light. */
    private static final class Halo extends BakedModelWrapper<BakedModel> {
        Halo(BakedModel pass) { super(pass); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return side != null ? List.of() : HALOS.computeIfAbsent(originalModel, pass ->
                    pass.getQuads(state, null, random).stream().filter(quad -> quad.getDirection() == Direction.SOUTH).map(HotMetalGlow::halo).toList());
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) { return List.of(Glow.HALO); }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}

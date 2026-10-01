package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.Civilization;
import dev.civilization.EquipmentGrade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Adds blemishes after the active pack's own item model has been selected. */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GradeModels {
    private static final List<ResourceLocation> ITEMS = catalog();
    private GradeModels() {}

    private static List<ResourceLocation> catalog() {
        var ids = new ArrayList<ResourceLocation>();
        for (String material : List.of("wooden", "stone", "iron", "golden", "diamond", "netherite"))
            for (String shape : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) ids.add(ResourceLocation.withDefaultNamespace(material + "_" + shape));
        for (String material : List.of("leather", "chainmail", "iron", "golden", "diamond", "netherite"))
            for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) ids.add(ResourceLocation.withDefaultNamespace(material + "_" + piece));
        for (String name : List.of("turtle_helmet", "shears", "flint_and_steel", "bow", "crossbow", "trident", "mace", "elytra", "fishing_rod", "brush", "carrot_on_a_stick", "warped_fungus_on_a_stick"))
            ids.add(ResourceLocation.withDefaultNamespace(name));
        for (String name : List.of("stone_saw", "iron_saw", "diamond_saw")) ids.add(ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, name));
        return List.copyOf(ids);
    }

    private static ResourceLocation overlay(ResourceLocation item, int variant) {
        return ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID,
                "item/grade_wear/" + item.getNamespace() + "_" + item.getPath() + "_v" + String.format("%02d", variant));
    }

    @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional event) {
        for (var item : ITEMS) for (int variant = 0; variant < 16; variant++)
            event.register(ModelResourceLocation.standalone(overlay(item, variant)));
    }

    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event) {
        int attached = 0, missingBase = 0, missingOverlay = 0;
        for (var item : ITEMS) {
            var inventory = ModelResourceLocation.inventory(item);
            var base = event.getModels().get(inventory);
            if (base == null || !BuiltInRegistries.ITEM.containsKey(item)) { missingBase++; continue; }
            BakedModel[] overlays = new BakedModel[16];
            boolean complete = true;
            for (int variant = 0; variant < 16; variant++) {
                var model = event.getModels().get(ModelResourceLocation.standalone(overlay(item, variant)));
                if (model == null) { complete = false; break; }
                overlays[variant] = new OffsetModel(model);
            }
            if (complete) { event.getModels().put(inventory, new GradeModel(base, overlays, null)); attached++; }
            else missingOverlay++;
        }
        com.mojang.logging.LogUtils.getLogger().info("Grade item models: attached {}, missing base {}, missing overlay {}", attached, missingBase, missingOverlay);
    }

    private static int layerCount(int grade) {
        return grade >= 100 ? 0 : Math.min(8, Math.max(1, (int)Math.ceil((100 - grade) / 9.0)));
    }

    private static List<BakedModel> wearLayers(BakedModel[] overlays, long seed, int count) {
        var order = new int[overlays.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        var random = new Random(seed);
        var selected = new ArrayList<BakedModel>(count);
        for (int i = 0; i < count; i++) {
            int j = i + random.nextInt(order.length - i);
            int swap = order[i]; order[i] = order[j]; order[j] = swap;
            selected.add(overlays[order[i]]);
        }
        return List.copyOf(selected);
    }

    private static final class GradeModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel[] overlays;
        private final List<BakedModel> active;
        GradeModel(BakedModel base, BakedModel[] overlays, List<BakedModel> active) {
            super(base); this.overlays = overlays; this.active = active;
        }
        @Override public ItemOverrides getOverrides() {
            var delegate = originalModel.getOverrides();
            return new ItemOverrides() {
                @Override public BakedModel resolve(BakedModel base, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                    BakedModel selected = delegate.resolve(originalModel, stack, level, entity, seed);
                    if (selected == null) selected = originalModel;
                    int count = EquipmentGrade.hasGrade(stack) ? layerCount(EquipmentGrade.value(stack)) : 0;
                    return count == 0 ? selected : new GradeModel(selected, overlays,
                            wearLayers(overlays, EquipmentGrade.wearSeed(stack), count));
                }
            };
        }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
            BakedModel transformed = originalModel.applyTransform(context, poseStack, leftHand);
            return transformed == originalModel ? this : new GradeModel(transformed, overlays, active);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            var passes = originalModel.getRenderPasses(stack, fabulous);
            if (active == null) return passes;
            var combined = new ArrayList<BakedModel>(passes);
            combined.addAll(active);
            return combined;
        }
    }

    private static final class OffsetModel extends BakedModelWrapper<BakedModel> {
        private final Map<Direction, List<BakedQuad>> faces = new HashMap<>();
        OffsetModel(BakedModel model) { super(model); }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
            originalModel.applyTransform(context, poseStack, leftHand);
            return this;
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return faces.computeIfAbsent(side, key -> {
                var quads = originalModel.getQuads(null, key, RandomSource.create(42));
                var shifted = new ArrayList<BakedQuad>(quads.size());
                for (var quad : quads) {
                    int[] vertices = quad.getVertices().clone();
                    int stride = vertices.length / 4;
                    var face = quad.getDirection();
                    int axis = face.getAxis() == Direction.Axis.X ? 0 : face.getAxis() == Direction.Axis.Y ? 1 : 2;
                    float shift = face.getAxisDirection().getStep() * 0.002f;
                    for (int i = 0; i < 4; i++) {
                        int position = i * stride + axis;
                        vertices[position] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[position]) + shift);
                    }
                    shifted.add(new BakedQuad(vertices, quad.getTintIndex(), face, quad.getSprite(), quad.isShade()));
                }
                return List.copyOf(shifted);
            });
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }
    }
}

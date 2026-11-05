package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.HotMetal;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Hot metal lights its surroundings as brightly as it glows, dropped or in a player's hands: LambDynamicLights calls
 * this when it loads and asks each item and player for its light every tick.
 */
public final class HotMetalLight implements DynamicLightsInitializer {
    @Override public void onInitializeDynamicLights(DynamicLightsContext context) {
        var event = context.entityLightSourceManager().onRegisterEvent();
        event.register(event.defaultPhaseId(), sources -> {
            sources.register(EntityType.ITEM, Glow.INSTANCE);
            sources.register(EntityType.PLAYER, Glow.INSTANCE);
        });
    }
    /** Only the context form above is called; LambDynamicLights still declares this older one. */
    @SuppressWarnings("removal")
    @Override public void onInitializeDynamicLights(ItemLightSourceManager items) {}

    /** The light of the hottest metal an entity drops or holds. */
    private enum Glow implements EntityLuminance {
        INSTANCE;
        private static final Type TYPE = Type.registerSimple(ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "hot_metal"), INSTANCE);

        @Override public Type type() { return TYPE; }
        @Override public int getLuminance(ItemLightSourceManager items, Entity entity) {
            long now = entity.level().getGameTime();
            return switch (entity) {
                case ItemEntity item -> light(item.getItem(), now);
                case LivingEntity living -> Math.max(light(living.getMainHandItem(), now), light(living.getOffhandItem(), now));
                default -> 0;
            };
        }
        /** Full light at white heat, two thirds at cherry red, fading with the glow. */
        private static int light(ItemStack stack, long now) {
            int glow = HotMetal.glow(stack, now);
            float brightness = ((glow >>> 16 & 0xFF) + (glow >>> 8 & 0xFF) + (glow & 0xFF)) / 765f;
            return Math.round(15 * (glow >>> 24) / 255f * (0.5f + 0.5f * brightness));
        }
    }
}

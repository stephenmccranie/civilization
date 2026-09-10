package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class FarmingLog {
    private final List<BlockDropsEvent> harvests = new ArrayList<>();
    public FarmingLog() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::drops);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stop);
    }
    private void drops(BlockDropsEvent event) {
        if (event.getState().getBlock() instanceof CropBlock && event.getBreaker() instanceof ServerPlayer) harvests.add(event);
    }
    private void tick(ServerTickEvent.Post event) {
        for (var harvest : harvests) {
            if (harvest.isCanceled()) continue;
            var player = (ServerPlayer) harvest.getBreaker();
            var crop = (CropBlock) harvest.getState().getBlock();
            String detail = BuiltInRegistries.BLOCK.getKey(crop) + "@" + harvest.getPos().toShortString()
                    + (crop.isMaxAge(harvest.getState()) ? ":mature" : ":immature");
            for (var drop : harvest.getDrops()) {
                var stack = drop.getItem();
                if (stack.isEmpty()) continue;
                double potential = stack.is(Items.WHEAT) ? FoodCalories.breadCalories() / 3
                        : FoodCalories.of(stack, stack.getFoodProperties(player));
                EnergyLog.production(player, "crop_output", detail, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        0, stack.getCount(), potential * stack.getCount());
            }
        }
        harvests.clear();
    }
    private void stop(ServerStoppedEvent event) { harvests.clear(); }

    public static void crafted(Player player, ItemStack stack, int amount) {
        if (!(player instanceof ServerPlayer) || amount <= 0 || stack.getFoodProperties(player) == null) return;
        String input = stack.is(FarmingContent.RATION.get()) ? "minecraft:bread" : stack.is(Items.BREAD) ? "minecraft:wheat" : null;
        EnergyLog.production(player, "food_produced", "manual_craft_or_processing",
                new EnergyLog.Production(input, input == null ? 0 : amount * 3,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), amount,
                        FoodCalories.of(stack, stack.getFoodProperties(player)) * amount));
    }
}

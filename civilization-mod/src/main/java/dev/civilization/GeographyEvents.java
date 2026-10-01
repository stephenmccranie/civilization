package dev.civilization;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = "civilization")
public final class GeographyEvents {
    public static Component report(Geography.Site site) {
        return Component.translatable("message.civilization.geography.report",
                Component.translatable("message.civilization.geography." + site.reason(), site.minSoilY(), site.maxSoilY()),
                Component.translatable("message.civilization.geography." + (site.woodland() ? "woodland" : "ordinary"),
                        GeographyConfig.OUTSIDE_WOODLAND.get()));
    }
    @SubscribeEvent public static void inspect(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !event.getItemStack().is(ItemTags.HOES)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            var pos = SoilSystem.cropBase(player.level(),event.getPos());
            var state=player.level().getBlockState(pos);
            if (state.is(Geography.CROPS)||SoilSystem.crop(state)) pos = pos.below();
            String water=SoilSystem.describe(player.serverLevel(),pos);
            player.displayClientMessage(water.isEmpty()?report(Geography.inspect(player.serverLevel(),pos)):Component.literal(water),true);
        }
    }
    // Planting remains possible for decoration and old builds; explain immediately why it will not grow.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void planted(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getPlacedBlock().is(Geography.CROPS)
                && !Geography.canFarm(player.serverLevel(), event.getPos().below()))
            player.displayClientMessage(Component.translatable("message.civilization.geography.dormant"), true);
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("civilization").then(Commands.literal("geography")
                .executes(context -> {
                    var source = context.getSource();
                    var soil = BlockPos.containing(source.getPosition()).below();
                    source.sendSuccess(() -> Component.translatable("message.civilization.geography.at", soil.toShortString())
                            .append(report(Geography.inspect(source.getLevel(), soil))), false);
                    return 1;
                })));
    }
    @SubscribeEvent public static void tags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) Geography.tagsChanged();
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) Geography.unload(level);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { Geography.clear(); }
}

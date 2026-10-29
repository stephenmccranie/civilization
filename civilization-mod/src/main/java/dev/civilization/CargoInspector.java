package dev.civilization;

import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** On-demand operator audit. No ticking, stock edits, recursive contents, or capacity enforcement. */
@EventBusSubscriber(modid = "civilization")
public final class CargoInspector {
    private CargoInspector() {}

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("civilization").then(
                Commands.literal("cargo").requires(source -> source.hasPermission(2))
                    .executes(context -> report(context.getSource(),
                            StoneCargo.inspect(context.getSource().getPlayerOrException().getInventory()), "Player inventory"))
                    .then(Commands.literal("container")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context ->
                            inspectContainer(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))));
    }

    static int inspectContainer(net.minecraft.commands.CommandSourceStack source, BlockPos pos) {
        var level = source.getLevel();
        if (!level.hasChunkAt(pos)) {
            source.sendFailure(Component.literal("Container must be loaded."));
            return 0;
        }
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof Container container)) {
            source.sendFailure(Component.literal("This block has no directly inspectable item inventory. Bulk stores and tanks need separate adapters."));
            return 0;
        }
        if (entity instanceof RandomizableContainerBlockEntity loot && loot.getLootTable() != null) {
            source.sendFailure(Component.literal("Open this container normally before inspecting it; its loot has not been generated."));
            return 0;
        }
        return report(source, StoneCargo.inspect(container), "One block inventory at " + pos.toShortString()
                + " (each double-chest half is inspected separately)");
    }

    private static int report(net.minecraft.commands.CommandSourceStack source, StoneCargo.InventorySample sample, String label) {
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "%s | Supported stone: material %.3f m³ / packed storage %.3f m³ / estimated mass %.1f kg | %d supported stacks / %d unknown stacks",
                label, sample.materialVolume().cubicMeters(), sample.storageVolume().cubicMeters(),
                sample.massKg(), sample.supportedStacks(), sample.unknownStacks())), false);
        source.sendSuccess(() -> Component.literal("Prototype audit only: stone/cobblestone and their saw-cut pieces. Unknown stacks and nested contents are excluded, not weightless. No carrying/storage limits or vehicle mass changes are applied."), false);
        return 1;
    }
}

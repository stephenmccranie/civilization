package dev.civilization.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.civilization.PreviewConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class PreviewCommands {
    private PreviewCommands() {}
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        var modes = Commands.literal("preview");
        for (var mode : PreviewConfig.Mode.values()) {
            String name = mode.name().toLowerCase(java.util.Locale.ROOT);
            modes.then(Commands.literal(name).executes(context -> {
                PreviewConfig.MODE.set(mode);
                PreviewConfig.SPEC.save();
                context.getSource().sendSuccess(() -> Component.translatable("message.civilization.preview_mode", name), false);
                return 1;
            }));
        }
        modes.then(Commands.literal("opacity").then(Commands.argument("value", DoubleArgumentType.doubleArg(0.05, 0.6))
                .executes(context -> {
                    double opacity = DoubleArgumentType.getDouble(context, "value");
                    PreviewConfig.OPACITY.set(opacity);
                    PreviewConfig.SPEC.save();
                    context.getSource().sendSuccess(() -> Component.translatable("message.civilization.preview_opacity", Math.round(opacity * 100)), false);
                    return 1;
                })));
        event.getDispatcher().register(Commands.literal("civilization").then(modes));
    }
}

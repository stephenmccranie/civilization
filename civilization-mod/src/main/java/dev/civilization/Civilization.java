package dev.civilization;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Civilization.MOD_ID)
public final class Civilization {
    public static final String MOD_ID = "civilization";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String version;

    public Civilization(ModContainer container, net.neoforged.bus.api.IEventBus modBus) {
        version = container.getModInfo().getVersion().toString();
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CalorieConfig.SPEC);
        modBus.addListener(CaloriePayload::register);
        RecoveryItems.register(modBus);
        FarmingContent.register(modBus);
        KilnContent.register(modBus);
        CookingContent.register(modBus);
        modBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, MaterialRules::components);
        new CalorieSystem();
        new EnergyLog();
        new Foraging();
        new FarmingLog();
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        LOGGER.info("Civilization {} initialized", version);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        // Read-only and available without operator privileges, including survival worlds.
        event.getDispatcher().register(Commands.literal(MOD_ID)
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(
                            () -> Component.translatable("command.civilization.status", version), false);
                    return 1;
                })));
        LOGGER.info("Registered /civilization status");
    }
}

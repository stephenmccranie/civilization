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
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, GeographyConfig.SPEC, "civilization-geography.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CivicConfig.SPEC, "civilization-civic.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, PreviewConfig.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, TemperatureWashConfig.SPEC, "civilization-temperature-wash.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,WeatherConfig.SPEC,"civilization-weather.toml");
        modBus.addListener(WeatherPayload::register);
        modBus.addListener(ThermalPayload::register);
        ThermalContent.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,ThermalConfig.SPEC,"civilization-heat.toml");
        SoilChunk.register(modBus);
        OilRetrofitChunk.register(modBus);
        Accessories.register(modBus);
        modBus.addListener(CaloriePayload::register);
        modBus.addListener(LandPayload::register);
        modBus.addListener(TradePayload::register);
        modBus.addListener(SurveyPayload::register);
        modBus.addListener(UraniumRadiationPayload::register);
        RecoveryItems.register(modBus);
        EquipmentGrade.register(modBus);
        FarmingContent.register(modBus);
        KilnContent.register(modBus);
        WorkshopContent.register(modBus);
        CookingContent.register(modBus);
        PrototypeStoveContent.register(modBus);
        BakingOvenContent.register(modBus);
        FirearmContent.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,FirearmConfig.SPEC,"civilization-firearms.toml");
        CuttingContent.register(modBus);
        BuilderLineContent.register(modBus);
        CoalMiningContent.register(modBus);
        RoadContent.register(modBus);
        CivicContent.register(modBus);
        ArenaContent.register(modBus);
        IndustrialContent.register(modBus);
        FrontierContent.register(modBus);
        OilEngineContent.register(modBus);
        BulkContent.register(modBus);
        BoatContent.register(modBus);
        AirshipContent.register(modBus);
        modBus.addListener(BoatPayload::register);
        CivilizationCreativeTab.register(modBus);
        modBus.addListener(VanillaRetirement::creative);
        modBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, MaterialRules::components);
        new CalorieSystem();
        new EnergyLog();
        new Foraging();
        new GeigerSurvey();
        new UraniumRadiationSystem();
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

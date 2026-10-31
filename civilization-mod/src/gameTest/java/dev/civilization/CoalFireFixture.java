package dev.civilization;

/** Existing recipe tests start with an already-lit fire; ignition has separate lifecycle coverage. */
@net.neoforged.fml.common.EventBusSubscriber(modid="civilization")
final class CoalFireFixture {
    @net.neoforged.bus.api.SubscribeEvent
    public static void resetPreviousRun(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer))return;
        var level=event.getServer().overworld();
        int previous=ThermalField.get(level).queuedSources();
        // The reused test save keeps heat sources at old randomized suite locations unloaded.
        // They eventually fill the production queue cap and prevent new fixture emissions.
        level.getDataStorage().set("civilization_heat",new ThermalField());
        System.out.println("GAMETEST_HEAT_RESET previousSources="+previous);
    }
    static void light(net.minecraft.world.level.block.entity.BlockEntity machine){
        var tag=new net.minecraft.nbt.CompoundTag();var state=new net.minecraft.nbt.CompoundTag();state.putBoolean("lit",true);tag.put("coalFire",state);
        if(machine instanceof KilnBlockEntity k)k.fire.load(tag);
        else if(machine instanceof WorkshopBlockEntity w)w.fire.load(tag);
        else if(machine instanceof IndustrialBlockEntity i&&i.coalPowered())i.fire.load(tag);
    }
}

package dev.civilization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Initialize once; NeoForge owns fractional advancement, persistence, and client synchronization. */
@EventBusSubscriber(modid="civilization")
public final class WorldTime {
    public static final int CYCLE_MINUTES = 60;
    @SubscribeEvent public static void loaded(LevelEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
            var defaults=level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Defaults::new,Defaults::load),"civilization_world_defaults");
            defaults.apply(level);
        }
    }
    public static final class Defaults extends SavedData {
        private boolean timeInitialized;
        public void apply(ServerLevel level) {
            if(timeInitialized)return;
            // Respect worlds that already have an explicitly chosen clock speed.
            if(level.getDayTimePerTick()<0)level.setDayTimePerTick(20f/CYCLE_MINUTES);
            timeInitialized=true;setDirty();
        }
        public static Defaults load(CompoundTag tag,HolderLookup.Provider provider) {
            var data=new Defaults();data.timeInitialized=tag.getBoolean("timeInitialized");return data;
        }
        @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider) {tag.putBoolean("timeInitialized",timeInitialized);return tag;}
    }
}

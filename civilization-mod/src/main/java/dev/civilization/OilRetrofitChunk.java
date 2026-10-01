package dev.civilization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** One bit per visited oil-site chunk prevents the optional backfill from restoring extracted oil. */
public final class OilRetrofitChunk implements INBTSerializable<CompoundTag> {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Civilization.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OilRetrofitChunk>> TYPE =
            TYPES.register("oil_retrofit", () -> AttachmentType.serializable(OilRetrofitChunk::new).build());
    public boolean done;

    private OilRetrofitChunk() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
        var tag = new CompoundTag();
        tag.putBoolean("done", done);
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag tag) {
        done = tag.getBoolean("done");
    }
}

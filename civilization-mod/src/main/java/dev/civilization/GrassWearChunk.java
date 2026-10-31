package dev.civilization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.*;

public final class GrassWearChunk implements INBTSerializable<CompoundTag> {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,Civilization.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<GrassWearChunk>> TYPE=TYPES.register("grass_wear",()->AttachmentType.serializable(GrassWearChunk::new).build());
    public static void register(net.neoforged.bus.api.IEventBus bus){TYPES.register(bus);}
    public final GrassWear wear=new GrassWear();
    @Override public CompoundTag serializeNBT(HolderLookup.Provider lookup){var tag=new CompoundTag();tag.putLongArray("footfalls",wear.save());return tag;}
    @Override public void deserializeNBT(HolderLookup.Provider lookup,CompoundTag tag){wear.load(tag.getLongArray("footfalls"));}
}

package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RestingSkilletEntity extends BlockEntity implements SkilletHolder {
    private SkilletContents skillet = new SkilletContents();
    public RestingSkilletEntity(BlockPos p, BlockState s) { super(PrototypeStoveContent.RESTING_ENTITY.get(), p, s); }
    @Override public SkilletContents skillet() { return skillet; }
    public void put(SkilletContents s) { skillet = s; changed(); }
    public void changed() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    public void tick() {
        if (skillet.warmth() == 0) return;
        skillet.tick(0); setChanged(); if (level.getGameTime() % 5 == 0 || skillet.warmth() == 0) changed();
    }
    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) { super.saveAdditional(t, r); t.put("skillet", skillet.save()); }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) { super.loadAdditional(t, r); skillet = SkilletContents.load(t.getCompound("skillet")); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

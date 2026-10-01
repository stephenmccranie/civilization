package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AirshipBlockEntity extends BlockEntity {
    private double power=AirshipFlight.DEFAULT_POWER;
    public AirshipBlockEntity(BlockPos pos,BlockState state) { super(AirshipContent.ENTITY.get(),pos,state); }
    public double power() { return power; }
    public void power(double value) { if(!AirshipFlight.validPower(value))throw new IllegalArgumentException("Use a finite, non-negative number.");power=value;setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries);tag.putDouble("power",power); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.loadAdditional(tag,registries);double v=tag.contains("power")?tag.getDouble("power"):AirshipFlight.DEFAULT_POWER;power=AirshipFlight.validPower(v)?v:0; }
}

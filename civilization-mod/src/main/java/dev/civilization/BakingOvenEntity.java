package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BakingOvenEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private double dial=.5;
    public float door,previousDoor;
    boolean removing;
    public BakingOvenEntity(BlockPos p,BlockState s){super(BakingOvenContent.ENTITY.get(),p,s);}
    public double dial(){return dial;}
    public void turn(double delta){if(!Double.isFinite(delta)||Math.abs(delta)>.1)return;dial=Math.clamp(dial+delta,0,1);setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void clientTick(){previousDoor=door;door=(float)Math.clamp(door+(getBlockState().getValue(BakingOvenBlock.OPEN)?.0625:-.0625),0,1);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putDouble("dial",dial);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);double d=t.contains("dial")?t.getDouble("dial"):.5;dial=Double.isFinite(d)?Math.clamp(d,0,1):.5;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
}

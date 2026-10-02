package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent falling batch. No pickup/despawn; a blocked landing retains its stock. */
public final class LooseCoalEntity extends Entity {
    private static final EntityDataAccessor<Integer> UNITS=SynchedEntityData.defineId(LooseCoalEntity.class,EntityDataSerializers.INT);
    public LooseCoalEntity(EntityType<? extends LooseCoalEntity> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(UNITS,0);}
    public int units(){return entityData.get(UNITS);}
    public static boolean spawn(net.minecraft.server.level.ServerLevel l,Vec3 at,int units){
        return create(l,at,units)!=null;
    }
    public static LooseCoalEntity create(net.minecraft.server.level.ServerLevel l,Vec3 at,int units){
        if(units<=0)return null;var e=new LooseCoalEntity(CoalMiningContent.LOOSE.get(),l);e.entityData.set(UNITS,units);e.setPos(at);return l.addFreshEntity(e)?e:null;
    }
    @Override public void tick(){
        super.tick();if(!level().hasChunkAt(blockPosition()))return;
        if(getY()<level().getMinBuildHeight()+.1){setPos(getX(),level().getMinBuildHeight()+.1,getZ());setDeltaMovement(Vec3.ZERO);}
        else if(!onGround()){
            var v=getDeltaMovement().add(0,-.04,0);if(!level().hasChunkAt(BlockPos.containing(position().add(v)))){setDeltaMovement(Vec3.ZERO);return;}
            setDeltaMovement(v);move(MoverType.SELF,v);setDeltaMovement(getDeltaMovement().scale(.96));
        }
        if(!level().isClientSide&&(onGround()||getY()<level().getMinBuildHeight()+1)&&tickCount%10==0){
            var l=(net.minecraft.server.level.ServerLevel)level();int remaining=units();BlockPos base=blockPosition();
            remaining-=RawCoalPileBlock.insert(l,base,remaining);
            for(int dx=-1;dx<=1&&remaining>0;dx++)for(int dz=-1;dz<=1&&remaining>0;dz++)remaining-=RawCoalPileBlock.insert(l,base.offset(dx,0,dz),remaining);
            entityData.set(UNITS,remaining);if(remaining==0)discard();
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){t.putInt("rawCoal",units());}
    @Override protected void readAdditionalSaveData(CompoundTag t){entityData.set(UNITS,Math.clamp(t.getInt("rawCoal"),0,4096));}
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return true;}
}

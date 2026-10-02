package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native rail physics, one saved cargo amount; no general inventory. */
public final class CoalMinecart extends AbstractMinecart {
    public static final int CAPACITY=4096;
    private long lastPush=Long.MIN_VALUE;
    private static final EntityDataAccessor<Integer> LOAD=SynchedEntityData.defineId(CoalMinecart.class,EntityDataSerializers.INT);
    public CoalMinecart(EntityType<? extends CoalMinecart> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(LOAD,0);}
    public int load(){return entityData.get(LOAD);}
    public int insert(int offered){int n=Math.min(Math.max(0,offered),CAPACITY-load());entityData.set(LOAD,load()+n);return n;}
    public int extract(int requested){int n=Math.min(Math.max(0,requested),load());entityData.set(LOAD,load()-n);return n;}
    @Override public Type getMinecartType(){return Type.CHEST;}
    @Override protected Item getDropItem(){return CoalMiningContent.CART_ITEM.get();}
    @Override public BlockState getDefaultDisplayBlockState(){return load()==0?net.minecraft.world.level.block.Blocks.AIR.defaultBlockState():CoalMiningContent.PILE.get().defaultBlockState().setValue(RawCoalPileBlock.UNITS,Math.min(64,Math.max(1,(load()*64+CAPACITY-1)/CAPACITY)));}
    @Override public int getDefaultDisplayOffset(){return 8;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        // Native Creative destruction discards directly instead of calling destroy.
        if(!level().isClientSide&&source.getEntity() instanceof Player p&&p.isCreative()&&!isInvulnerableTo(source)&&load()>0){
            if(!LooseCoalEntity.spawn((net.minecraft.server.level.ServerLevel)level(),position().add(0,.4,0),load()))return false;
            extract(load());
        }
        return super.hurt(source,amount);
    }
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!CivicAccess.allowed(level(),blockPosition(),p)||p.distanceToSqr(this)>16)return InteractionResult.FAIL;
        var s=p.getMainHandItem();
        if(s.is(CoalMiningContent.SHOVEL.get())){
            int n=insert(CoalMiningContent.load(s));CoalMiningContent.load(s,CoalMiningContent.load(s)-n);
            p.displayClientMessage(Component.literal("Coal cart: "+load()+" / "+CAPACITY+" raw units"),true);
        }else if(s.isEmpty()){
            long now=level().getGameTime();if(lastPush!=Long.MIN_VALUE&&now-lastPush<10)return InteractionResult.CONSUME;lastPush=now;
            var d=p.getLookAngle();double length=Math.sqrt(d.x*d.x+d.z*d.z);if(length>0){setDeltaMovement(getDeltaMovement().add(d.x/length*.12,0,d.z/length*.12));hurtMarked=true;}
            p.displayClientMessage(Component.literal("Push coal cart · "+(load()/4.0)+" Coal after preparation"),true);
            if(!p.isCreative())CalorieFoodData.of(p).spendLabor(p,1,false,"coal_cart_push");
        }
        return InteractionResult.CONSUME;
    }
    @Override public void destroy(net.minecraft.world.damagesource.DamageSource source){
        if(load()>0&&!level().isClientSide){if(!LooseCoalEntity.spawn((net.minecraft.server.level.ServerLevel)level(),position().add(0,.4,0),load()))return;extract(load());}
        super.destroy(source);
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putInt("rawCoal",load());}
    @Override protected void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);entityData.set(LOAD,Math.clamp(t.getInt("rawCoal"),0,CAPACITY));}
}

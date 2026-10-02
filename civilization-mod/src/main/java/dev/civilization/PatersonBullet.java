package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Swept segment collision prevents fast projectiles tunnelling through targets. */
public final class PatersonBullet extends Projectile {
    private double damage=8,gravity=.025,drag=.995;
    private int lifetime=60;
    public PatersonBullet(EntityType<? extends PatersonBullet> type,Level level){super(type,level);if(!level.isClientSide){damage=FirearmConfig.DAMAGE.get();gravity=FirearmConfig.GRAVITY.get();drag=FirearmConfig.DRAG.get();lifetime=FirearmConfig.LIFETIME.get();}}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override public void tick(){
        super.tick();if(tickCount>lifetime||!level().hasChunkAt(blockPosition())){discard();return;}
        var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(!level().isClientSide&&hit.getType()!=HitResult.Type.MISS){
            if(hit instanceof EntityHitResult e&&getOwner() instanceof net.minecraft.world.entity.player.Player p&&CivicAccess.allowed(level(),e.getEntity().blockPosition(),p)){
                e.getEntity().hurt(damageSources().thrown(this,p),(float)damage);
            }
            discard();return;
        }
        var v=getDeltaMovement();setPos(position().add(v));updateRotation();setDeltaMovement(v.scale(drag).add(0,-gravity,0));
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putDouble("damage",damage);t.putDouble("gravity",gravity);t.putDouble("drag",drag);t.putInt("lifetime",lifetime);t.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);damage=Math.clamp(t.getDouble("damage"),.1,100);gravity=Math.clamp(t.getDouble("gravity"),0,1);drag=Math.clamp(t.getDouble("drag"),.5,1);lifetime=Math.clamp(t.getInt("lifetime"),1,200);tickCount=Math.max(0,t.getInt("age"));}
}

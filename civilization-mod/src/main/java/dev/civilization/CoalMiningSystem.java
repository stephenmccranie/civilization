package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="civilization")
public final class CoalMiningSystem {
    private record Swing(net.minecraft.world.item.ItemStack tool,int slot,long start,int contact,net.minecraft.server.level.ServerLevel level){}
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    public static boolean start(ServerPlayer p){
        var stack=p.getMainHandItem();if(!stack.is(CoalMiningContent.PICK.get())||!p.isAlive()||p.isSpectator()||p.getCooldowns().isOnCooldown(stack.getItem())||SWINGS.containsKey(p.getUUID()))return false;
        int pace=CalorieFoodData.active(p)&&CalorieFoodData.of(p).isDepleted()?2:1;
        long now=p.serverLevel().getGameTime();SWINGS.put(p.getUUID(),new Swing(stack,p.getInventory().selected,now,CoalPickItem.CONTACT*pace,p.serverLevel()));p.getCooldowns().addCooldown(stack.getItem(),CoalPickItem.DURATION*pace);
        var t=CoalPickItem.state(stack);t.putLong("coalSwing",now);t.putInt("coalDuration",CoalPickItem.DURATION*pace);t.putFloat("swingYaw",p.getYRot());t.putFloat("swingPitch",p.getXRot());CoalPickItem.save(stack,t);
        return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p)step(p);}
    public static void step(ServerPlayer p){
        var swing=SWINGS.get(p.getUUID());if(swing==null)return;
        if(!p.isAlive()||p.isSpectator()||p.serverLevel()!=swing.level||p.getInventory().selected!=swing.slot||p.getMainHandItem()!=swing.tool){SWINGS.remove(p.getUUID());return;}
        if(swing.level.getGameTime()-swing.start<swing.contact)return;
        SWINGS.remove(p.getUUID());p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.35f,.7f);strike(p);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SWINGS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SWINGS.clear();}
    public static boolean strike(ServerPlayer p){
        if(!p.getMainHandItem().is(CoalMiningContent.PICK.get())||!p.isAlive()||p.isSpectator())return false;
        var l=p.serverLevel();var eye=p.getEyePosition();double reach=Math.min(4.5,p.blockInteractionRange());var end=eye.add(p.getLookAngle().scale(reach));
        for(int i=0;i<=10;i++)if(!Chunks.loaded(l,BlockPos.containing(eye.lerp(end,i/10.0))))return false;
        var hit=l.clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.BLOCK)return false;var pos=hit.getBlockPos();var state=l.getBlockState(pos);
        if(!state.is(IndustrialContent.COAL_SEAM.get())&&!state.is(CoalMiningContent.FACE.get()))return false;
        if(!l.mayInteract(p,pos)||!CivicAccess.allowed(l,pos,p)||BoatSystem.at(l,pos)!=null||AirshipSystem.at(l,pos)!=null)return false;
        long mask=l.getBlockEntity(pos) instanceof CoalWorkfaceEntity e?e.mask():CoalGeometry.FULL;
        long removed=CoalGeometry.chip(mask,hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)),hit.getDirection());if(removed==0)return false;
        var event=new BlockEvent.BreakEvent(l,pos,state,p);if(NeoForge.EVENT_BUS.post(event).isCanceled())return false;
        // Listeners may change the target or permissions. Never pay from a stale snapshot.
        if(!l.getBlockState(pos).equals(state)||!CivicAccess.allowed(l,pos,p)||!l.mayInteract(p,pos))return false;
        if(l.getBlockEntity(pos) instanceof CoalWorkfaceEntity e&&e.mask()!=mask)return false;
        // One physical chunk breaks away from the struck surface, including deep notches.
        // Its gentle outward impulse, gravity and ledge rolling replace teleporting stock
        // to the outside of the entire block.
        var outward=Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        // Pick an actual removed cell nearest the hit. A point at the hit's edge
        // can put the chunk's width into adjacent, uncut material in a deep notch.
        Vec3 center=null;double nearest=Double.MAX_VALUE;
        for(int i=0;i<64;i++)if((removed&(1L<<i))!=0){
            var c=CoalGeometry.box(i).getCenter().add(Vec3.atLowerCornerOf(pos));double distance=c.distanceToSqr(hit.getLocation());
            if(distance<nearest){nearest=distance;center=c;}
        }
        var at=center.add(0,-.11,0);
        var carrier=LooseCoalEntity.create(l,at,Long.bitCount(removed));if(carrier==null)return false;
        carrier.setDeltaMovement(outward.scale(.07).add(0,.02,0));
        boolean committed;
        if(mask==removed)committed=l.removeBlock(pos,false);
        else {
            committed=state.is(CoalMiningContent.FACE.get())||l.setBlockAndUpdate(pos,CoalMiningContent.FACE.get().defaultBlockState());
            if(committed&&l.getBlockEntity(pos) instanceof CoalWorkfaceEntity e)e.mask(mask&~removed);
            else committed=false;
        }
        if(!committed){carrier.discard();return false;}
        if(!p.isCreative()){
            p.getMainHandItem().hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
            CalorieFoodData.of(p).spendLabor(p,2,true,"coal_chipping");
        }
        l.playSound(null,pos,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.8f,.65f+l.random.nextFloat()*.15f);
        l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.COAL_BLOCK.defaultBlockState()),at.x,at.y,at.z,8,.07,.07,.07,.025);
        return true;
    }
}

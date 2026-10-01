package dev.civilization;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server owns ammunition, reload completion, cadence and projectile spawning. */
public final class PatersonItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public PatersonItem(){super(new Properties().stacksTo(1));}
    public static CompoundTag state(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static int rounds(ItemStack s){return Math.clamp(state(s).getInt("rounds"),0,5);}
    public static boolean reloading(ItemStack s){return state(s).contains("reloadEnd");}
    private static void save(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static int inventoryAmmo(Player p){int n=0;for(var s:p.getInventory().items)if(s.is(FirearmContent.AMMO.get()))n+=s.getCount();return n;}
    public static boolean reload(Player p){
        var s=p.getMainHandItem();if(p.level().isClientSide||!s.is(FirearmContent.PATERSON.get())||p.isSpectator()||!p.isAlive()||reloading(s)||rounds(s)==5||inventoryAmmo(p)==0)return false;
        var t=state(s);long now=p.level().getGameTime();t.putLong("reloadStart",now);t.putLong("reloadEnd",now+FirearmConfig.RELOAD.get());save(s,t);return true;
    }
    public static boolean fire(Player p){
        var s=p.getMainHandItem();if(!(p.level() instanceof ServerLevel l)||!s.is(FirearmContent.PATERSON.get())||!p.isAlive()||p.isSpectator()||reloading(s)||p.getCooldowns().isOnCooldown(s.getItem()))return false;
        if(rounds(s)==0){p.displayClientMessage(Component.literal("Empty — press Reload with .36 ammunition in your inventory."),true);p.getCooldowns().addCooldown(s.getItem(),8);return false;}
        var bullet=new PatersonBullet(FirearmContent.BULLET.get(),l);bullet.setOwner(p);bullet.setPos(p.getEyePosition());bullet.shootFromRotation(p,p.getXRot(),p.getYRot(),0,FirearmConfig.SPEED.get().floatValue(),.2f);
        if(!l.addFreshEntity(bullet))return false;
        var t=state(s);t.putInt("rounds",rounds(s)-1);t.putInt("chamber",(Math.floorMod(t.getInt("chamber"),5)+1)%5);t.putLong("shotTick",l.getGameTime());save(s,t);
        p.getCooldowns().addCooldown(s.getItem(),FirearmConfig.COOLDOWN.get());
        l.playSound(null,p.getX(),p.getEyeY(),p.getZ(),FirearmContent.SHOT.get(),SoundSource.PLAYERS,3.0f,.98f+l.random.nextFloat()*.04f);
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,p.getEyePosition().x+p.getLookAngle().x*.7,p.getEyePosition().y+p.getLookAngle().y*.7,p.getEyePosition().z+p.getLookAngle().z*.7,8,.08,.08,.08,.03);
        if(p instanceof net.minecraft.server.level.ServerPlayer server)PacketDistributor.sendToPlayer(server,new FirearmPayload(2));
        return true;
    }
    @Override public void inventoryTick(ItemStack s,Level l,Entity owner,int slot,boolean selected){
        if(l.isClientSide||!reloading(s)||!(owner instanceof Player p))return;
        var t=state(s);
        if(!selected||!p.isAlive()){t.remove("reloadStart");t.remove("reloadEnd");save(s,t);return;}
        if(l.getGameTime()<t.getLong("reloadEnd"))return;
        int needed=5-rounds(s),loaded=0;
        for(var ammo:p.getInventory().items)if(ammo.is(FirearmContent.AMMO.get())&&loaded<needed){int take=Math.min(needed-loaded,ammo.getCount());ammo.shrink(take);loaded+=take;}
        t.putInt("rounds",rounds(s)+loaded);t.remove("reloadStart");t.remove("reloadEnd");save(s,t);
        l.playSound(null,p.blockPosition(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,.5f,1.3f);
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack old,ItemStack next,boolean changed){return changed||!old.is(next.getItem());}
    @Override public boolean onLeftClickEntity(ItemStack s,Player p,Entity target){return true;}
    @Override public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState b,Level l,net.minecraft.core.BlockPos pos,Player p){return false;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> text,TooltipFlag f){text.add(Component.literal(rounds(s)+" / 5 chambers"));text.add(Component.literal("Left-click: fire. Reload: R (rebindable)."));text.add(Component.literal("Reload consumes .36 ammunition; switching away cancels."));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
}

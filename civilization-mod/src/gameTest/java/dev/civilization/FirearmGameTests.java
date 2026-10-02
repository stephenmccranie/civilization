package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class FirearmGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"paterson"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(3,3,3)).getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,FirearmContent.PATERSON.toStack());return p;}
    @GameTest(template="industrial") public static void reloadCustodyAndCadence(GameTestHelper h){
        var p=player(h);var s=p.getMainHandItem();h.assertTrue(!PatersonItem.reload(p)&&!PatersonItem.fire(p),"Empty gun cannot reload/fire without ammo");p.getCooldowns().removeCooldown(s.getItem());p.getInventory().setItem(1,FirearmContent.AMMO.toStack(3));
        h.assertTrue(PatersonItem.reload(p),"Inventory ammo starts reload");((PatersonItem)s.getItem()).inventoryTick(s,h.getLevel(),p,0,false);h.assertTrue(!PatersonItem.reloading(s)&&PatersonItem.inventoryAmmo(p)==3,"Switch cancels without consuming ammunition");
        PatersonItem.reload(p);var t=PatersonItem.state(s);t.putLong("reloadEnd",h.getLevel().getGameTime());s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(t));((PatersonItem)s.getItem()).inventoryTick(s,h.getLevel(),p,0,true);
        h.assertTrue(PatersonItem.rounds(s)==3&&PatersonItem.inventoryAmmo(p)==0,"Partial reload conserves three rounds");h.assertTrue(PatersonItem.rounds(s.copy())==3,"Loaded state survives stack copy");
        h.assertTrue(!PatersonItem.fire(p),"Loaded gun requires manual cocking");
        h.assertTrue(PatersonItem.cock(p)&&PatersonItem.armed(s),"R cocks loaded hammer");var armed=PatersonItem.state(s);armed.putLong("cockTick",h.getLevel().getGameTime()-6);s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(armed));
        h.assertTrue(PatersonItem.fire(p)&&PatersonItem.rounds(s)==2,"Successful shot consumes exactly one round");h.assertTrue(!PatersonItem.fire(p)&&PatersonItem.rounds(s)==2,"Packet repetition cannot bypass cadence");
        p.getCooldowns().removeCooldown(s.getItem());h.assertTrue(!PatersonItem.fire(p)&&!PatersonItem.armed(s),"Every shot lowers hammer and requires cocking again");
        p.getInventory().setItem(1,FirearmContent.AMMO.toStack(2));h.assertTrue(PatersonItem.cock(p)&&PatersonItem.reload(p)&&!PatersonItem.armed(s),"Partial reload lowers cocked hammer");
        var partial=PatersonItem.state(s);partial.putLong("reloadEnd",h.getLevel().getGameTime());s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(partial));((PatersonItem)s.getItem()).inventoryTick(s,h.getLevel(),p,0,true);
        h.assertTrue(PatersonItem.rounds(s)==4&&PatersonItem.inventoryAmmo(p)==0&&!PatersonItem.armed(s),"Partial reload preserves existing chambers and leaves hammer down");h.succeed();
    }
    @GameTest(template="industrial") public static void sweptDamageAndWallStops(GameTestHelper h){
        var l=h.getLevel();var p=player(h);var start=h.absolutePos(new BlockPos(4,5,4)).getCenter();
        for(var at:BlockPos.betweenClosed(BlockPos.containing(start).offset(-1,-1,-1),BlockPos.containing(start).offset(7,2,1)))l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        var pig=EntityType.PIG.create(l);pig.setNoAi(true);pig.setNoGravity(true);pig.setPos(start.add(4,0,0));l.addFreshEntity(pig);float health=pig.getHealth();
        var bullet=new PatersonBullet(FirearmContent.BULLET.get(),l);bullet.setOwner(p);bullet.setPos(start);bullet.setDeltaMovement(6,0,0);l.addFreshEntity(bullet);bullet.tick();h.assertTrue(bullet.isRemoved()&&pig.getHealth()<health,"Fast swept projectile damages entity between tick endpoints");
        pig.setHealth(health);pig.invulnerableTime=0;l.setBlockAndUpdate(BlockPos.containing(start.add(2,0,0)),Blocks.STONE.defaultBlockState());var blocked=new PatersonBullet(FirearmContent.BULLET.get(),l);blocked.setOwner(p);blocked.setPos(start);blocked.setDeltaMovement(6,0,0);l.addFreshEntity(blocked);blocked.tick();h.assertTrue(blocked.isRemoved()&&pig.getHealth()==health,"Solid block stops shot before target; no penetration");pig.discard();h.succeed();
    }
    @GameTest(template="industrial") public static void gravityAndSavedFlight(GameTestHelper h){var l=h.getLevel();var b=new PatersonBullet(FirearmContent.BULLET.get(),l);var pos=h.absolutePos(new BlockPos(6,6,6)).getCenter();for(var at:BlockPos.betweenClosed(BlockPos.containing(pos).offset(-1,-1,-1),BlockPos.containing(pos).offset(2,2,1)))l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());b.setPos(pos);b.setDeltaMovement(.5,0,0);l.addFreshEntity(b);b.tick();h.assertTrue(b.getDeltaMovement().y<0&&b.getDeltaMovement().x<.5,"Bullet drop and drag apply per tick");var tag=new net.minecraft.nbt.CompoundTag();b.saveWithoutId(tag);var copy=new PatersonBullet(FirearmContent.BULLET.get(),l);copy.load(tag);h.assertTrue(copy.tickCount==b.tickCount,"Saved flight retains age instead of resetting lifetime");b.discard();h.succeed();}
}

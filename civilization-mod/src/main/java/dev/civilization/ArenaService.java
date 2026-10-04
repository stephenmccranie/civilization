package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** All actions recheck identity, match revision, world and stock on the server thread. */
public final class ArenaService {
    public static CivicData.Address address(ServerLevel level,BlockPos pos){return new CivicData.Address(level.dimension().location().toString(),pos);}
    public static void placed(ServerLevel level,BlockPos pos,ServerPlayer player,Direction front){
        var d=ArenaData.get(level.getServer());var at=address(level,pos);if(d.pits.containsKey(at))removed(level,pos);
        var p=new ArenaData.Pit();p.at=at;p.owner=player.getUUID();p.front=front;d.pits.put(at,p);d.setDirty();
    }
    public static void removed(ServerLevel level,BlockPos pos){var d=ArenaData.get(level.getServer());var p=d.pits.remove(address(level,pos));if(p!=null){finish(d,p,level,-1,"Controller removed: stakes refunded");d.setDirty();}}
    public static ServerLevel level(MinecraftServer server,ArenaData.Pit p){for(var level:server.getAllLevels())if(level.dimension().location().toString().equals(p.at.dimension()))return level;return null;}
    public static ArenaData.Pit enrolled(ArenaData data,UUID player){for(var pit:data.pits.values())if(pit.side(player)>=0)return pit;return null;}
    public static boolean opposed(ArenaData data,Player attacker,Player target){var p=enrolled(data,attacker.getUUID());return p!=null&&p==enrolled(data,target.getUUID())&&p.phase==ArenaData.LIVE&&p.side(attacker.getUUID())!=p.side(target.getUUID())&&ArenaStructure.inside(p,attacker.position(),true)&&ArenaStructure.inside(p,target.position(),true)&&attacker.level()==target.level();}
    private static void say(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),false);}
    private static void clearAccept(ArenaData.Pit p){Arrays.fill(p.accepted,false);Arrays.fill(p.ready,false);}
    public static boolean spectator(ArenaData.Pit p,UUID player){return p.bets.stream().anyMatch(b->player.equals(b.owner)||player.equals(b.opponent));}
    public static boolean collect(ArenaData d,ServerPlayer player){
        var owed=d.credits.get(player.getUUID());if(owed==null)return false;boolean moved=false;
        for(var it=owed.iterator();it.hasNext();){var stack=it.next();if(CivicItems.exchange(player,ItemStack.EMPTY,0,stack,stack.getCount())){it.remove();moved=true;}}
        if(owed.isEmpty())d.credits.remove(player.getUUID());if(moved)d.setDirty();return moved;
    }
    public static void recover(ArenaData d,ServerPlayer player){var hp=d.recovery.remove(player.getUUID());if(hp!=null){player.setHealth(Math.clamp(hp,1,player.getMaxHealth()));player.clearFire();player.fallDistance=0;
        for(var effect:new ArrayList<>(player.getActiveEffects()))if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)player.removeEffect(effect.getEffect());d.setDirty();}}
    public static void finish(ArenaData d,ArenaData.Pit pit,ServerLevel level,int winner,String why){
        if(pit.phase==ArenaData.RESULT)return;
        // A result is never awarded against a damaged/unloaded blueprint.
        if(winner>=0&&(level==null||!ArenaStructure.problem(level,pit).isEmpty())){winner=-1;why="Arena interrupted: stakes refunded";}
        d.settle(pit,winner,why);if(level!=null){ArenaStructure.gates(level,pit,false);pit.deadline=level.getGameTime()+100;for(var id:pit.fighters)if(id!=null){var fighter=level.getServer().getPlayerList().getPlayer(id);if(fighter!=null){recover(d,fighter);say(fighter,why);}}}
    }
    public static boolean action(ArenaData d,ArenaData.Pit p,ServerPlayer player,int action,int offerId){
        var level=player.serverLevel();var id=player.getUUID();int side=p.side(id);
        if(action==12){boolean moved=collect(d,player);say(player,moved?"Collected available winnings/refunds":"No room or no items to collect");return moved;}
        if(action==13){if(side>=0){finish(d,p,level,-1,"Fighter withdrew: stakes refunded");return true;}return false;}
        if(action==14){if((id.equals(p.owner)||player.hasPermissions(2))&&CivicAccess.allowed(level,p.at.pos(),player)){finish(d,p,level,-1,"Host cancelled: stakes refunded");return true;}return false;}
        if(p.phase!=ArenaData.LOBBY){say(player,"This match is locked");return false;}
        String problem=ArenaStructure.problem(level,p);if(!problem.isEmpty()){say(player,problem);return false;}
        if(action==0||action==1){
            if(side>=0||p.fighters[action]!=null||enrolled(d,id)!=null||spectator(p,id)||player.isSpectator()){say(player,"That fighter slot is unavailable");return false;}
            p.fighters[action]=id;if(p.deadline==0)p.deadline=level.getGameTime()+12000;clearAccept(p);
        }else if(action==2){
            if(side<0||p.ready[0]||p.ready[1])return false;
            var item=player.getMainHandItem();if(item.isEmpty()||item.getCount()>item.getMaxStackSize())return false;
            d.credit(id,p.stakes[side]);p.stakes[side]=item.copy();item.setCount(0);player.getInventory().setChanged();clearAccept(p);
        }else if(action==3){
            if(side<0||p.ready[0]||p.ready[1])return false;d.credit(id,p.stakes[side]);p.stakes[side]=ItemStack.EMPTY;clearAccept(p);
        }else if(action==4){if(side<0||!p.paired()||p.ready[side])return false;p.accepted[side]=true;}
        else if(action==5){
            if(side<0||!p.accepted[0]||!p.accepted[1]||p.ready[side])return false;
            if(player.getHealth()<player.getMaxHealth()||!player.getActiveEffects().isEmpty()||player.isCreative()||player.isSpectator()){say(player,"Ready requires full health, no potion effects and Survival mode");return false;}
            for(var fighterId:p.fighters){var other=level.getServer().getPlayerList().getPlayer(fighterId);if(other==null||other.level()!=level)return false;}
            p.health[side]=player.getHealth();p.ready[side]=true;player.closeContainer();
            var prep=ArenaStructure.position(p.at.pos(),p.front,side==0?-17:17,1,0);var target=Vec3.atBottomCenterOf(prep);
            player.teleportTo(level,target.x,target.y,target.z,Set.of(),side==0?p.front.getClockWise().toYRot():p.front.getCounterClockWise().toYRot(),0);
            if(p.ready[0]&&p.ready[1]){
                for(var it=p.bets.iterator();it.hasNext();){var b=it.next();if(b.opponent==null){d.credit(b.owner,b.stake);it.remove();}}
                p.phase=ArenaData.COUNTDOWN;p.deadline=level.getGameTime()+100;ArenaStructure.gates(level,p,false);
            }
        }else if(action==6||action==7){
            if(side>=0||!p.paired()||spectator(p,id)||p.bets.size()>=64||enrolled(d,id)!=null)return false;
            var stack=player.getMainHandItem();if(stack.isEmpty()||stack.getCount()>stack.getMaxStackSize())return false;
            var b=new ArenaData.Bet();
            while(p.bets.stream().anyMatch(bet->bet.id==p.nextBet))p.nextBet=p.nextBet%32767+1;
            b.id=p.nextBet;p.nextBet=p.nextBet%32767+1;b.side=action-6;b.owner=id;b.stake=stack.copy();stack.setCount(0);player.getInventory().setChanged();p.bets.add(b);
        }else if(action==8){
            var b=p.bets.stream().filter(bet->bet.id==offerId).findFirst().orElse(null);
            if(b==null||b.opponent!=null||side>=0||spectator(p,id)||enrolled(d,id)!=null)return false;
            if(!CivicItems.exchange(player,b.stake,b.stake.getCount(),ItemStack.EMPTY,0)){say(player,"You need the same item and quantity to match this bet");return false;}b.opponent=id;
        }else if(action==9){
            var b=p.bets.stream().filter(bet->bet.id==offerId).findFirst().orElse(null);
            if(b==null||b.opponent!=null||!b.owner.equals(id))return false;d.credit(id,b.stake);p.bets.remove(b);
        }else return false;
        p.changed();d.setDirty();return true;
    }
    private ArenaService() {}
}

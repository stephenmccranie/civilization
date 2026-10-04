package dev.civilization;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="civilization")
public final class ArenaEvents {
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var d=ArenaData.get(e.getServer());
        for(var p:d.pits.values()){
            if(!p.paired()&&p.fighters[0]==null&&p.fighters[1]==null)continue;
            var level=ArenaService.level(e.getServer(),p);
            if(p.phase==ArenaData.RESULT){if(level==null||level.getGameTime()>=p.deadline)d.reset(p);continue;}
            if(e.getServer().getTickCount()%20!=0)continue;
            if(level==null||!level.hasChunkAt(p.at.pos())){ArenaService.finish(d,p,level,-1,"Arena unloaded: stakes refunded");continue;}
            if(!level.getBlockState(p.at.pos()).is(ArenaContent.PIT.get())||!ArenaStructure.problem(level,p).isEmpty()){ArenaService.finish(d,p,level,-1,"Arena damaged: stakes refunded");continue;}
            boolean invalid=false;
            for(int i=0;i<2;i++)if(p.fighters[i]!=null){
                var fighter=e.getServer().getPlayerList().getPlayer(p.fighters[i]);
                if(fighter==null||fighter.level()!=level||!fighter.isAlive()||fighter.isCreative()||fighter.isSpectator()){invalid=true;break;}
                if(p.ready[i]){
                    var c=ArenaStructure.cell(p.at.pos(),p.front,fighter.position());
                    boolean allowed=ArenaStructure.inside(p,fighter.position(),false)
                            && (p.phase==ArenaData.LIVE?(ArenaLayout.FIGHTING.contains(c)||ArenaLayout.prep(i,c.x(),c.z())||Math.abs(c.x())==14&&Math.abs(c.z())<=1):ArenaLayout.prep(i,c.x(),c.z()));
                    if(!allowed)invalid=true;
                }
            }
            // The placer must still have structural rights throughout the footprint.
            for(var c:ArenaLayout.FLOOR){var claim=CivicAccess.claim(level,ArenaStructure.position(p.at.pos(),p.front,c.x(),0,c.z()));if(claim!=null&&!CivicData.get(e.getServer()).canUse(claim,p.owner)){invalid=true;break;}}
            if(invalid){ArenaService.finish(d,p,level,-1,"Match interrupted: stakes refunded");continue;}
            if(p.phase==ArenaData.LOBBY&&p.deadline>0&&level.getGameTime()>=p.deadline){ArenaService.finish(d,p,level,-1,"Preparation expired: stakes refunded");continue;}
            if(p.ready[0]||p.ready[1])ArenaStructure.gates(level,p,p.phase==ArenaData.LIVE);
            if(p.phase==ArenaData.COUNTDOWN){
                long left=p.deadline-level.getGameTime();
                if(left<=0){p.phase=ArenaData.LIVE;p.deadline=level.getGameTime()+6000;p.changed();d.setDirty();ArenaStructure.gates(level,p,true);}
                for(var id:p.fighters){var f=e.getServer().getPlayerList().getPlayer(id);if(f!=null)f.displayClientMessage(net.minecraft.network.chat.Component.literal(left>0?"Fight begins in "+((left+19)/20):"Fight! Leave your prep room"),true);}
            }else if(p.phase==ArenaData.LIVE){
                if(level.getGameTime()>=p.deadline){ArenaService.finish(d,p,level,-1,"Time limit: draw, stakes refunded");continue;}
                // Safe prep rooms are staging only; stalling there aborts after ten seconds.
                if(p.deadline-level.getGameTime()<5800)for(var id:p.fighters){var f=e.getServer().getPlayerList().getPlayer(id);if(f!=null&&!ArenaStructure.inside(p,f.position(),true)){ArenaService.finish(d,p,level,-1,"Fighter did not enter the pit: stakes refunded");break;}}
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void gate(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel() instanceof ServerLevel level)||!level.getBlockState(e.getPos()).is(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE))return;
        for(var pit:ArenaData.get(level.getServer()).pits.values())if(pit.at.dimension().equals(level.dimension().location().toString())&&(pit.ready[0]||pit.ready[1]))
            for(var piece:ArenaLayout.GATES)if(ArenaStructure.position(pit.at.pos(),pit.front,piece.x(),piece.y(),piece.z()).equals(e.getPos())){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);return;}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer player){var d=ArenaData.get(player.server);var p=ArenaService.enrolled(d,player.getUUID());if(p!=null)ArenaService.finish(d,p,ArenaService.level(player.server,p),-1,"Fighter disconnected: stakes refunded");}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){var d=ArenaData.get(p.server);ArenaService.recover(d,p);if(d.credits.containsKey(p.getUUID()))p.displayClientMessage(net.minecraft.network.chat.Component.literal("Arena winnings/refunds await collection at any Gladiator Pit or /arena collect"),false);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingIncomingDamageEvent e){
        if(!(e.getEntity().level() instanceof ServerLevel level))return;
        var d=ArenaData.get(level.getServer());var target=e.getEntity() instanceof Player player?protectedPit(d,player):null;
        var source=e.getSource();var attacker=source.getEntity() instanceof Player player?protectedPit(d,player):null;
        if(target==null&&attacker==null)return;
        boolean valid=target!=null&&attacker==target&&target.phase==ArenaData.LIVE
                &&source.getEntity() instanceof Player player&&e.getEntity() instanceof Player victim
                &&ArenaService.opposed(d,player,victim)&&!source.is(DamageTypeTags.IS_EXPLOSION)
                &&(source.getDirectEntity() instanceof Player||source.getDirectEntity() instanceof Projectile||source.getDirectEntity() instanceof PatersonBullet);
        if(!valid)e.setCanceled(true);
    }
    private static ArenaData.Pit protectedPit(ArenaData d,Player player){var p=ArenaService.enrolled(d,player.getUUID());return p!=null&&p.ready[p.side(player.getUUID())]&&player.level().dimension().location().toString().equals(p.at.dimension())&&ArenaStructure.inside(p,player.position(),false)?p:null;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void knockout(LivingDamageEvent.Pre e){
        if(!(e.getEntity() instanceof ServerPlayer victim))return;
        var d=ArenaData.get(victim.server);var p=protectedPit(d,victim);if(p==null||p.phase!=ArenaData.LIVE
                ||!(e.getSource().getEntity() instanceof Player attacker)||!ArenaService.opposed(d,attacker,victim))return;
        if(e.getNewDamage()-victim.getAbsorptionAmount()>=victim.getHealth()){
            e.setNewDamage(0);ArenaService.finish(d,p,victim.serverLevel(),1-p.side(victim.getUUID()),"Knockout! "+(1-p.side(victim.getUUID())==0?"Fighter A":"Fighter B")+" wins");
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer victim){var d=ArenaData.get(victim.server);var p=protectedPit(d,victim);if(p!=null){e.setCanceled(true);victim.setHealth(1);ArenaService.finish(d,p,victim.serverLevel(),-1,"Unexpected lethal event: stakes refunded");}}
    }
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("arena").then(net.minecraft.commands.Commands.literal("collect").executes(c->{var p=c.getSource().getPlayerOrException();boolean moved=ArenaService.collect(ArenaData.get(p.server),p);c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(moved?"Collected arena items":"No room or no arena items owed"),false);return moved?1:0;}))
                .then(net.minecraft.commands.Commands.literal("leave").executes(c->{var player=c.getSource().getPlayerOrException();var d=ArenaData.get(player.server);var pit=ArenaService.enrolled(d,player.getUUID());if(pit==null)return 0;ArenaService.finish(d,pit,ArenaService.level(player.server,pit),-1,"Fighter withdrew: stakes refunded");return 1;})));
    }
    private ArenaEvents() {}
}

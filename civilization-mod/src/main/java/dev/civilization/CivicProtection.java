package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "civilization")
public final class CivicProtection {
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (level.getBlockState(e.getPos()).getBlock() instanceof CivicBlock block) {
            e.setCanceled(true); e.setCancellationResult(InteractionResult.SUCCESS);
            if (e.getEntity() instanceof ServerPlayer player && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
                CivicMenu.open(player, CivicService.address(level, e.getPos()), block.land);
        } else if (!CivicAccess.allowed(level, e.getPos(), e.getEntity())) {
            e.setCanceled(true); e.setCancellationResult(InteractionResult.FAIL);
            e.getEntity().displayClientMessage(Component.literal("This land is protected."), true);
        }
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !CivicAccess.allowed(level, e.getPos(), e.getPlayer())) e.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void placing(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        var player = e.getEntity() instanceof Player p ? p : null;
        if (!CivicAccess.allowed(level, e.getPos(), player)) e.setCanceled(true);
        if (e instanceof BlockEvent.EntityMultiPlaceEvent multiple)
            for (var snapshot : multiple.getReplacedBlockSnapshots()) if (!CivicAccess.allowed(level, snapshot.getPos(), player)) e.setCanceled(true);
    }
    @SubscribeEvent public static void tool(BlockEvent.BlockToolModificationEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !CivicAccess.allowed(level, e.getPos(), e.getPlayer())) e.setCanceled(true);
    }
    @SubscribeEvent public static void trample(BlockEvent.FarmlandTrampleEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !CivicAccess.allowed(level, e.getPos(), e.getEntity() instanceof Player p ? p : null)) e.setCanceled(true);
    }
    @SubscribeEvent public static void fluidBlock(BlockEvent.FluidPlaceBlockEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !CivicAccess.boundary(level, e.getLiquidPos(), e.getPos())) e.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(pos -> CivicAccess.claim(e.getLevel(), pos) != null);
        e.getAffectedEntities().removeIf(entity -> CivicAccess.claim(e.getLevel(), entity.blockPosition()) != null);
    }
    @SubscribeEvent public static void piston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        var resolver = e.getStructureHelper(); if (resolver == null || !resolver.resolve()) return;
        var direction = e.getPistonMoveType().isExtend ? e.getDirection() : e.getDirection().getOpposite();
        if (!CivicAccess.boundary(level, e.getPos(), e.getFaceOffsetPos())) { e.setCanceled(true); return; }
        for (var pos : resolver.getToPush()) if (!CivicAccess.boundary(level, e.getPos(), pos) || !CivicAccess.boundary(level, pos, pos.relative(direction))) e.setCanceled(true);
        for (var pos : resolver.getToDestroy()) if (!CivicAccess.boundary(level, e.getPos(), pos)) e.setCanceled(true);
    }
    @SubscribeEvent public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent e) {
        if (!CivicAccess.allowed(e.getEntity().level(), e.getTarget().blockPosition(), e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent public static void interactEntity(PlayerInteractEvent.EntityInteract e) {
        if (!CivicAccess.allowed(e.getLevel(), e.getTarget().blockPosition(), e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent public static void preciseEntity(PlayerInteractEvent.EntityInteractSpecific e) {
        if (!CivicAccess.allowed(e.getLevel(), e.getTarget().blockPosition(), e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent public static void projectile(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent e) {
        if (e.getSource().getEntity() instanceof Player player && !CivicAccess.allowed(e.getEntity().level(), e.getEntity().blockPosition(), player)) e.setInvulnerable(true);
    }
    @SubscribeEvent public static void grief(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent e) {
        // Include adjacent targets (doors, crops and blocks reached by a mob standing just outside).
        var pos = e.getEntity().blockPosition(); var level = e.getEntity().level();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (CivicAccess.claim(level, pos.offset(x, 0, z)) != null) { e.setCanGrief(false); return; }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        var data = CivicData.get(e.getServer()); long now = System.currentTimeMillis();
        for (var c : data.claims.values()) if (c.energy > 0 || c.buyer != null) data.settle(c, now);
    }
}

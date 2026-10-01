package dev.civilization;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class CalorieSystem {
    private final List<Action> actions = new ArrayList<>();
    private final Map<UUID,SleepSession> sleeping = new HashMap<>();

    CalorieSystem() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::broken);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::placed);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::clonePlayer);
        NeoForge.EVENT_BUS.addListener(this::login);
        NeoForge.EVENT_BUS.addListener(this::respawn);
        NeoForge.EVENT_BUS.addListener(this::dimension);
        NeoForge.EVENT_BUS.addListener(this::logout);
        NeoForge.EVENT_BUS.addListener(this::woke);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::sleepFinished);
        NeoForge.EVENT_BUS.addListener(this::stopped);
        NeoForge.EVENT_BUS.addListener(this::breakSpeed);
    }

    private void broken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && CalorieFoodData.active(player)) {
            actions.add(new Action(player, player.serverLevel(), event.getPos().immutable(),
                    event.getState(), event, true, LaborCosts.breaking(event.getState()),false));
        }
    }

    private void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CalorieFoodData.active(player)) {
            double volume=Math.max(CutPlacement.takePlacedVolume(player,event.getPos()),MachineConstruction.takePlacedVolume(player,event.getPos()));
            actions.add(new Action(player, player.serverLevel(), event.getPos().immutable(),
                    event.getPlacedBlock(), event, false, volume>0?CalorieConfig.PLACE.get()*volume:LaborCosts.placing(event.getPlacedBlock(),event.getBlockSnapshot().getState()),volume>0));
        }
    }

    private void breakSpeed(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity();
        if (!player.isCreative() && !player.isSpectator() && CalorieFoodData.of(player).isDepleted()) {
            event.setNewSpeed(event.getNewSpeed() * CalorieConfig.DEPLETED_SPEED.get().floatValue());
        }
    }

    private void tick(ServerTickEvent.Post event) {
        for (Action action : actions) {
            if (action.level != event.getServer().getLevel(action.level.dimension())
                    || action.player.serverLevel() != action.level || !CalorieFoodData.active(action.player)
                    || !action.level.hasChunkAt(action.pos)) continue;
            boolean cancelled = action.mining
                    ? ((BlockEvent.BreakEvent) action.event).isCanceled()
                    : ((BlockEvent.EntityPlaceEvent) action.event).isCanceled();
            if (cancelled) continue;
            BlockState current = action.level.getBlockState(action.pos);
            boolean completed = action.mining ? !current.equals(action.state) : action.cut || current.is(action.state.getBlock());
            if (completed) {
                CalorieFoodData.of(action.player).spendLabor(action.player, action.cost, action.mining,
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(action.state.getBlock()).toString()
                        + "@" + action.pos.getX() + "," + action.pos.getY() + "," + action.pos.getZ());
                if (!action.mining && action.state.getBlock() instanceof net.minecraft.world.level.block.CropBlock) {
                    var seed = action.state.getBlock().getCloneItemStack(action.level, action.pos, action.state);
                    EnergyLog.production(action.player, "crop_plant", action.pos.toShortString(),
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(seed.getItem()).toString(),
                            1, 0, 0);
                }
            }
        }
        actions.clear();
        CutPlacement.clearPending();
        for(var player:event.getServer().getPlayerList().getPlayers()) {
            if(player.isSleeping()&&CalorieFoodData.active(player))
                sleeping.computeIfAbsent(player.getUUID(),id->new SleepSession(player.serverLevel().getDayTime()));
            else finishSleep(player);
        }
    }

    private void clonePlayer(PlayerEvent.Clone event) {
        var saved = new CompoundTag();
        event.getOriginal().getFoodData().addAdditionalSaveData(saved);
        event.getEntity().getFoodData().readAdditionalSaveData(saved);
        EnergyLog.marker(event.getEntity(), event.isWasDeath() ? "death_clone" : "player_clone");
    }

    private void syncPlayer(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CalorieFoodData.of(player).resizeForConfig(player);
            CalorieFoodData.of(player).sync(player, true);
        }
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) { syncPlayer(event); EnergyLog.marker(event.getEntity(), "login"); }
    private void respawn(PlayerEvent.PlayerRespawnEvent event) { syncPlayer(event); EnergyLog.marker(event.getEntity(), "respawn"); }
    private void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { syncPlayer(event); EnergyLog.marker(event.getEntity(), "dimension_change"); }
    private void logout(PlayerEvent.PlayerLoggedOutEvent event) { if(event.getEntity() instanceof ServerPlayer player)finishSleep(player);EnergyLog.marker(event.getEntity(), "logout"); }
    private void woke(PlayerWakeUpEvent event) { if(event.getEntity() instanceof ServerPlayer player)finishSleep(player); }
    private void sleepFinished(SleepFinishedTimeEvent event) {
        var level=(ServerLevel)event.getLevel();
        for(var player:level.players())if(player.isSleeping())chargeSleep(player,event.getNewTime());
    }
    private void finishSleep(ServerPlayer player) {
        if(sleeping.containsKey(player.getUUID())){
            chargeSleep(player,player.serverLevel().getDayTime());
            sleeping.remove(player.getUUID());
        }
    }
    private void chargeSleep(ServerPlayer player,long dayTime) {
        var session=sleeping.get(player.getUUID());if(session==null)return;
        long elapsed=Math.max(0,dayTime-session.dayTime);
        if(elapsed>0)CalorieFoodData.of(player).spendOther(player,LaborCosts.sleeping(elapsed),"sleep","in_game_hours");
        session.dayTime=dayTime;
    }
    private void stopped(ServerStoppedEvent event) { actions.clear();sleeping.clear(); }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("civilization")
                .then(Commands.literal("calories").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var data = CalorieFoodData.of(player);
                    context.getSource().sendSuccess(() -> Component.literal(data.isDepleted()
                            ? "Depleted: eat back to " + (int) data.recoveryThreshold() + " kcal. Empty hands + crouch-right-click natural ground to forage."
                            : "Energy state: normal."), false);
                    context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                            "Calories: %.1f / %.0f kcal | Sprint minimum: %.0f kcal\nLabor: %d broken, %d placed; %.1f kcal\nTravel: %.2f walking, %.2f sprinting, %.2f rowing blocks; %.2f kcal\nOther/healing/cold: %.1f kcal | Food absorbed: %.1f kcal\nWalk: %.2f | Sprint: %.2f | Row: %.2f kcal/block",
                            data.reserve().calories(), data.reserve().capacity(), CalorieConfig.SPRINT_MINIMUM.get(),
                            data.broken, data.placed, data.laborSpent, data.walkDistance, data.sprintDistance, data.rowDistance,
                            data.travelSpent, data.otherSpent, data.eaten, CalorieConfig.WALK.get(),
                            CalorieConfig.WALK.get() * CalorieConfig.SPRINT_MULTIPLIER.get(), CalorieConfig.ROW.get())), false);
                    return 1;
                }).then(Commands.literal("reset").executes(context -> {
                    CalorieFoodData.of(context.getSource().getPlayerOrException()).resetCounters();
                    EnergyLog.marker(context.getSource().getPlayerOrException(), "measurement_reset");
                    context.getSource().sendSuccess(() -> Component.literal("Measurement counters reset. Calories unchanged."), false);
                    return 1;
                })).then(Commands.literal("set").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("kcal", DoubleArgumentType.doubleArg(0, 100000)).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var data = CalorieFoodData.of(player);
                            double before = data.reserve().calories();
                            data.reserve().set(DoubleArgumentType.getDouble(context, "kcal"));
                            EnergyLog.record(player, "admin_set", context.getSource().getTextName(),
                                    DoubleArgumentType.getDouble(context, "kcal") - before,
                                    before, data.reserve().calories(), 1, false);
                            data.sync(player, true);
                            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                                    "Test reserve set to %.1f kcal.", data.reserve().calories())), false);
                            return 1;
                        })))));
    }

    private record Action(ServerPlayer player, ServerLevel level, BlockPos pos,
                          BlockState state, BlockEvent event, boolean mining, double cost, boolean cut) {}
    private static final class SleepSession {
        private long dayTime;
        private SleepSession(long dayTime){this.dayTime=dayTime;}
    }
}

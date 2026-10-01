package dev.civilization;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EnergyLog {
    private static final Gson JSON = new Gson();
    private static EnergyLog active;
    private final Map<UUID, Entry> pending = new HashMap<>();
    private EnergyJournal journal;
    private String run;
    private long sequence;

    public EnergyLog() {
        NeoForge.EVENT_BUS.addListener(this::start);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stop);
        NeoForge.EVENT_BUS.addListener(this::stopping);
    }

    private void start(ServerStartingEvent event) {
        run = UUID.randomUUID().toString();
        sequence = 0;
        var path = event.getServer().getWorldPath(LevelResource.ROOT).resolve("civilization-energy");
        journal = new EnergyJournal(path, 16L * 1024 * 1024, 8, 8192);
        active = this;
        LogUtils.getLogger().info("Per-player energy audit enabled: {} (16 MiB files, 8 backups)", path.toAbsolutePath());
    }

    private void tick(ServerTickEvent.Post event) {
        if (journal == null) return;
        // At most one second of like-kind movement/effect activity per entry.
        long now = System.nanoTime();
        pending.entrySet().removeIf(item -> {
            if (now - item.getValue().startedNanos >= 1_000_000_000L) { emit(item.getValue()); return true; }
            return false;
        });
    }

    private void stop(ServerStoppedEvent event) {
        if (journal == null) return;
        pending.values().forEach(this::emit);
        pending.clear();
        active = null;
        journal.close();
        journal = null;
    }

    private void stopping(ServerStoppingEvent event) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> marker(player, "server_stop"));
    }

    public static void record(Player player, String action, String detail, double requestedDelta,
                              double before, double after, double quantity, boolean aggregate) {
        if (!(player instanceof ServerPlayer serverPlayer) || active == null) return;
        active.accept(serverPlayer, action, detail, requestedDelta, before, after, quantity, aggregate, null);
    }

    public record Production(String input_resource, double input_units, String output_resource,
                             double output_units, double potential_food_kcal) {}

    public static void production(Player player, String action, String detail, String resource,
                                  double input, double output, double potentialKcal) {
        production(player, action, detail, new Production(input > 0 ? resource : null, input,
                output > 0 ? resource : null, output, potentialKcal));
    }

    public static void production(Player player, String action, String detail, Production production) {
        if (!(player instanceof ServerPlayer serverPlayer) || active == null) return;
        double balance = CalorieFoodData.of(player).reserve().calories();
        active.accept(serverPlayer, action, detail, 0, balance, balance, 1, false, production);
    }

    public static void marker(Player player, String action) {
        double balance = CalorieFoodData.of(player).reserve().calories();
        record(player, action, "", 0, balance, balance, 1, false);
    }

    /** Machine events have no body calorie balance or invented online player attribution. */
    public static void machine(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                               String action, String input, int consumed, String output, int produced) {
        if (active == null || level.isClientSide) return;
        var entry = new java.util.LinkedHashMap<String, Object>();
        entry.put("schema", 1); entry.put("record_type", "machine");
        entry.put("server_run", active.run); entry.put("sequence", ++active.sequence);
        entry.put("utc", Instant.now().toString()); entry.put("tick", level.getServer().getTickCount());
        entry.put("action", action); entry.put("dimension", level.dimension().location().toString());
        entry.put("machine", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        entry.put("x", pos.getX()); entry.put("y", pos.getY()); entry.put("z", pos.getZ());
        entry.put("production", new Production(input, consumed, output, produced, 0));
        active.journal.append(JSON.toJson(entry));
    }

    private void accept(ServerPlayer player, String action, String detail, double requested,
                        double before, double after, double quantity, boolean aggregate, Production production) {
        UUID id = player.getUUID();
        Entry old = pending.get(id);
        String dimension = player.level().dimension().location().toString();
        if (old != null && aggregate && old.action.equals(action) && old.detail.equals(detail)
                && old.dimension.equals(dimension) && Math.abs(old.after_kcal - before) < 1e-7
                && System.nanoTime() - old.startedNanos < 1_000_000_000L) {
            old.end_utc = Instant.now().toString();
            old.end_tick = player.server.getTickCount();
            old.after_kcal = after; old.delta_kcal += after - before;
            old.requested_delta_kcal += requested; old.quantity += quantity; old.count++;
            old.x = player.getX(); old.y = player.getY(); old.z = player.getZ();
            return;
        }
        if (old != null) { pending.remove(id); emit(old); }
        Entry next = new Entry();
        next.server_run = run; next.player_uuid = id.toString(); next.player_name = player.getGameProfile().getName();
        next.start_utc = next.end_utc = Instant.now().toString();
        next.start_tick = next.end_tick = player.server.getTickCount();
        next.action = action; next.detail = detail; next.dimension = dimension;
        next.x = player.getX(); next.y = player.getY(); next.z = player.getZ();
        next.before_kcal = before; next.after_kcal = after; next.delta_kcal = after - before;
        next.requested_delta_kcal = requested; next.quantity = quantity;
        next.production = production;
        if (aggregate) pending.put(id, next); else emit(next);
    }

    private void emit(Entry entry) {
        entry.sequence = ++sequence;
        journal.append(JSON.toJson(entry));
    }

    private static final class Entry {
        int schema = 1;
        String server_run, player_uuid, player_name, start_utc, end_utc, action, detail, dimension;
        long sequence, start_tick, end_tick, count = 1;
        double x, y, z, before_kcal, after_kcal, delta_kcal, requested_delta_kcal, quantity;
        Production production;
        transient long startedNanos = System.nanoTime();
    }
}

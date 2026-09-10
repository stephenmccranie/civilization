package dev.civilization;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One deliberate hand-foraging action per click; no terrain scans or persistent per-block state. */
public final class Foraging {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "forage_ground"));
    private final Map<UUID, Attempt> attempts = new HashMap<>();

    public Foraging() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::interact);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::logout);
        NeoForge.EVENT_BUS.addListener(this::stop);
    }

    private void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!canForage(player, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (attempts.containsKey(player.getUUID())) return;
        attempts.put(player.getUUID(), new Attempt(player, event.getPos().immutable(), player.position(),
                player.level().dimension().location(), CalorieConfig.FORAGE_TICKS.get()));
        EnergyLog.marker(player, "forage_start");
        player.displayClientMessage(Component.translatable("message.civilization.forage_start",
                (CalorieConfig.FORAGE_TICKS.get() + 19) / 20), true);
    }

    public static boolean canForage(ServerPlayer player, BlockPos pos) {
        return CalorieFoodData.active(player) && player.mayBuild() && player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                && !player.isPassenger() && !player.getAbilities().flying
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 20.25
                && player.serverLevel().hasChunkAt(pos) && player.serverLevel().mayInteract(player, pos)
                && player.level().getBlockState(pos).is(GROUND);
    }

    private void tick(ServerTickEvent.Post event) {
        attempts.values().removeIf(attempt -> {
            ServerPlayer player = attempt.player;
            if (player.server != event.getServer() || !player.level().dimension().location().equals(attempt.dimension)
                    || player.position().distanceToSqr(attempt.origin) > 0.16 || !canForage(player, attempt.pos)) {
                player.displayClientMessage(Component.translatable("message.civilization.forage_cancelled"), true);
                EnergyLog.marker(player, "forage_cancelled");
                return true;
            }
            attempt.elapsed++;
            if (attempt.elapsed >= attempt.duration) {
                var morsel = RecoveryItems.MORSEL.toStack();
                if (!player.addItem(morsel)) player.drop(morsel, false);
                EnergyLog.record(player, "forage_complete", "civilization:foraged_morsel@" + attempt.pos.toShortString(),
                        0, CalorieFoodData.of(player).reserve().calories(), CalorieFoodData.of(player).reserve().calories(), 1, false);
                player.displayClientMessage(Component.translatable("message.civilization.forage_complete"), true);
                return true;
            }
            if (attempt.elapsed % 20 == 0) player.displayClientMessage(Component.translatable(
                    "message.civilization.forage_progress", attempt.elapsed * 100 / attempt.duration), true);
            return false;
        });
    }

    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (attempts.remove(event.getEntity().getUUID()) != null) EnergyLog.marker(event.getEntity(), "forage_cancelled");
    }
    private void stop(ServerStoppedEvent event) { attempts.clear(); }

    private static final class Attempt {
        final ServerPlayer player;
        final BlockPos pos;
        final Vec3 origin;
        final ResourceLocation dimension;
        final int duration;
        int elapsed;
        Attempt(ServerPlayer player, BlockPos pos, Vec3 origin, ResourceLocation dimension, int duration) {
            this.player = player; this.pos = pos; this.origin = origin; this.dimension = dimension; this.duration = duration;
        }
    }
}

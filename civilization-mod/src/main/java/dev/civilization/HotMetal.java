package dev.civilization;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Heat and hammer. A Foundry pass that keeps its material heats a whole load at once, and the load stays hot until a
 * game time a minute later, so it cools on time wherever it is. Holding use on an anvil with hot metal in hand strikes
 * the whole stack; every strike is the player's labor for each unit struck, and twenty strikes refine every unit one
 * random step and leave the stack cold.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID)
public final class HotMetal {
    public static final int LOAD = 8, COOL_TICKS = 60 * 20, STRIKES = 20;
    /** Holding use repeats a strike every four ticks; strikes sent faster are ignored. */
    private static final int STRIKE_TICKS = 3;
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Civilization.MOD_ID);
    /** The game time a stack's heat lasts until. Every dimension shares game time, and it passes only while the world runs. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HOT = COMPONENTS.registerComponentType("hot_until",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** A player's last strike and the labor paid since their last finished hammering, in strikes on one unit. */
    private record Hammering(long last, int work) {}
    private static final Map<UUID, Hammering> HAMMERING = new HashMap<>();

    private HotMetal() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }

    /** Ticks until the stack cools, zero once it has. */
    public static long heatLeft(ItemStack stack, long now) {
        Long until = stack.get(HOT.get());
        return until == null ? 0 : Math.max(0, until - now);
    }
    public static boolean hot(ItemStack stack, long now) { return heatLeft(stack, now) > 0; }
    /** Gives the stack a full cooling time from `now`. */
    public static ItemStack heated(ItemStack stack, long now) {
        stack.set(HOT.get(), now + COOL_TICKS);
        return stack;
    }
    /** Drops spent heat, so cooled metal stacks with cold metal again. */
    static void cool(ItemStack stack, long now) {
        if (stack.has(HOT.get()) && !hot(stack, now)) stack.remove(HOT.get());
    }
    /** Up to a load's worth more of the furnace's input joins `produced`, its first unit, as far as the output bank has room. */
    static void load(Container furnace, int input, int[] output, ItemStack produced) {
        var rest = furnace.getItem(input);
        int more = Math.min(LOAD - 1, rest.getCount());
        while (more > 0 && !MachineInventory.room(furnace, output, produced.copyWithCount(1 + more))) more--;
        Grade.join(produced, rest, more);
    }

    @SubscribeEvent public static void strike(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (event.getHand() != InteractionHand.MAIN_HAND || !hot(event.getItemStack(), level.getGameTime())
                || !level.getBlockState(event.getPos()).is(BlockTags.ANVIL)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) strike(player, event.getPos(), event.getItemStack());
    }
    /**
     * One strike on the hot stack in hand, paid for each of its units. Paid labor counts on across pauses and toward
     * whatever hot metal is struck next, and the hammering is done once it covers twenty strikes on every unit in hand.
     */
    static void strike(ServerPlayer player, BlockPos anvil, ItemStack stack) {
        var level = player.serverLevel();
        long now = level.getGameTime();
        var last = HAMMERING.get(player.getUUID());
        if (last != null && now - last.last() < STRIKE_TICKS) return;
        int units = stack.getCount(), work = (last == null ? 0 : last.work()) + units;
        CalorieFoodData.of(player).spendOther(player, units * CalorieConfig.STRIKE.get(), "anvil_strike", anvil.toShortString());
        level.playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, .25f, 1.1f + level.random.nextFloat() * .3f);
        level.sendParticles(ParticleTypes.SMALL_FLAME, anvil.getX() + .5, anvil.getY() + 1.05, anvil.getZ() + .5, 4, .15, .02, .15, .04);
        if (work < STRIKES * units) {
            HAMMERING.put(player.getUUID(), new Hammering(now, work));
            player.displayClientMessage(Component.translatable("message.civilization.hammering", stack.getHoverName(), work / units, STRIKES), true);
            return;
        }
        HAMMERING.put(player.getUUID(), new Hammering(now, 0));
        int before = Grade.value(stack);
        stack.remove(HOT.get());
        Grade.refine(stack, level.random);
        level.playSound(null, anvil, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .3f, 1.4f);
        player.displayClientMessage(Component.translatable("message.civilization.hammered", stack.getHoverName(),
                Grade.percent(before), Grade.percent(Grade.value(stack))), true);
    }

    /** Once a second, spent heat leaves what a player carries and has open. */
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        long now = player.level().getGameTime();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) cool(inventory.getItem(i), now);
        for (var slot : player.containerMenu.slots) cool(slot.getItem(), now);
        cool(player.containerMenu.getCarried(), now);
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) { HAMMERING.remove(event.getEntity().getUUID()); }
}

package dev.civilization;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Smelt, hammer and quench. Metal leaves the Foundry hot until a game time a minute later, so it cools on time
 * wherever it is, and a hot furnace keeps the metal in it hot. Holding use on an anvil with glowing metal in hand
 * strikes the whole stack with a heavy blow a second, each the player's labor for every unit struck; the hammering's
 * last blow beats rough metal into its ingot, raises every unit one random grade step and leaves the stack too cool
 * to work again. A grindstone cuts diamonds the same way, cold. Water takes all of a stack's heat at once.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID)
public final class HotMetal {
    /** Metal stays hot a minute; it glows hot enough to work for the first forty seconds. */
    public static final int LOAD = 8, COOL_TICKS = 60 * 20, DULL_TICKS = 20 * 20;
    public static final int ANVIL_BLOWS = 5, STONE_ANVIL_BLOWS = 8, GRINDSTONE_STROKES = 5;
    /** A blow takes a second to swing again, shown as the stack's cooldown; uses sent sooner are ignored. */
    private static final int STRIKE_TICKS = 20;
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Civilization.MOD_ID);
    /** The game time a stack's heat lasts until. Every dimension shares game time, and it passes only while the world runs. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HOT = COMPONENTS.registerComponentType("hot_until",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** Labor each player has paid since their last finished hammering or cutting, in blows on one unit. */
    private static final Map<UUID, Integer> WORK = new HashMap<>();

    private HotMetal() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }

    /** Ticks until the stack cools, zero once it has. */
    public static long heatLeft(ItemStack stack, long now) {
        Long until = stack.get(HOT.get());
        return until == null ? 0 : Math.max(0, until - now);
    }
    public static boolean hot(ItemStack stack, long now) { return heatLeft(stack, now) > 0; }
    public static boolean glowing(ItemStack stack, long now) { return heatLeft(stack, now) > DULL_TICKS; }
    /** Heat is counted from whole seconds, so metal heated within the same second stacks. */
    private static long second(long now) { return now - Math.floorMod(now, 20); }
    /** Gives the stack a full heat from `now`. */
    public static ItemStack heated(ItemStack stack, long now) {
        stack.set(HOT.get(), second(now) + COOL_TICKS);
        return stack;
    }
    /** Drops spent heat, so cooled metal stacks with cold metal again. */
    static void cool(ItemStack stack, long now) {
        if (stack.has(HOT.get()) && !hot(stack, now)) stack.remove(HOT.get());
    }
    /** Once a second for a furnace's output: hot metal in a hot furnace keeps a full heat, elsewhere spent heat goes. */
    static void tend(ItemStack stack, boolean inFire, long now) {
        if (inFire && stack.has(HOT.get())) heated(stack, now);
        else cool(stack, now);
    }
    /** Up to a load's worth more of the furnace's input joins `produced`, its first unit, as far as the output bank has room. */
    static void load(Container furnace, int input, int[] output, ItemStack produced) {
        var rest = furnace.getItem(input);
        int more = Math.min(LOAD - 1, rest.getCount());
        while (more > 0 && !MachineInventory.room(furnace, output, produced.copyWithCount(1 + more))) more--;
        Grade.join(produced, rest, more);
    }

    /** Rough metal and the ingots it is beaten into. */
    static boolean forgeable(Item item) {
        return item instanceof RoughMetal || KilnContent.ROUGH_METALS.stream().anyMatch(rough -> rough.get().ingot() == item);
    }
    /** Blows a hammering takes on this block, zero if it isn't an anvil. */
    private static int anvil(BlockState block) {
        return block.is(BlockTags.ANVIL) ? ANVIL_BLOWS : block.is(KilnContent.STONE_ANVIL.get()) ? STONE_ANVIL_BLOWS : 0;
    }

    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var stack = event.getItemStack();
        var block = level.getBlockState(event.getPos());
        long now = level.getGameTime();
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        boolean metal = forgeable(stack.getItem());
        int anvil = metal ? anvil(block) : 0;
        int blows = anvil > 0 && glowing(stack, now) ? anvil : stack.is(Items.DIAMOND) && block.is(Blocks.GRINDSTONE) ? GRINDSTONE_STROKES : 0;
        Vec3 water = hot(stack, now) ? block.is(Blocks.WATER_CAULDRON) ? Vec3.atCenterOf(event.getPos()) : water(event.getEntity()) : null;
        if (blows == 0 && anvil == 0 && water == null) return;
        event.setCanceled(true);
        // The hand swings with each landed blow, not with every repeat of held use.
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blows > 0) strike(player, event.getPos(), stack, blows);
        else if (water != null) quench(player, stack, water);
        else player.displayClientMessage(Component.translatable("message.civilization.too_cool", stack.getHoverName()), true);
    }
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem event) {
        var water = hot(event.getItemStack(), event.getLevel().getGameTime()) ? water(event.getEntity()) : null;
        if (water == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getEntity() instanceof ServerPlayer player) quench(player, event.getItemStack(), water);
    }
    /** Where the player's reach first meets water, if it does. */
    private static Vec3 water(Player player) {
        var eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(player.blockInteractionRange())),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.BLOCK && player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER) ? hit.getLocation() : null;
    }
    private static void quench(ServerPlayer player, ItemStack stack, Vec3 water) {
        stack.remove(HOT.get());
        player.swing(InteractionHand.MAIN_HAND, true);
        hiss(player.serverLevel(), water);
    }
    private static void hiss(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .6f, 1.2f);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + .1, at.z, 8, .15, .05, .15, .02);
    }
    @SubscribeEvent public static void drop(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !item.isInWater() || !(item.level() instanceof ServerLevel level)
                || !hot(item.getItem(), level.getGameTime())) return;
        var cooled = item.getItem().copy();
        cooled.remove(HOT.get());
        item.setItem(cooled);
        hiss(level, item.position());
    }

    /**
     * One blow or stroke on the stack in hand, paid for each of its units. Paid labor counts on across pauses and
     * toward whatever is worked next, and the work is done once it covers every blow on every unit in hand.
     */
    static void strike(ServerPlayer player, BlockPos station, ItemStack stack, int blows) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
        var level = player.serverLevel();
        boolean metal = stack.has(HOT.get());
        int units = stack.getCount(), work = WORK.getOrDefault(player.getUUID(), 0) + units;
        player.getCooldowns().addCooldown(stack.getItem(), STRIKE_TICKS);
        player.swing(InteractionHand.MAIN_HAND, true);
        CalorieFoodData.of(player).spendOther(player, units * CalorieConfig.BLOW.get(), metal ? "anvil_strike" : "grindstone_cut", station.toShortString());
        double x = station.getX() + .5, y = station.getY() + 1.05, z = station.getZ() + .5;
        if (metal) {
            level.playSound(null, station, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, .7f, .75f + level.random.nextFloat() * .15f);
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 16, .2, .02, .2, .12);
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 2, .1, 0, .1, 0);
        } else {
            level.playSound(null, station, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .7f, .9f + level.random.nextFloat() * .2f);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), x, y, z, 8, .2, .05, .2, .05);
        }
        if (work < blows * units) {
            WORK.put(player.getUUID(), work);
            player.displayClientMessage(Component.translatable(metal ? "message.civilization.hammering" : "message.civilization.cutting",
                    stack.getHoverName(), work / units, blows), true);
            return;
        }
        WORK.remove(player.getUUID());
        int before = Grade.value(stack);
        var done = stack.getItem() instanceof RoughMetal rough ? stack.transmuteCopy(rough.ingot(), units) : stack.copy();
        if (metal) done.set(HOT.get(), Math.min(stack.get(HOT.get()), second(level.getGameTime()) + DULL_TICKS));
        Grade.refine(done, level.random);
        player.setItemInHand(InteractionHand.MAIN_HAND, done);
        player.displayClientMessage(Component.translatable(metal ? "message.civilization.hammered" : "message.civilization.cut",
                done.getHoverName(), Grade.percent(before), Grade.percent(Grade.value(done))), true);
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
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) { WORK.remove(event.getEntity().getUUID()); }
}

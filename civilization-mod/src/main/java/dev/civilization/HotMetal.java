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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Unit;
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
 * Smelt, hammer and quench. A stack's temperature is its age out of the fire, counted from the game time it goes
 * cold a minute after leaving the Foundry, so it cools on time wherever it is; a hot furnace keeps the metal in it at
 * full heat. Everything that shows hot metal takes its colour from that age. Holding use on an anvil with metal at
 * working heat in hand strikes the whole stack with a heavy blow a second, each the player's labor for every unit
 * struck; the hammering's last blow beats rough metal into its ingot and raises every unit one random grade step, and
 * the stack cools on as before but cannot be worked again until it has been back in the fire. A grindstone cuts diamonds the same way, cold. Taking the heat out right at
 * the ideal age rewards hammered metal with another hammering's step, a little off it neither helps nor harms, and
 * further off costs every unit grade along a bell curve: water takes the heat at once, and metal left to cool in air
 * takes the worst cost once it is next seen cold.
 */
@EventBusSubscriber(modid = Civilization.MOD_ID)
public final class HotMetal {
    /** Metal is cold a minute out of the fire. It can be worked for its first 25 s, and quenches best at 35 s, bright cherry red. */
    public static final int LOAD = 8, COOL_TICKS = 60 * 20, WORKING_TICKS = 25 * 20, IDEAL_TICKS = 35 * 20;
    /**
     * A quench of hammered metal within `MASTERY` ticks (1 s) of the ideal rewards each unit with up to one mean hammering
     * step; within `FORGIVEN` ticks (3 s) it costs nothing; beyond, each unit loses up to `WORST` tenths of a point along
     * a bell curve `SPREAD` ticks (5 s) wide from there.
     */
    static final int MASTERY = 20, FORGIVEN = 3 * 20, WORST = 100, SPREAD = 5 * 20;
    /** The glow by age in seconds: white-yellow, orange, bright cherry red, dull red, faint, then none; alpha is its strength. */
    private static final int[] GLOW_AGES = {0, 12, 25, 35, 45, 52, 56};
    private static final int[] GLOW = {0xFFFFF8E0, 0xFFFFAE3C, 0xFFFF6A18, 0xFFE81E1E, 0xB4901410, 0x50600C08, 0x00400806};
    public static final int ANVIL_BLOWS = 5, STONE_ANVIL_BLOWS = 8, GRINDSTONE_STROKES = 5;
    /** A blow takes a second to swing again, shown as the stack's cooldown; uses sent sooner are ignored. */
    private static final int STRIKE_TICKS = 20;
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Civilization.MOD_ID);
    /** The game time a stack's heat lasts until. Every dimension shares game time, and it passes only while the world runs. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HOT = COMPONENTS.registerComponentType("hot_until",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** Marks metal hammered this heat, which cannot be hammered again and whose quench can earn the mastery reward; a quench takes the mark with the heat. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> WORKED = COMPONENTS.registerComponentType("worked",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    /** Labor each player has paid since their last finished hammering or cutting, in blows on one unit. */
    private static final Map<UUID, Integer> WORK = new HashMap<>();
    /**
     * When each player last used an anvil with metal in hand. Held use repeats every four ticks, so a use within
     * `HELD_TICKS` of the last continues one press: it keeps striking, but never replaces a hammering's result with a refusal.
     */
    private static final Map<UUID, Long> PRESSED = new HashMap<>();
    private static final int HELD_TICKS = 8;

    private HotMetal() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }

    /** The stack's temperature, as ticks since it left the fire: `COOL_TICKS` once cold. */
    public static long age(ItemStack stack, long now) {
        Long until = stack.get(HOT.get());
        return until == null ? COOL_TICKS : Math.clamp(now - until + COOL_TICKS, 0, COOL_TICKS);
    }
    public static boolean hot(ItemStack stack, long now) { return age(stack, now) < COOL_TICKS; }
    /** Whether the stack can be hammered: at working heat and not yet hammered this heat. */
    public static boolean workable(ItemStack stack, long now) { return age(stack, now) < WORKING_TICKS && !stack.has(WORKED.get()); }
    /** Tenths of a point a quench at `age` costs every unit: none near the ideal, the worst once cold. */
    public static int loss(long age) {
        double off = Math.max(0, Math.abs(age - IDEAL_TICKS) - FORGIVEN) / (double)SPREAD;
        return (int)Math.round(WORST * (1 - Math.exp(-off * off / 2)));
    }
    /** The share of a mean hammering step quenching hammered metal at `age` adds to every unit: all of it at the ideal, none a second off. */
    public static double reward(long age) { return Math.max(0, 1 - Math.abs(age - IDEAL_TICKS) / (double)MASTERY); }
    /** The glow's colour at the stack's temperature, its alpha the glow's strength. */
    public static int glow(ItemStack stack, long now) {
        double seconds = age(stack, now) / 20.0;
        int key = 1;
        while (key < GLOW_AGES.length - 1 && seconds > GLOW_AGES[key]) key++;
        double t = Math.clamp((seconds - GLOW_AGES[key - 1]) / (GLOW_AGES[key] - GLOW_AGES[key - 1]), 0, 1);
        int colour = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int from = GLOW[key - 1] >>> shift & 0xFF, to = GLOW[key] >>> shift & 0xFF;
            colour |= (int)Math.round(from + (to - from) * t) << shift;
        }
        return colour;
    }
    /** Heat is counted from whole seconds, so metal heated within the same second stacks. */
    private static long second(long now) { return now - Math.floorMod(now, 20); }
    /** Gives the stack a full heat from `now`. */
    public static ItemStack heated(ItemStack stack, long now) {
        stack.set(HOT.get(), second(now) + COOL_TICKS);
        return stack;
    }
    /** Takes all of the stack's heat, costing every unit the loss at its temperature. The only way heat leaves metal. */
    static void quench(ItemStack stack, long now) {
        long age = age(stack, now);
        Grade.temper(stack, stack.has(WORKED.get()) ? reward(age) : 0, loss(age));
        stack.remove(HOT.get());
        stack.remove(WORKED.get());
    }
    /**
     * Settles metal that has gone cold in air, at the worst cost, so it stacks with cold metal again. A settlement that
     * is never saved comes out the same when the stack is next seen, so containers need not be marked changed.
     */
    static void cool(ItemStack stack, long now) {
        if (stack.has(HOT.get()) && !hot(stack, now)) quench(stack, now);
    }
    /** Once a second for a furnace's output: hot metal in a hot furnace keeps a full heat, elsewhere it settles once cold. */
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
    public static boolean forgeable(Item item) {
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
        int blows = anvil > 0 && workable(stack, now) ? anvil : stack.is(Items.DIAMOND) && block.is(Blocks.GRINDSTONE) ? GRINDSTONE_STROKES : 0;
        Vec3 water = hot(stack, now) ? block.is(Blocks.WATER_CAULDRON) ? Vec3.atCenterOf(event.getPos()) : water(event.getEntity()) : null;
        if (blows == 0 && anvil == 0 && water == null) return;
        event.setCanceled(true);
        // The hand swings with each landed blow, not with every repeat of held use.
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long last = anvil > 0 ? PRESSED.put(player.getUUID(), now) : null;
        if (blows > 0) strike(player, event.getPos(), stack, blows);
        else if (water != null) quench(player, stack, water);
        else if (last == null || now - last > HELD_TICKS) player.displayClientMessage(Component.translatable(stack.has(WORKED.get()) ? "message.civilization.worked" : "message.civilization.too_cool", stack.getHoverName()), true);
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
        int before = Grade.value(stack);
        long age = age(stack, player.level().getGameTime());
        quench(stack, player.level().getGameTime());
        player.swing(InteractionHand.MAIN_HAND, true);
        hiss(player.serverLevel(), water);
        player.displayClientMessage(Component.translatable("message.civilization.quenched",
                stack.getHoverName(), timing(age), Grade.percent(before), Grade.percent(Grade.value(stack))), true);
    }
    /** How a quench at `age` was timed, to a tenth of a second: at cherry red, or how far early or late. */
    private static Component timing(long age) {
        long tenths = Math.round(Math.abs(age - IDEAL_TICKS) / 2.0);
        if (tenths == 0) return Component.translatable("message.civilization.quench_ideal");
        return Component.translatable(age < IDEAL_TICKS ? "message.civilization.quench_early" : "message.civilization.quench_late", tenths / 10 + "." + tenths % 10);
    }
    private static void hiss(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .6f, 1.2f);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + .1, at.z, 8, .15, .05, .15, .02);
    }
    /** Dropped metal quenches in water and settles once cold. */
    @SubscribeEvent public static void drop(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !item.getItem().has(HOT.get()) || !(item.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        boolean hot = hot(item.getItem(), now);
        if (hot && !item.isInWater()) return;
        var settled = item.getItem().copy();
        quench(settled, now);
        item.setItem(settled);
        if (hot) hiss(level, item.position());
    }

    /**
     * One blow or stroke on the stack in hand, paid for each of its units at the smith's comfort, as bench work is; a
     * smith who cannot pay is refused. Paid labor counts on across pauses and toward whatever is worked next, and the
     * work is done once it covers every blow on every unit in hand.
     */
    static void strike(ServerPlayer player, BlockPos station, ItemStack stack, int blows) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
        var level = player.serverLevel();
        boolean metal = stack.has(HOT.get());
        int units = stack.getCount(), work = WORK.getOrDefault(player.getUUID(), 0) + units;
        var food = CalorieFoodData.of(player);
        double labor = units * CalorieConfig.BLOW.get() * ThermalRules.calorieFactor(ThermalSystem.comfort(player));
        if (CalorieFoodData.active(player) && (food.isDepleted() || !food.reserve().canAfford(labor))) {
            player.displayClientMessage(Component.translatable("message.civilization.needs_food", stack.getHoverName()), true);
            return;
        }
        player.getCooldowns().addCooldown(stack.getItem(), STRIKE_TICKS);
        player.swing(InteractionHand.MAIN_HAND, true);
        food.spendOther(player, labor, metal ? "anvil_strike" : "grindstone_cut", station.toShortString());
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
        if (metal) done.set(WORKED.get(), Unit.INSTANCE);
        Grade.refine(done, level.random);
        player.setItemInHand(InteractionHand.MAIN_HAND, done);
        player.displayClientMessage(Component.translatable(metal ? "message.civilization.hammered" : "message.civilization.cut",
                done.getHoverName(), Grade.percent(before), Grade.percent(Grade.value(done))), true);
    }

    /** Once a second, metal that has cooled in what a player carries and has open settles. */
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        long now = player.level().getGameTime();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) cool(inventory.getItem(i), now);
        for (var slot : player.containerMenu.slots) cool(slot.getItem(), now);
        cool(player.containerMenu.getCarried(), now);
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        WORK.remove(event.getEntity().getUUID());
        PRESSED.remove(event.getEntity().getUUID());
    }
}

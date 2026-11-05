package dev.civilization;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Grades of the units in a stack, in tenths of a percent (750 is 75.0%), best first: one value means every unit has it, otherwise there is
 * one value per unit. Units leave a stack from the top, so a stack hands out and spends its best units first. A
 * stack resized without saying which units moved keeps its worst ones, so no path can invent grade.
 */
public record Grade(List<Integer> units) {
    public static final int STANDARD = 750, TARGET = 1500, MAX_UNITS = 99;
    private static final Grade STANDARD_UNITS = new Grade(List.of(STANDARD));
    /** Materials whose units carry grades: smelted metal, diamonds and the Smithy's parts. */
    public static final TagKey<Item> GRADED = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "graded"));
    /** Saved as a percentage, so a whole-percent grade saved by an older build reads unchanged. */
    private static final Codec<Integer> PERCENT = Codec.DOUBLE.xmap(percent -> (int)Math.round(percent * 10), tenths -> tenths / 10.0);
    private static final Codec<Grade> CODEC = Codec.either(PERCENT, ExtraCodecs.nonEmptyList(PERCENT.sizeLimitedListOf(MAX_UNITS))).xmap(
            either -> new Grade(either.map(List::of, list -> list)),
            grade -> grade.units.size() == 1 ? Either.left(grade.units.getFirst()) : Either.right(grade.units));
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Civilization.MOD_ID);
    // The ID predates material grades; a single grade is saved as a bare number, as equipment's always was.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Grade>> GRADE = COMPONENTS.registerComponentType("equipment_grade",
            builder -> builder.persistent(CODEC).networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_UNITS)).map(Grade::new, Grade::units)));

    public Grade {
        var sorted = new ArrayList<>(units);
        sorted.sort(Comparator.reverseOrder());
        units = sorted.getFirst().equals(sorted.getLast()) ? List.of(sorted.getFirst()) : List.copyOf(sorted);
    }

    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
    public static boolean graded(ItemStack stack) { return stack.is(GRADED); }
    /** Units without a recorded grade are standard. */
    public static Grade of(ItemStack stack) { return stack.getOrDefault(GRADE.get(), STANDARD_UNITS); }
    /** The single unit's grade, or the rounded mean of the stack's units. */
    public static int value(ItemStack stack) { return mean(of(stack).units); }
    public static int mean(List<Integer> units) { return (int)Math.round(units.stream().mapToInt(Integer::intValue).average().orElseThrow()); }
    public static String percent(int grade) { return grade % 10 == 0 ? grade / 10 + "%" : String.format(java.util.Locale.ROOT, "%.1f%%", grade / 10.0); }
    /** The grades of the top `n` units, best first. */
    public static List<Integer> top(ItemStack stack, int n) { return fit(of(stack).units, stack.getCount()).subList(0, n); }
    /** The units a list describes at `count`: the worst `count` of them, the worst repeated to make up a shortfall. */
    private static List<Integer> fit(List<Integer> units, int count) {
        if (units.size() == 1) return Collections.nCopies(count, units.getFirst());
        var fitted = new ArrayList<>(units.subList(Math.max(0, units.size() - count), units.size()));
        while (fitted.size() < count) fitted.add(fitted.getLast());
        return fitted;
    }

    /** A freshly smelted unit: normally distributed about the standard with a 20-point deviation, unbounded. */
    public static int roll(RandomSource random) { return (int)Math.round(STANDARD + random.nextGaussian() * 200); }
    /**
     * A hammering always raises a unit by at least a tenth of a point. Its gain is exponentially distributed, with a
     * mean of 4.5 points at the standard that falls with the fourth power of the distance left to the target, and it
     * never carries a unit past the target; from there on each pass adds a tenth.
     */
    public static int refined(int grade, RandomSource random) {
        int gap = TARGET - grade;
        double mean = 45 * Math.pow(Math.max(0, gap) / (double)(TARGET - STANDARD), 4);
        return grade + Math.max(1, Math.min(gap, (int)Math.round(-mean * Math.log(1 - random.nextDouble()))));
    }
    /** A hammering of the whole stack: every unit takes its own step. */
    public static void refine(ItemStack stack, RandomSource random) {
        set(stack, top(stack, stack.getCount()).stream().map(unit -> refined(unit, random)).toList());
    }
    /** The unit a furnace makes from the top unit of `input`: rolled from ore, carried into another metal or through a heat. */
    public static void smelted(ItemStack product, ItemStack input, RandomSource random) {
        if (graded(product)) assign(product, graded(input) ? top(input, 1).getFirst() : roll(random));
    }
    /** Gives every unit of the stack one grade. */
    public static void assign(ItemStack stack, int grade) { put(stack, new Grade(List.of(grade))); }

    /** Whether two stacks combine: identical, or the same graded material differing only in its units' grades. */
    public static boolean stacks(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b) || !a.isEmpty() && !b.isEmpty() && graded(a) && a.is(b.getItem())
                && a.getComponentsPatch().forget(type -> type == GRADE.get()).equals(b.getComponentsPatch().forget(type -> type == GRADE.get()));
    }
    /** Moves up to `n` units from the top of `from` onto `into`, a stack it combines with, and returns how many moved. */
    public static int join(ItemStack into, ItemStack from, int n) {
        int moved = Math.min(n, from.getCount());
        if (moved <= 0) return 0;
        var units = new ArrayList<>(top(into, into.getCount()));
        units.addAll(top(from, moved));
        from.shrink(moved);
        set(into, units);
        return moved;
    }

    /** Every count change that names no units keeps the stack's units fitted to its count. */
    public static void resize(ItemStack stack, int count) {
        var grade = GRADE.isBound() ? stack.get(GRADE.get()) : null;
        if (grade == null || grade.units.size() == 1 || count <= 0 || count == grade.units.size()) return;
        put(stack, new Grade(fit(grade.units, count)));
    }
    /** ItemStack.split for a stack whose units differ: the part takes the top units. */
    public static ItemStack split(ItemStack stack, int n) {
        var grade = stack.get(GRADE.get());
        int taken = Math.min(n, stack.getCount());
        if (grade == null || grade.units.size() == 1 || taken <= 0) return null;
        var units = fit(grade.units, stack.getCount());
        var part = stack.copy();
        set(part, units.subList(0, taken));
        if (taken == units.size()) stack.setCount(0);
        else set(stack, units.subList(taken, units.size()));
        return part;
    }
    /** Cuts the stack to its top `n` units, the ones a split would hand out. */
    public static void keepTop(ItemStack stack, int n) { set(stack, top(stack, n)); }
    /** Makes `units` exactly the stack's units, its count included. */
    private static void set(ItemStack stack, List<Integer> units) {
        put(stack, new Grade(units));
        stack.setCount(units.size());
    }
    private static void put(ItemStack stack, Grade grade) {
        if (grade.equals(STANDARD_UNITS)) stack.remove(GRADE.get());
        else stack.set(GRADE.get(), grade);
    }
}

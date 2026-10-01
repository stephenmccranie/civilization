package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Food plus a bounded reserve of already-paid cooking heat, in work-tick units. */
public final class SkilletContents {
    public static final double CAPACITY = 160;
    private boolean batch;
    private double work, budget, warmth;

    public boolean batch() { return batch; }
    public double work() { return work; }
    public double activity() { return warmth / CAPACITY; }
    public double warmth() { return warmth; }
    /** Lazy cooling for carried/stored items: storage cannot freeze their paid heat. */
    public void cool(long ticks) {
        if (ticks <= 0 || warmth == 0) return;
        double released = warmth * (1 - Math.pow(1 - 1 / CAPACITY, Math.min(ticks, 3000)));
        warmth -= released;
        if (batch) work = Math.min(StoveCooking.MAX, work + released);
        if (warmth < .02) warmth = 0;
    }
    public void start(double calories) { batch = true; work = 0; budget = calories; }
    public boolean edible() { return batch && work >= StoveCooking.EDIBLE; }
    public boolean needsHeat() { return batch && work < StoveCooking.MAX; }
    public void tick(double supplied) {
        double released = warmth / CAPACITY;
        warmth = Math.clamp(warmth + supplied - released, 0, CAPACITY);
        if (batch) work = Math.min(StoveCooking.MAX, work + released);
        // Discard only an imperceptible remainder; never create cooking energy.
        if (warmth < .02) warmth = 0;
    }
    public ItemStack serve() {
        if (!edible()) return ItemStack.EMPTY;
        var meal = PrototypeStoveContent.meal(StoveCooking.quality(work), budget);
        batch = false; work = budget = 0;
        return meal;
    }
    public String observation() {
        if (!batch) return warmth > 1 ? "Empty skillet, cooling" : "Empty skillet";
        String food = work < 400 ? "Pale vegetables" : work < 800 ? "Color deepening" : work <= 1000 ? "Golden vegetables" : work < 1400 ? "Deeply browned vegetables" : "Charred vegetables";
        return food + (warmth > 80 ? ", sizzling" : warmth > 1 ? ", cooling" : ", cool");
    }
    public CompoundTag save() {
        var t = new CompoundTag(); t.putBoolean("batch", batch); t.putDouble("work", work);
        t.putDouble("budget", budget); t.putDouble("warmth", warmth); return t;
    }
    private static double finite(double value, double max) { return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0; }
    public static SkilletContents load(CompoundTag t) {
        var s = new SkilletContents(); s.batch = t.getBoolean("batch"); s.work = finite(t.getDouble("work"), StoveCooking.MAX);
        s.budget = finite(t.getDouble("budget"), 1e9); s.warmth = finite(t.getDouble("warmth"), CAPACITY); return s;
    }
}

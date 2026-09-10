package dev.civilization;

/** The sole food-energy store. Values are game kcal, with fractional precision. */
public final class CalorieReserve {
    private double capacity;
    private double calories;

    public CalorieReserve(double capacity, double calories) {
        if (!Double.isFinite(capacity) || capacity <= 0) throw new IllegalArgumentException("Invalid capacity");
        this.capacity = capacity;
        set(calories);
    }

    public double calories() { return calories; }
    public double capacity() { return capacity; }
    public boolean needsFood() { return calories < capacity; }

    public void set(double value) {
        calories = Double.isFinite(value) ? Math.clamp(value, 0, capacity) : 0;
    }

    public void resize(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid capacity");
        capacity = value;
        set(calories);
    }

    public double eat(double amount) {
        validate(amount);
        double before = calories;
        set(calories + amount);
        return calories - before;
    }

    public double spend(double amount) {
        validate(amount);
        double paid = Math.min(calories, amount);
        calories -= paid;
        return paid;
    }

    public boolean canAfford(double amount) { return calories >= amount; }

    private static void validate(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid calorie amount");
    }
}

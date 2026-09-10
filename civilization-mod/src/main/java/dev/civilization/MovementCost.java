package dev.civilization;

public final class MovementCost {
    private MovementCost() {}

    public static double distance(double dx, double dy, double dz, boolean swimming, boolean climbing) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) return 0;
        // This runs on validated movement deltas, not differences between ticks or teleport positions.
        double distance = swimming ? Math.sqrt(dx * dx + dy * dy + dz * dz)
                : Math.hypot(dx, dz) + (climbing ? Math.max(0, dy) : 0);
        return distance <= 16 ? distance : 0;
    }

    public static double calories(double distance, boolean sprinting, double walkRate, double multiplier) {
        return distance * walkRate * (sprinting ? multiplier : 1);
    }
}

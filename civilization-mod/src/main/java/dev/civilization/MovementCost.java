package dev.civilization;

public final class MovementCost {
    private MovementCost() {}

    public static double distance(double dx, double dy, double dz, boolean swimming, boolean climbing) {
        if (!valid(dx, dy, dz)) return 0;
        // This runs on validated movement deltas, not differences between ticks or teleport positions.
        double distance = swimming ? Math.sqrt(dx * dx + dy * dy + dz * dz)
                : Math.hypot(dx, dz);
        return distance <= 16 ? distance : 0;
    }

    private static boolean valid(double dx, double dy, double dz) {
        return Double.isFinite(dx) && Double.isFinite(dy) && Double.isFinite(dz)
                && dx * dx + dy * dy + dz * dz <= 256;
    }

    public static double ascent(double dx, double dy, double dz) {
        return valid(dx, dy, dz) ? Math.max(0, dy) : 0;
    }

    public static double calories(double distance, boolean sprinting, double rate, double sprintMultiplier) {
        return distance * rate * (sprinting ? sprintMultiplier : 1);
    }
}

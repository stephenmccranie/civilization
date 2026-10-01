package dev.civilization;

/** Small, testable rules for recovering from an empty reserve without calorie debt. */
public final class RecoveryRules {
    private RecoveryRules() {}

    public static boolean depleted(boolean wasDepleted, double calories, double recoveryThreshold) {
        return calories <= 0 || (wasDepleted && calories < recoveryThreshold);
    }

    public static float starvationDamage(float health, float floor) {
        return Math.max(0, Math.min(1, health - floor));
    }
}

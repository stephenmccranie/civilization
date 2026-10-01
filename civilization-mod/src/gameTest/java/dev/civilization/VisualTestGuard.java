package dev.civilization;

/** State for the opt-in visual test mixins. Never packaged in the production mod. */
public final class VisualTestGuard {
    public static boolean windowPrepared;
    public static int preventedMouseCalls;
    private VisualTestGuard() {}
}

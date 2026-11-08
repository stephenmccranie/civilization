package dev.civilization.client;

/**
 * The Hot-Bulb Engine's flywheel heard as exhaust knocks, two a turn. While the engine runs it spins up to full speed,
 * a knock every {@link #BEAT} ticks (the run animation's 0.8-second turn); once it stops it coasts down, its knocks
 * spreading apart until it rests. A start from rest begins with the kick that throws the flywheel over.
 */
final class EngineRhythm {
    enum Beat { NONE, KICK, KNOCK }
    static final int BEAT = 8;
    private static final double KICKED = .7, STOPPED = .35;
    /** The flywheel's speed as a fraction of full; 0 at rest. */
    double speed;
    private double phase;

    Beat tick(boolean running) {
        if (running && speed == 0) { speed = KICKED; phase = .3; return Beat.KICK; }
        // Running, the flywheel reaches exactly full speed 1.5 seconds after a kick; stopped, it coasts down.
        speed = running ? Math.min(1, speed + .01) : speed * .96;
        if (speed < STOPPED) { speed = 0; phase = 0; return Beat.NONE; }
        phase += speed / BEAT;
        if (phase < 1) return Beat.NONE;
        phase -= 1;
        return Beat.KNOCK;
    }
}

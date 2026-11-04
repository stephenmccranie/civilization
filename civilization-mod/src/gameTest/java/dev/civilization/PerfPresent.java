package dev.civilization;

import java.util.ArrayDeque;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

/**
 * The benchmark's stand-in for presenting a frame. Drivers throttle a hidden window's buffer swaps, and the next frame's
 * buffer uploads then wait on them, so the hidden client presents nothing and instead keeps at most {@value #IN_FLIGHT}
 * frames queued on the GPU, as a visible window's swap chain does.
 */
public final class PerfPresent {
    private static final int IN_FLIGHT = 2;
    private static final ArrayDeque<Long> fences = new ArrayDeque<>();

    private PerfPresent() {}

    public static void present() {
        GL11.glFlush();
        fences.add(GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
        while (fences.size() > IN_FLIGHT) {
            long fence = fences.removeFirst();
            while (GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000L) == GL32.GL_TIMEOUT_EXPIRED) {}
            GL32.glDeleteSync(fence);
        }
    }
}

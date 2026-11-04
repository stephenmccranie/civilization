package dev.civilization;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Times every profiler section of the client's frames on the render thread with the system clock. With GPU timing on,
 * the frame's rendering ({@value #RENDER} and its sections up to {@value #GPU_DEPTH} deep) is also timed on the GPU with
 * OpenGL timestamp queries issued at the same points in the command stream; ticking and other CPU-only work never is,
 * since each query costs the driver microseconds. Per-object renders (one block entity or entity) issue no GL commands
 * of their own, so they are timed on the CPU only, under the section that draws them. Readings are kept per frame, so
 * frame pacing and the cause of each slow frame can be read back, together with the recorder's own cost.
 */
public final class PerfRecorder implements ProfilerFiller {
    public static final PerfRecorder INSTANCE = new PerfRecorder();
    private static final String RENDER = "root/gameRenderer";
    private static final int GPU_DEPTH = 6;
    /** GPU results are read this many frames later, when the GPU has long finished them. */
    private static final int LAG = 4;
    /** A composite render type prints as `RenderType[name:CompositeState[...texture[Optional[id](...)...]]`. */
    private static final java.util.regex.Pattern DRAW = java.util.regex.Pattern.compile("^RenderType\\[([^:]+):(?:.*?Optional\\[([^\\]]+)\\])?");

    /** One frame: its interval from the previous frame's start, per path its CPU and GPU nanoseconds and calls, and the
     *  nanoseconds the recorder itself spent on GPU queries. */
    record Frame(long interval, long[] cpu, long[] gpu, int[] calls, long overhead) {}
    /** A window's frames, with each path's parent (or -1) and own section name, since names may hold slashes. */
    record Window(String name, boolean gpu, List<String> paths, int[] parents, List<String> sections, List<Frame> frames) {}
    private record Span(int path, int start, int end) {}

    private final Map<Object, String> names = new IdentityHashMap<>();
    private final List<String> paths = new ArrayList<>(), sections = new ArrayList<>();
    private final IntArrayList parents = new IntArrayList(), depths = new IntArrayList();
    private final List<Object2IntOpenHashMap<String>> children = new ArrayList<>();
    private final Object2IntOpenHashMap<String> roots = new Object2IntOpenHashMap<>();
    private final BitSet timed = new BitSet();
    private final IntArrayList free = new IntArrayList();
    private final ArrayDeque<long[]> stack = new ArrayDeque<>();  // path, query (or -1) and CPU start
    private final ArrayDeque<List<Span>> pending = new ArrayDeque<>();
    private final ArrayDeque<long[]> pendingGpu = new ArrayDeque<>();
    private final List<Frame> frames = new ArrayList<>();
    private Thread thread;
    private String name;
    private boolean gpuTiming;
    private long frameStart, objectStart, overhead;
    private int objectPath = -1;
    private long[] cpu = new long[64];
    private int[] calls = new int[64];
    private List<Span> spans = new ArrayList<>();
    private volatile boolean active;

    private PerfRecorder() { roots.defaultReturnValue(-1); }

    public boolean active() { return active; }

    /** Starts recording frames on the calling render thread under `name`, timing the rendering on the GPU when `gpu`. */
    void begin(String name, boolean gpu) {
        this.name = name; gpuTiming = gpu; thread = Thread.currentThread(); frames.clear(); frameStart = 0; objectPath = -1; overhead = 0;
        Arrays.fill(cpu, 0); Arrays.fill(calls, 0); active = true;
    }

    /** Stops recording, drops the unfinished frame, waits for the GPU to report the others and returns them. */
    Window end() {
        active = false; GL11.glFinish();
        for (var open : stack) if (open[1] >= 0) free.add((int) open[1]);
        for (var span : spans) { free.add(span.start()); free.add(span.end()); }
        stack.clear(); spans = new ArrayList<>();
        while (!pending.isEmpty()) resolve();
        return new Window(name, gpuTiming, List.copyOf(paths), parents.toIntArray(), List.copyOf(sections), List.copyOf(frames));
    }

    private int child(int parent, String section) {
        var map = parent < 0 ? roots : children.get(parent);
        int id = map.getInt(section);
        if (id >= 0) return id;
        var path = parent < 0 ? section : paths.get(parent) + "/" + section;
        id = paths.size(); paths.add(path); sections.add(section); parents.add(parent); map.put(section, id);
        int depth = parent < 0 ? 1 : depths.getInt(parent) + 1; depths.add(depth);
        var own = new Object2IntOpenHashMap<String>(); own.defaultReturnValue(-1); children.add(own);
        if ((path.equals(RENDER) || path.startsWith(RENDER + "/")) && depth <= GPU_DEPTH) timed.set(id);
        if (id >= cpu.length) { cpu = Arrays.copyOf(cpu, id * 2); calls = Arrays.copyOf(calls, id * 2); }
        return id;
    }

    private int query() {
        long start = System.nanoTime();
        if (free.isEmpty()) { int[] made = new int[512]; GL15.glGenQueries(made); free.addElements(0, made); }
        int q = free.removeInt(free.size() - 1);
        GL33.glQueryCounter(q, GL33.GL_TIMESTAMP);
        overhead += System.nanoTime() - start;
        return q;
    }

    private boolean recording() { return active && Thread.currentThread() == thread && frameStart != 0; }

    @Override public void startTick() {
        if (!active || Thread.currentThread() != thread) return;
        long now = System.nanoTime();
        if (frameStart != 0) finishFrame(now);
        frameStart = now;
        enter("root", now);
    }

    @Override public void endTick() {
        if (!recording()) return;
        while (!stack.isEmpty()) exit(System.nanoTime());
    }

    private void finishFrame(long now) {
        while (!stack.isEmpty()) exit(now);
        int n = paths.size();
        var gpu = new long[n];
        frames.add(new Frame(now - frameStart, Arrays.copyOf(cpu, n), gpu, Arrays.copyOf(calls, n), overhead));
        pending.add(spans); pendingGpu.add(gpu);
        spans = new ArrayList<>(); Arrays.fill(cpu, 0); Arrays.fill(calls, 0); overhead = 0;
        while (pending.size() > LAG) resolve();
    }

    /** Reads the oldest pending frame's timestamps into its GPU times and returns the queries to the pool. */
    private void resolve() {
        long start = System.nanoTime();
        var done = pending.removeFirst(); var into = pendingGpu.removeFirst();
        for (var span : done) {
            long t0 = GL33.glGetQueryObjecti64(span.start(), GL15.GL_QUERY_RESULT), t1 = GL33.glGetQueryObjecti64(span.end(), GL15.GL_QUERY_RESULT);
            if (span.path() < into.length) into[span.path()] += t1 - t0;
            free.add(span.start()); free.add(span.end());
        }
        overhead += System.nanoTime() - start;
    }

    private void enter(String section, long now) {
        int path = child(stack.isEmpty() ? -1 : (int) stack.peek()[0], section);
        stack.push(new long[]{path, gpuTiming && timed.get(path) ? query() : -1, now});
        calls[path]++;
    }

    private void exit(long now) {
        var top = stack.pop(); int path = (int) top[0];
        cpu[path] += now - top[2];
        if (top[1] >= 0) spans.add(new Span(path, (int) top[1], query()));
    }

    /** Times one object's render on the CPU under the current section, as `<section>/<kind>:<registry name>`. */
    public void beginObject(Object type, String kind, net.minecraft.core.Registry<?> registry) {
        if (!recording() || stack.isEmpty() || objectPath >= 0) return;
        var object = names.computeIfAbsent(type, t -> kind + ":" + ((net.minecraft.core.Registry<Object>) registry).getKey(t));
        objectPath = child((int) stack.peek()[0], object); calls[objectPath]++; objectStart = System.nanoTime();
    }

    public void endObject() {
        if (objectPath < 0) return;
        cpu[objectPath] += System.nanoTime() - objectStart; objectPath = -1;
    }

    /** A draw's section name: its render type and, where it has one, its texture, so each model's draws read apart. */
    public String drawSection(net.minecraft.client.renderer.RenderType type) {
        return names.computeIfAbsent(type, t -> {
            var text = t.toString();
            var composite = DRAW.matcher(text);
            return "draw:" + (composite.find() ? composite.group(1) + (composite.group(2) != null ? " " + composite.group(2) : "") : text);
        });
    }

    @Override public void push(String section) { if (recording()) enter(section, System.nanoTime()); }
    @Override public void push(Supplier<String> section) { if (recording()) enter(section.get(), System.nanoTime()); }
    @Override public void pop() { if (recording() && stack.size() > 1) exit(System.nanoTime()); }
    @Override public void popPush(String section) { pop(); push(section); }
    @Override public void popPush(Supplier<String> section) { pop(); push(section); }
    @Override public void markForCharting(MetricCategory category) {}
    @Override public void incrementCounter(String counter, int amount) {}
    @Override public void incrementCounter(Supplier<String> counter, int amount) {}
}

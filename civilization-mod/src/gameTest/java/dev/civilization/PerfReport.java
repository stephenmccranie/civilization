package dev.civilization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;
import net.minecraft.client.Minecraft;

/**
 * Turns recorded frames and Java Flight Recorder profiles into runs/perf/report: report.md for reading and report.json
 * for comparing runs. Frame pacing is judged against the {@value PerfBenchmark#TARGET_FPS} fps budget; costs are means
 * per frame; the slowest frames are explained by the sections that ran furthest over their usual time.
 */
final class PerfReport {
    record Pass(PerfRecorder.Window window, boolean capped, Profile profile) {}
    /** A profile's sample counts: per thread group, and on the render thread and over all threads per owner and method. */
    record Profile(int samples, Map<String, Integer> threads, Map<String, Integer> renderOwners, Map<String, Integer> renderMethods,
                   Map<String, Integer> allOwners, Map<String, Integer> allMethods, int gcs, double gcPauseMs, double gcMaxMs, double periodMs) {}
    private static final double BUDGET = 1000.0 / PerfBenchmark.TARGET_FPS;
    private static final double SAMPLE_MS = 4;
    /** Package roots of the code that runs in the pack, by owner; the first match wins. */
    private static final String[][] OWNERS = {
            {"dev.civilization", "Civilization"}, {"software.bernie.geckolib", "GeckoLib"}, {"net.caffeinemc", "Sodium"},
            {"net.irisshaders", "Iris"}, {"com.seibel.distanthorizons", "Distant Horizons"}, {"DistantHorizons", "Distant Horizons"},
            {"traben.entity_texture_features", "ETF"}, {"traben.entity_model_features", "EMF"}, {"me.pepperbell.continuity", "Continuity"},
            {"dev.ryanhcode.sable", "Sable"}, {"journeymap", "JourneyMap"}, {"snownee", "Jade/Kiwi"}, {"mezz.jei", "JEI"},
            {"com.ishland.c2me", "C2ME"}, {"net.caffeinemc.mods.lithium", "Lithium"}, {"malte0811.ferritecore", "FerriteCore"},
            {"net.neoforged", "NeoForge"}, {"cpw.mods", "NeoForge"}, {"org.lwjgl", "LWJGL (OpenGL driver)"},
            {"net.minecraft", "Minecraft"}, {"com.mojang", "Minecraft"}};

    private PerfReport() {}

    static jdk.jfr.Recording startProfile() {
        try {
            var recording = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
            recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis((long) SAMPLE_MS));
            recording.enable("jdk.NativeMethodSample").withPeriod(java.time.Duration.ofMillis(20));
            recording.start();
            return recording;
        } catch (IOException | java.text.ParseException e) { throw new IllegalStateException("Flight recorder", e); }
    }

    /** Stops the recording, keeps its file beside the report and reduces it to a profile. */
    static Profile stopProfile(jdk.jfr.Recording recording, Minecraft mc, String name) {
        try {
            recording.stop();
            var file = dir(mc).resolve(name + ".jfr"); recording.dump(file); recording.close();
            var threads = new HashMap<String, Integer>(); var renderOwners = new HashMap<String, Integer>(); var renderMethods = new HashMap<String, Integer>();
            var allOwners = new HashMap<String, Integer>(); var allMethods = new HashMap<String, Integer>();
            int samples = 0, gcs = 0; double pause = 0, longest = 0;
            for (var event : jdk.jfr.consumer.RecordingFile.readAllEvents(file)) {
                var type = event.getEventType().getName();
                if (type.equals("jdk.GarbageCollection")) {
                    gcs++; pause += event.getDuration("sumOfPauses").toNanos() / 1e6; longest = Math.max(longest, event.getDuration("longestPause").toNanos() / 1e6);
                    continue;
                }
                if (!type.equals("jdk.ExecutionSample") || event.getStackTrace() == null) continue;
                samples++;
                var thread = event.getThread("sampledThread").getJavaName();
                var group = thread == null ? "?" : thread.replaceAll("\\d+", "#");
                threads.merge(group, 1, Integer::sum);
                var frame = event.getStackTrace().getFrames().stream().filter(f -> !jdk(f.getMethod().getType().getName())).findFirst();
                if (frame.isEmpty()) continue;
                var owner = owner(frame.get().getMethod().getType().getName());
                var method = simple(frame.get().getMethod().getType().getName()) + "." + frame.get().getMethod().getName();
                allOwners.merge(owner, 1, Integer::sum); allMethods.merge(method, 1, Integer::sum);
                if ("Render thread".equals(thread)) { renderOwners.merge(owner, 1, Integer::sum); renderMethods.merge(method, 1, Integer::sum); }
            }
            return new Profile(samples, threads, renderOwners, renderMethods, allOwners, allMethods, gcs, pause, longest, SAMPLE_MS);
        } catch (IOException e) { throw new IllegalStateException("Flight recorder", e); }
    }

    private static boolean jdk(String type) { return type.startsWith("java.") || type.startsWith("jdk.") || type.startsWith("sun.") || type.startsWith("it.unimi.dsi"); }
    private static String simple(String type) { return type.substring(type.lastIndexOf('.') + 1); }
    private static String owner(String type) {
        for (var o : OWNERS) if (type.startsWith(o[0])) return o[1];
        var parts = type.split("\\.");
        return parts.length > 2 ? parts[0] + "." + parts[1] : type;
    }

    private static Path dir(Minecraft mc) throws IOException {
        var dir = mc.gameDirectory.toPath().resolve("report"); Files.createDirectories(dir); return dir;
    }

    // --- frames -----------------------------------------------------------------------------------------------------

    private static double percentile(double[] sorted, double p) { return sorted[Math.min(sorted.length - 1, (int) Math.floor(p * (sorted.length - 1)))]; }
    private static double mean(double[] values) { return Arrays.stream(values).average().orElse(0); }
    /** The mean of the slowest share of frames, the basis of "1% low" frame rates. */
    private static double worstMean(double[] sorted, double share) { int n = Math.max(1, (int) Math.ceil(sorted.length * share)); return mean(Arrays.copyOfRange(sorted, sorted.length - n, sorted.length)); }

    private static double value(PerfRecorder.Frame f, int path, boolean gpu) {
        var values = gpu ? f.gpu() : f.cpu(); return path >= 0 && path < values.length ? values[path] / 1e6 : 0;
    }

    /** Per path, its self time in one frame: its own time less that of its direct child sections. */
    private static double[] self(PerfRecorder.Window w, int[] parents, PerfRecorder.Frame f, boolean gpu) {
        var self = new double[w.paths().size()];
        for (int i = 0; i < self.length; i++) self[i] += value(f, i, gpu);
        for (int i = 0; i < self.length; i++) if (parents[i] >= 0) self[parents[i]] -= value(f, i, gpu);
        return self;
    }

    /** One measured pass reduced to numbers: pacing over every frame, mean section costs and the slowest frames' causes. */
    private static Map<String, Object> summarize(Pass pass) {
        var w = pass.window(); var frames = w.frames(); var paths = w.paths(); var parents = w.parents();
        int render = paths.indexOf("root/gameRenderer"), display = paths.indexOf("root/updateDisplay"), root = paths.indexOf("root");
        var interval = frames.stream().mapToDouble(f -> f.interval() / 1e6).toArray();
        var cpuWork = frames.stream().mapToDouble(f -> value(f, root, false) - value(f, display, false)).toArray();
        var gpu = frames.stream().mapToDouble(f -> value(f, render, true)).toArray();
        var deltas = IntStream.range(1, interval.length).mapToDouble(i -> Math.abs(interval[i] - interval[i - 1])).toArray();
        var sorted = interval.clone(); Arrays.sort(sorted); var cpuSorted = cpuWork.clone(); Arrays.sort(cpuSorted);
        var gpuSorted = gpu.clone(); Arrays.sort(gpuSorted); var deltaSorted = deltas.clone(); Arrays.sort(deltaSorted);
        double tolerance = pass.capped() ? 1.0 : 0;
        var out = new LinkedHashMap<String, Object>();
        out.put("view", w.name()); out.put("capped", pass.capped()); out.put("gpuTimed", w.gpu()); out.put("frames", frames.size());
        out.put("recorderMs", frames.stream().mapToDouble(f -> f.overhead() / 1e6).average().orElse(0));
        out.put("seconds", Arrays.stream(interval).sum() / 1000);
        out.put("fps", 1000 / mean(interval));
        out.put("low1Fps", 1000 / worstMean(sorted, .01)); out.put("low01Fps", 1000 / worstMean(sorted, .001));
        out.put("frameMs", Map.of("mean", mean(interval), "p50", percentile(sorted, .5), "p90", percentile(sorted, .9), "p99", percentile(sorted, .99),
                "p999", percentile(sorted, .999), "max", sorted.length == 0 ? 0 : sorted[sorted.length - 1]));
        out.put("overBudgetPct", 100.0 * Arrays.stream(interval).filter(v -> v > BUDGET + tolerance).count() / Math.max(1, interval.length));
        out.put("stutters17", Arrays.stream(interval).filter(v -> v > 2 * BUDGET).count());
        out.put("stutters33", Arrays.stream(interval).filter(v -> v > 4 * BUDGET).count());
        out.put("jitterMs", Map.of("mean", mean(deltas), "p99", deltaSorted.length == 0 ? 0 : percentile(deltaSorted, .99)));
        out.put("cpuWorkMs", Map.of("p50", percentile(cpuSorted, .5), "p99", percentile(cpuSorted, .99)));
        out.put("gpuMs", Map.of("p50", percentile(gpuSorted, .5), "p99", percentile(gpuSorted, .99)));
        // Mean cost per frame of every section, and of every section name summed across the frame.
        var cpuSelf = new double[paths.size()]; var gpuSelf = new double[paths.size()]; var cpuTotal = new double[paths.size()]; var gpuTotal = new double[paths.size()]; var calls = new double[paths.size()];
        var medianSelf = new ArrayList<double[][]>();
        for (var f : frames) {
            var cs = self(w, parents, f, false); var gs = self(w, parents, f, true);
            for (int i = 0; i < paths.size(); i++) {
                cpuSelf[i] += cs[i]; gpuSelf[i] += gs[i]; cpuTotal[i] += value(f, i, false); gpuTotal[i] += value(f, i, true);
                calls[i] += i < f.calls().length ? f.calls()[i] : 0;
            }
            medianSelf.add(new double[][]{cs, gs});
        }
        int n = Math.max(1, frames.size());
        var sections = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < paths.size(); i++) sections.add(Map.of("path", paths.get(i), "calls", calls[i] / n, "cpuMs", cpuTotal[i] / n, "cpuSelfMs", cpuSelf[i] / n,
                "gpuMs", gpuTotal[i] / n, "gpuSelfMs", gpuSelf[i] / n));
        out.put("sections", sections);
        var names = new TreeMap<String, double[]>();
        for (int i = 0; i < paths.size(); i++) {
            var name = w.sections().get(i);
            var v = names.computeIfAbsent(name, k -> new double[3]); v[0] += cpuSelf[i] / n; v[1] += gpuSelf[i] / n; v[2] += calls[i] / n;
        }
        out.put("names", names.entrySet().stream().map(e -> Map.of("name", e.getKey(), "cpuSelfMs", e.getValue()[0], "gpuSelfMs", e.getValue()[1], "calls", e.getValue()[2])).toList());
        // The slowest frames, each with the sections that ran furthest over their median self time.
        var medians = new double[2][paths.size()];
        for (int g = 0; g < 2; g++) for (int i = 0; i < paths.size(); i++) {
            int fi = i, gi = g; var column = medianSelf.stream().mapToDouble(s -> s[gi][fi]).sorted().toArray();
            medians[g][i] = column.length == 0 ? 0 : column[column.length / 2];
        }
        var worst = IntStream.range(0, frames.size()).boxed().sorted(Comparator.comparingDouble(i -> -interval[i])).limit(10).toList();
        out.put("slowest", worst.stream().map(i -> {
            var causes = new ArrayList<String>();
            for (int g = 0; g < 2; g++) {
                int gi = g; var s = medianSelf.get(i)[g];
                IntStream.range(0, paths.size()).boxed().sorted(Comparator.comparingDouble(k -> -(s[k] - medians[gi][k]))).limit(3)
                        .filter(k -> s[k] - medians[gi][k] > .2)
                        .forEach(k -> causes.add(String.format(Locale.ROOT, "%s %s +%.1f ms", gi == 0 ? "CPU" : "GPU", paths.get(k), s[k] - medians[gi][k])));
            }
            return Map.of("frame", i, "ms", interval[i], "cpuWorkMs", cpuWork[i], "gpuMs", gpu[i], "causes", causes);
        }).toList());
        if (pass.profile() != null) out.put("profile", pass.profile());
        return out;
    }

    // --- writing -----------------------------------------------------------------------------------------------------

    static void write(Minecraft mc, List<Pass> passes) throws IOException {
        var dir = dir(mc);
        var summaries = passes.stream().map(PerfReport::summarize).toList();
        var meta = new LinkedHashMap<String, Object>();
        meta.put("resolution", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
        meta.put("renderer", com.mojang.blaze3d.platform.GlUtil.getRenderer()); meta.put("cpu", com.mojang.blaze3d.platform.GlUtil.getCpuInfo());
        var iris = new Properties();
        try (var in = Files.newInputStream(mc.gameDirectory.toPath().resolve("config/iris.properties"))) { iris.load(in); }
        meta.put("shaderPack", iris.getProperty("enableShaders", "false").equals("true") ? iris.getProperty("shaderPack") : "none");
        meta.put("resourcePacks", mc.getResourcePackRepository().getSelectedIds());
        meta.put("renderDistance", mc.options.renderDistance().get()); meta.put("targetFps", PerfBenchmark.TARGET_FPS);
        meta.put("mods", net.neoforged.fml.ModList.get().getMods().stream().map(m -> m.getModId() + " " + m.getVersion()).sorted().toList());
        Files.writeString(dir.resolve("report.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create()
                .toJson(Map.of("meta", meta, "passes", summaries)));
        Files.writeString(dir.resolve("report.md"), markdown(meta, summaries));
    }

    @SuppressWarnings("unchecked")
    private static String markdown(Map<String, Object> meta, List<Map<String, Object>> passes) {
        var md = new StringBuilder("# Performance report\n\n");
        md.append(String.format(Locale.ROOT, "%s, %s, %s, shader %s, render distance %s, target %d fps (%.2f ms).%n%n", meta.get("resolution"), meta.get("renderer"),
                meta.get("cpu"), meta.get("shaderPack"), meta.get("renderDistance"), PerfBenchmark.TARGET_FPS, BUDGET));
        md.append("## Frame pacing\n\n| View | Cap | FPS | 1% low | 0.1% low | p50 ms | p99 ms | max ms | over budget | >16.7 ms | >33 ms | jitter mean / p99 ms | CPU work p50 / p99 | GPU p50 / p99 | Recorder ms |\n");
        md.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        for (var p : passes) {
            var f = (Map<String, Double>) p.get("frameMs"); var j = (Map<String, Double>) p.get("jitterMs"); var c = (Map<String, Double>) p.get("cpuWorkMs"); var g = (Map<String, Double>) p.get("gpuMs");
            var gpu = (boolean) p.get("gpuTimed") ? String.format(Locale.ROOT, "%.2f / %.2f", g.get("p50"), g.get("p99")) : "not timed";
            md.append(String.format(Locale.ROOT, "| %s | %s | %.0f | %.0f | %.0f | %.2f | %.2f | %.1f | %.1f%% | %d | %d | %.2f / %.2f | %.2f / %.2f | %s | %.2f |%n", p.get("view"),
                    (boolean) p.get("capped") ? PerfBenchmark.TARGET_FPS : "none", p.get("fps"), p.get("low1Fps"), p.get("low01Fps"), f.get("p50"), f.get("p99"), f.get("max"),
                    p.get("overBudgetPct"), p.get("stutters17"), p.get("stutters33"), j.get("mean"), j.get("p99"), c.get("p50"), c.get("p99"), gpu, p.get("recorderMs")));
        }
        md.append("\nCPU work is the render thread's frame less the present and frame-cap wait. GPU is the GPU time of everything the frame renders; it includes idle gaps when the GPU waits for the CPU. ")
                .append("The capped pass times nothing on the GPU, so it shows the pacing players get; the uncapped pass times the frame's rendering on the GPU, at the recorder cost shown. ")
                .append("The hidden client presents nothing and keeps at most two frames in flight on the GPU, as a visible window's swap chain does; Distant Horizons generates no terrain locally, as for players on the server.\n");
        for (var p : passes) {
            if ((boolean) p.get("capped")) continue;
            md.append("\n## ").append(p.get("view")).append(", uncapped\n\n### Costliest by name (self ms per frame, summed over the frame)\n\n| Section | Calls | CPU | GPU |\n| --- | ---: | ---: | ---: |\n");
            var names = (List<Map<String, Object>>) p.get("names");
            names.stream().filter(m -> !m.get("name").equals("root")).sorted(Comparator.comparingDouble(m -> -Math.max((double) m.get("cpuSelfMs"), (double) m.get("gpuSelfMs")))).limit(30)
                    .forEach(m -> md.append(String.format(Locale.ROOT, "| %s | %.1f | %.3f | %.3f |%n", m.get("name"), m.get("calls"), m.get("cpuSelfMs"), m.get("gpuSelfMs"))));
            md.append("\n### Costliest sections (ms per frame)\n\n| Path | Calls | CPU | CPU self | GPU | GPU self |\n| --- | ---: | ---: | ---: | ---: | ---: |\n");
            var sections = (List<Map<String, Object>>) p.get("sections");
            sections.stream().filter(m -> !m.get("path").equals("root")).sorted(Comparator.comparingDouble(m -> -Math.max((double) m.get("cpuSelfMs"), (double) m.get("gpuSelfMs")))).limit(30)
                    .forEach(m -> md.append(String.format(Locale.ROOT, "| %s | %.1f | %.3f | %.3f | %.3f | %.3f |%n", m.get("path"), m.get("calls"), m.get("cpuMs"), m.get("cpuSelfMs"), m.get("gpuMs"), m.get("gpuSelfMs"))));
            md.append("\n### Slowest frames\n\n");
            for (var s : (List<Map<String, Object>>) p.get("slowest"))
                md.append(String.format(Locale.ROOT, "- %.1f ms (CPU work %.1f, GPU %.1f): %s%n", s.get("ms"), s.get("cpuWorkMs"), s.get("gpuMs"), String.join("; ", (List<String>) s.get("causes"))));
            if (p.get("profile") instanceof Profile profile) {
                md.append(String.format(Locale.ROOT, "%n### Java profile%n%n%d samples every %.0f ms; %d collections, %.1f ms paused in all, longest %.1f ms.%n%n", profile.samples(),
                        profile.periodMs(), profile.gcs(), profile.gcPauseMs(), profile.gcMaxMs()));
                table(md, "Threads (samples)", profile.threads(), 12);
                table(md, "Render thread by owner", profile.renderOwners(), 15);
                table(md, "Render thread methods (self)", profile.renderMethods(), 25);
                table(md, "All threads by owner", profile.allOwners(), 15);
                table(md, "All threads methods (self)", profile.allMethods(), 25);
            }
        }
        return md.toString();
    }

    private static void table(StringBuilder md, String title, Map<String, Integer> counts, int limit) {
        int total = Math.max(1, counts.values().stream().mapToInt(Integer::intValue).sum());
        md.append("| ").append(title).append(" | Samples | Share |\n| --- | ---: | ---: |\n");
        counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit)
                .forEach(e -> md.append(String.format(Locale.ROOT, "| %s | %d | %.1f%% |%n", e.getKey(), e.getValue(), 100.0 * e.getValue() / total)));
        md.append('\n');
    }
}

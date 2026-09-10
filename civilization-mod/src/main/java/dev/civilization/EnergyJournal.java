package dev.civilization;

import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** One bounded queue and writer for all players; disk work never runs on the server tick. */
public final class EnergyJournal implements AutoCloseable {
    private final Path directory;
    private final long maxBytes;
    private final int backups;
    private final ArrayBlockingQueue<String> queue;
    private final AtomicLong dropped = new AtomicLong();
    private final Thread worker;
    private volatile boolean closing;
    private BufferedWriter writer;
    private long bytes;

    public EnergyJournal(Path directory, long maxBytes, int backups, int queueCapacity) {
        this.directory = directory;
        this.maxBytes = maxBytes;
        this.backups = backups;
        queue = new ArrayBlockingQueue<>(queueCapacity);
        worker = new Thread(this::run, "Civilization-energy-log");
        worker.setDaemon(true);
        worker.start();
    }

    public boolean append(String line) {
        if (closing || !queue.offer(line)) {
            lost();
            return false;
        }
        return true;
    }

    public long dropped() { return dropped.get(); }

    private void lost() {
        long count = dropped.incrementAndGet();
        if (count == 1 || count % 1024 == 0)
            LogUtils.getLogger().error("Energy audit log lost {} records (queue full, shutdown, or disk failure)", count);
    }

    private void run() {
        long lastFlush = System.nanoTime();
        try {
            while (!closing || !queue.isEmpty()) {
                String line = queue.poll(250, TimeUnit.MILLISECONDS);
                if (line != null) {
                    try { write(line); }
                    catch (IOException ex) {
                        lost();
                        if (dropped.get() == 1) LogUtils.getLogger().error("Cannot write energy log in {}", directory, ex);
                        closeWriter();
                    }
                }
                if (System.nanoTime() - lastFlush >= TimeUnit.SECONDS.toNanos(1)) {
                    if (writer != null) writer.flush();
                    lastFlush = System.nanoTime();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            LogUtils.getLogger().error("Energy log flush failed", ex);
        } finally {
            closing = true;
            closeWriter();
            String remaining;
            while ((remaining = queue.poll()) != null) lost();
        }
    }

    private void write(String line) throws IOException {
        Path current = directory.resolve("energy-current.jsonl");
        if (writer == null) {
            Files.createDirectories(directory);
            bytes = Files.exists(current) ? Files.size(current) : 0;
            writer = Files.newBufferedWriter(current, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        long size = line.getBytes(StandardCharsets.UTF_8).length + 1;
        if (bytes > 0 && bytes + size > maxBytes) {
            writer.close(); writer = null;
            Files.deleteIfExists(directory.resolve("energy-" + backups + ".jsonl"));
            for (int i = backups - 1; i >= 1; i--) {
                Path previous = directory.resolve("energy-" + i + ".jsonl");
                if (Files.exists(previous)) Files.move(previous, directory.resolve("energy-" + (i + 1) + ".jsonl"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(current, directory.resolve("energy-1.jsonl"), StandardCopyOption.REPLACE_EXISTING);
            writer = Files.newBufferedWriter(current, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            bytes = 0;
        }
        writer.write(line); writer.write('\n'); bytes += size;
    }

    private void closeWriter() {
        if (writer != null) {
            try { writer.close(); }
            catch (IOException ex) { LogUtils.getLogger().error("Closing energy log failed", ex); }
            writer = null;
        }
    }

    @Override public void close() {
        closing = true;
        try { worker.join(5000); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        if (worker.isAlive()) LogUtils.getLogger().error("Energy writer still draining after 5 seconds; abrupt exit may lose records");
    }
}

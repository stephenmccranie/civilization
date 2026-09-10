package dev.civilization;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EnergyJournalTest {
    @TempDir Path directory;

    @Test void shutdownDrainsQueueInOrder() throws Exception {
        try (var journal = new EnergyJournal(directory, 65536, 2, 1024)) {
            for (int i = 0; i < 100; i++) assertTrue(journal.append("{\"n\":" + i + "}"));
        }
        var lines = Files.readAllLines(directory.resolve("energy-current.jsonl"));
        assertEquals(100, lines.size());
        assertEquals("{\"n\":0}", lines.getFirst());
        assertEquals("{\"n\":99}", lines.getLast());
    }

    @Test void rotationKeepsOnlyBoundedBackupsAndNewestRecords() throws Exception {
        try (var journal = new EnergyJournal(directory, 20, 2, 1024)) {
            for (int i = 0; i < 10; i++) journal.append("{\"n\":" + i + "}");
        }
        try (var files = Files.list(directory)) { assertEquals(3, files.count()); }
        assertEquals(java.util.List.of("{\"n\":8}", "{\"n\":9}"), Files.readAllLines(directory.resolve("energy-current.jsonl")));
        assertEquals(java.util.List.of("{\"n\":4}", "{\"n\":5}"), Files.readAllLines(directory.resolve("energy-2.jsonl")));
    }

    @Test void restartAppendsWithoutOverwritingAndPreservesUnicode() throws Exception {
        try (var journal = new EnergyJournal(directory, 65536, 2, 10)) { journal.append("{\"name\":\"玩家\"}"); }
        try (var journal = new EnergyJournal(directory, 65536, 2, 10)) { journal.append("{\"name\":\"second\"}"); }
        assertEquals(java.util.List.of("{\"name\":\"玩家\"}", "{\"name\":\"second\"}"),
                Files.readAllLines(directory.resolve("energy-current.jsonl")));
    }

    @Test void writesBecomeReadableWithoutShutdown() throws Exception {
        try (var journal = new EnergyJournal(directory, 65536, 2, 10)) {
            journal.append("{\"live\":true}");
            long deadline = System.nanoTime() + 3_000_000_000L;
            Path file = directory.resolve("energy-current.jsonl");
            while ((!Files.exists(file) || Files.size(file) == 0) && System.nanoTime() < deadline) Thread.sleep(20);
            assertEquals("{\"live\":true}\n", Files.readString(file));
        }
    }
}

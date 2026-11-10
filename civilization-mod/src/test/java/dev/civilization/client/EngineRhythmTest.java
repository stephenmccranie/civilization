package dev.civilization.client;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EngineRhythmTest {
    /** The ticks on which `beat` sounds over `ticks` ticks of running or coasting. */
    private static List<Integer> beats(EngineRhythm rhythm, boolean running, int ticks, EngineRhythm.Beat beat) {
        var at = new ArrayList<Integer>();
        for (int t = 0; t < ticks; t++) if (rhythm.tick(running) == beat) at.add(t);
        return at;
    }

    @Test void startFromRestKicksOnceThenKnocksTightenToTwoATurn() {
        var rhythm = new EngineRhythm(false);
        assertEquals(EngineRhythm.Beat.KICK, rhythm.tick(true));
        var knocks = beats(rhythm, true, 200, EngineRhythm.Beat.KNOCK);
        assertTrue(knocks.getFirst() <= 10, "The engine catches within half a second of the kick");
        assertTrue(knocks.get(1) - knocks.get(0) > EngineRhythm.BEAT, "The first knocks come slower than running speed");
        for (int i = knocks.size() - 5; i < knocks.size(); i++)
            assertEquals(EngineRhythm.BEAT, knocks.get(i) - knocks.get(i - 1), "At speed, a knock every half turn");
    }

    @Test void coastingSpreadsTheKnocksThenStopsAndTheNextStartKicksAgain() {
        var rhythm = new EngineRhythm(false);
        beats(rhythm, true, 200, EngineRhythm.Beat.KNOCK);
        var knocks = beats(rhythm, false, 100, EngineRhythm.Beat.KNOCK);
        assertFalse(knocks.isEmpty(), "The flywheel coasts on after the load stops");
        for (int i = 1; i < knocks.size(); i++) assertTrue(knocks.get(i) - knocks.get(i - 1) > EngineRhythm.BEAT, "Coasting knocks spread apart");
        assertEquals(0, rhythm.speed, "The flywheel comes to rest");
        assertEquals(EngineRhythm.Beat.KICK, rhythm.tick(true), "A start from rest kicks the flywheel over again");
    }

    @Test void restartingWhileCoastingResumesWithoutAKick() {
        var rhythm = new EngineRhythm(false);
        beats(rhythm, true, 200, EngineRhythm.Beat.KNOCK);
        for (int t = 0; t < 5; t++) rhythm.tick(false);
        assertTrue(beats(rhythm, true, 100, EngineRhythm.Beat.KICK).isEmpty(), "A turning flywheel needs no kick");
    }

    @Test void anEngineFirstMetRunningKnocksAtSpeedWithoutAKick() {
        var rhythm = new EngineRhythm(true);
        assertTrue(beats(rhythm, true, 100, EngineRhythm.Beat.KICK).isEmpty(), "A running engine coming into view was not just started");
        var knocks = beats(rhythm, true, 100, EngineRhythm.Beat.KNOCK);
        assertEquals(EngineRhythm.BEAT, knocks.get(1) - knocks.get(0), "It is already at speed");
    }
}

package dev.civilization.client;

import dev.civilization.OilEngineContent;
import dev.civilization.OilEngineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Hot-Bulb Engine heard: the blowlamp's roar at the lamp ports while they heat the bulbs and keep them hot, fading
 * as the engine catches, and under load the flywheel's {@link EngineRhythm} of exhaust knocks from the stack over a
 * quiet mechanical bed that follows its speed.
 * One voice per engine, ticked by the engine on the client.
 */
public final class OilEngineSounds {
    private static final Map<OilEngineEntity, Voice> VOICES = new WeakHashMap<>();

    public static void tick(OilEngineEntity engine) { VOICES.computeIfAbsent(engine, Voice::new).tick(); }

    private static final class Voice {
        private final OilEngineEntity engine;
        private final EngineRhythm rhythm = new EngineRhythm();
        private final RandomSource random = RandomSource.create();
        private Loop blowlamp, bed;

        Voice(OilEngineEntity engine) { this.engine = engine; }

        void tick() {
            boolean running = engine.working();
            var beat = rhythm.tick(running);
            blowlamp = sustain(blowlamp, OilEngineContent.BLOWLAMP.get(), engine.lit() ? 1 : 0, engine.point(7, 40.5, 30.5));
            bed = sustain(bed, OilEngineContent.BED.get(), (float) rhythm.speed, engine.point(16, 20, 32));
            var stack = engine.point(16, 48, 54);
            if (beat == EngineRhythm.Beat.KICK) play(OilEngineContent.KICK.get(), engine.point(16, 14, 57.5), 1, 1);
            else if (beat == EngineRhythm.Beat.KNOCK)
                play(OilEngineContent.KNOCK.get(), stack, running ? 1 : .6f * (float) rhythm.speed,
                     (.92f + .08f * (float) rhythm.speed) * (.96f + .08f * random.nextFloat()));
        }

        private void play(SoundEvent sound, Vec3 at, float volume, float pitch) {
            engine.getLevel().playLocalSound(at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch, false);
        }

        /** Keeps a loop sounding at `level`, starting a new one when it has stopped and is wanted again. */
        private Loop sustain(Loop loop, SoundEvent sound, float level, Vec3 at) {
            if (loop != null && !loop.isStopped()) { loop.target = level; return loop; }
            if (level == 0) return null;
            loop = new Loop(engine, sound, at);
            loop.target = level;
            Minecraft.getInstance().getSoundManager().play(loop);
            return loop;
        }
    }

    /** A steady bed at one point of the engine that eases toward its target level over about a second, so the blowlamp
     *  overlaps the kick and catch, and ends once it falls silent. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final OilEngineEntity engine;
        float target;

        Loop(OilEngineEntity engine, SoundEvent sound, Vec3 at) {
            super(sound, SoundSource.BLOCKS, RandomSource.create());
            this.engine = engine;
            looping = true; delay = 0; volume = 0; x = at.x; y = at.y; z = at.z;
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            if (engine.isRemoved() || engine.getLevel() != Minecraft.getInstance().level) { stop(); return; }
            volume += (target - volume) * .05f;
            if (target == 0 && volume < .01f) stop();
        }
    }
}

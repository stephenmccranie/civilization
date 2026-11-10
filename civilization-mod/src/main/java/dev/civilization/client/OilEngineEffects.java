package dev.civilization.client;

import dev.civilization.OilEngineContent;
import dev.civilization.OilEngineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Hot-Bulb Engine heard and seen: the blowlamp's roar at the lamp ports while they heat the bulbs, fading as the
 * engine catches, and while it runs the flywheel's {@link EngineRhythm} of exhaust knocks over a quiet mechanical bed
 * that follows its speed, each firing knock puffing smoke from the stack and the kick throwing out a heavy burst.
 * One voice per engine, ticked by the engine on the client.
 */
public final class OilEngineEffects {
    /** Beyond this many blocks from the camera an engine is silent: its farthest sound, the knock, carries 32. */
    private static final double EARSHOT = 40;
    /** Weak keys: a voice holds no reference to its engine, so an unloaded engine's voice goes with it. */
    private static final Map<OilEngineEntity, Voice> VOICES = new WeakHashMap<>();

    public static void tick(OilEngineEntity engine) {
        VOICES.computeIfAbsent(engine, e -> new Voice(e.working())).tick(engine);
    }

    private static final class Voice {
        private final EngineRhythm rhythm;
        private final RandomSource random = RandomSource.create();
        private Loop blowlamp, bed;

        Voice(boolean turning) { rhythm = new EngineRhythm(turning); }

        void tick(OilEngineEntity engine) {
            boolean running = engine.working();
            var beat = rhythm.tick(running);
            var level = engine.getLevel();
            var stack = engine.point(16, 48, 54);
            boolean heard = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(stack) < EARSHOT * EARSHOT;
            blowlamp = sustain(blowlamp, OilEngineContent.BLOWLAMP.get(), heard && engine.lit() ? 1 : 0, engine.point(7, 40.5, 30.5));
            bed = sustain(bed, OilEngineContent.BED.get(), heard ? (float) rhythm.speed : 0, engine.point(16, 20, 32));
            if (beat == EngineRhythm.Beat.KICK) {
                if (heard) play(level, OilEngineContent.KICK.get(), engine.point(16, 14, 57.5), 1, 1);
                puff(level, stack, 3, 10);
            } else if (beat == EngineRhythm.Beat.KNOCK) {
                if (heard) play(level, OilEngineContent.KNOCK.get(), stack, running ? 1 : .6f * (float) rhythm.speed,
                     (.92f + .08f * (float) rhythm.speed) * (.96f + .08f * random.nextFloat()));
                if (running) puff(level, stack, 1, 4);
            }
        }

        /** A puff from the stack: lingering columns that drift up and away, and dark smoke blown out with it. */
        private void puff(Level level, Vec3 at, int columns, int blown) {
            for (int i = 0; i < columns; i++)
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, at.x + spread(), at.y, at.z + spread(), 0, .07, 0);
            for (int i = 0; i < blown; i++)
                level.addParticle(ParticleTypes.LARGE_SMOKE, at.x + spread(), at.y, at.z + spread(), spread() / 4, .1 + .1 * random.nextDouble(), spread() / 4);
        }

        private double spread() { return (random.nextDouble() - .5) * .25; }

        private void play(Level level, SoundEvent sound, Vec3 at, float volume, float pitch) {
            level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch, false);
        }

        /** Keeps a loop sounding at `level` for this tick, starting a new one when it has stopped and is wanted again. */
        private Loop sustain(Loop loop, SoundEvent sound, float level, Vec3 at) {
            if (loop != null && !loop.isStopped()) { loop.target = level; loop.sustained = true; return loop; }
            if (level == 0) return null;
            loop = new Loop(sound, at);
            loop.target = level;
            Minecraft.getInstance().getSoundManager().play(loop);
            return loop;
        }
    }

    /** A steady bed at one point of the engine that eases toward its target level over about a second, so the blowlamp
     *  overlaps the kick and catch, and ends once it falls silent or its engine stops ticking: broken, unloaded or
     *  left behind in another world. */
    private static final class Loop extends AbstractTickableSoundInstance {
        float target;
        boolean sustained = true;

        Loop(SoundEvent sound, Vec3 at) {
            super(sound, SoundSource.BLOCKS, RandomSource.create());
            looping = true; delay = 0; volume = 0; x = at.x; y = at.y; z = at.z;
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            if (!sustained) { stop(); return; }
            sustained = false;
            volume += (target - volume) * .05f;
            if (target == 0 && volume < .01f) stop();
        }
    }
}

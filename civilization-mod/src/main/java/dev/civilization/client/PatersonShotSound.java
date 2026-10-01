package dev.civilization.client;

import dev.civilization.FirearmContent;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Shooter-only head-relative shot; remote players still hear the world-space report. */
public final class PatersonShotSound extends AbstractTickableSoundInstance {
    public PatersonShotSound(){super(FirearmContent.SHOT.get(),SoundSource.PLAYERS,RandomSource.create());relative=true;attenuation=SoundInstance.Attenuation.NONE;x=y=z=0;volume=3;pitch=.98f+random.nextFloat()*.04f;looping=false;}
    @Override public void tick(){} // The finite audio sample ends naturally; no world-position updates needed.
}

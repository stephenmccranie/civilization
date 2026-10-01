package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Persistent frying bed, golden crackle and scorched sputter; no success bell. */
final class StoveSound extends AbstractTickableSoundInstance {
    private final net.minecraft.world.level.block.entity.BlockEntity owner;private final SkilletHolder holder;private final int layer;
    private float textureVolume=.001f;
    <T extends net.minecraft.world.level.block.entity.BlockEntity & SkilletHolder> StoveSound(T owner,int layer){super(layer==0?PrototypeStoveContent.WET.get():layer==1?PrototypeStoveContent.SIZZLE.get():PrototypeStoveContent.CHAR.get(),SoundSource.BLOCKS,RandomSource.create());this.owner=owner;this.holder=owner;this.layer=layer;looping=true;delay=0;relative=true;attenuation=SoundInstance.Attenuation.NONE;x=y=0;z=-1;volume=.001f;}
    @Override public boolean canStartSilent(){return true;}
    @Override public void tick(){
        if(owner.isRemoved()||!holder.skillet().batch()||holder.skillet().warmth()==0||owner.getLevel()==null||owner.getLevel().getBlockEntity(owner.getBlockPos())!=owner){stop();return;}
        var pan=holder.skillet();
        float target=(float)(StoveSoundMix.gain(pan.work(),layer)*.47*pan.activity());
        textureVolume+=(target-textureVolume)*.15f;
        // Head-relative, shallow stereo direction; actual distance belongs to the pan.
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        var listener=camera.getPosition();
        var panPosition=net.minecraft.world.phys.Vec3.atCenterOf(owner.getBlockPos()).add(0,.5,0);
        var offset=panPosition.subtract(listener);
        double distance=offset.length();
        double rightward=-offset.dot(new net.minecraft.world.phys.Vec3(camera.getLeftVector()));
        x+=(StoveSoundMix.panOffset(rightward,distance)-x)*.35;
        volume=textureVolume*(float)StoveSoundMix.distanceGain(distance);
        // Heat changes loudness; restrained pitch preserves the generated texture identities.
        pitch=(float)(.95+.05*pan.activity());
    }
    boolean owns(net.minecraft.world.level.block.entity.BlockEntity b){return owner==b;}
    void end(){stop();}
    boolean ended(){return isStopped();}
}

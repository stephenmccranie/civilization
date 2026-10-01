package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Three overlapping textures evolve smoothly; there is no success bell. */
final class StoveSound extends AbstractTickableSoundInstance {
    private final net.minecraft.world.level.block.entity.BlockEntity owner;private final SkilletHolder holder;private final int layer;
    <T extends net.minecraft.world.level.block.entity.BlockEntity & SkilletHolder> StoveSound(T owner,int layer){super(layer==0?PrototypeStoveContent.WET.get():layer==1?PrototypeStoveContent.SIZZLE.get():PrototypeStoveContent.CHAR.get(),SoundSource.BLOCKS,RandomSource.create());this.owner=owner;this.holder=owner;this.layer=layer;looping=true;delay=0;x=owner.getBlockPos().getX()+.5;y=owner.getBlockPos().getY()+1;z=owner.getBlockPos().getZ()+.5;volume=.001f;}
    @Override public boolean canStartSilent(){return true;}
    @Override public void tick(){
        if(owner.isRemoved()||!holder.skillet().batch()||holder.skillet().warmth()==0||owner.getLevel()==null||owner.getLevel().getBlockEntity(owner.getBlockPos())!=owner){stop();return;}
        var pan=holder.skillet();double w=pan.work(),wet=1-Math.clamp(w/800,0,1),charred=Math.clamp((w-1000)/600,0,1),dry=1-wet;
        double blend=layer==0?wet:layer==1?dry*(1-charred*.85):charred;
        float target=(float)(blend*.47*pan.activity());volume+=(target-volume)*.15f;pitch=(float)(.8+.3*pan.activity()+.12*Math.clamp(w/1000,0,1));
    }
    boolean owns(net.minecraft.world.level.block.entity.BlockEntity b){return owner==b;}
    void end(){stop();}
    boolean ended(){return isStopped();}
}

package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Three overlapping textures evolve smoothly; there is no success bell. */
final class StoveSound extends AbstractTickableSoundInstance {
    private final PrototypeStoveEntity stove;private final int layer;
    StoveSound(PrototypeStoveEntity stove,int layer){super(layer==0?PrototypeStoveContent.WET.get():layer==1?PrototypeStoveContent.SIZZLE.get():PrototypeStoveContent.CHAR.get(),SoundSource.BLOCKS,RandomSource.create());this.stove=stove;this.layer=layer;looping=true;delay=0;x=stove.getBlockPos().getX()+.5;y=stove.getBlockPos().getY()+1;z=stove.getBlockPos().getZ()+.5;volume=.001f;}
    @Override public boolean canStartSilent(){return true;}
    @Override public void tick(){
        if(stove.isRemoved()||!stove.batch()||!stove.fire.lit()||stove.getLevel()==null||stove.getLevel().getBlockEntity(stove.getBlockPos())!=stove){stop();return;}
        double w=stove.work(),wet=1-Math.clamp(w/800,0,1),charred=Math.clamp((w-1000)/600,0,1),dry=1-wet;
        double blend=layer==0?wet:layer==1?dry*(1-charred*.85):charred;
        float target=stove.batch()&&stove.fire.lit()&&stove.dial()>0?(float)(blend*(.22+.25*stove.dial())):0;
        volume+=(target-volume)*.15f;pitch=(float)(.8+.3*stove.dial()+.12*Math.clamp(w/1000,0,1));
    }
    boolean ended(){return isStopped();}
}

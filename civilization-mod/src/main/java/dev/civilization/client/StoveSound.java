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
    private final net.minecraft.world.entity.player.Player carrier;
    private final net.minecraft.world.InteractionHand hand;
    private float textureVolume=.001f;
    <T extends net.minecraft.world.level.block.entity.BlockEntity & SkilletHolder> StoveSound(T owner,int layer){this(owner,null,null,layer);}
    StoveSound(net.minecraft.world.entity.player.Player carrier,net.minecraft.world.InteractionHand hand,int layer){this(null,carrier,hand,layer);}
    private StoveSound(net.minecraft.world.level.block.entity.BlockEntity owner,net.minecraft.world.entity.player.Player carrier,net.minecraft.world.InteractionHand hand,int layer){
        super(layer==0?PrototypeStoveContent.WET.get():layer==1?PrototypeStoveContent.SIZZLE.get():PrototypeStoveContent.CHAR.get(),SoundSource.BLOCKS,RandomSource.create());
        this.owner=owner;this.holder=owner instanceof SkilletHolder h?h:null;this.carrier=carrier;this.hand=hand;this.layer=layer;
        looping=true;delay=0;relative=true;attenuation=SoundInstance.Attenuation.NONE;x=y=0;z=-1;volume=.001f;
        // Start a lifted pan at its current heat rather than fading in from silence.
        if(carrier!=null){var pan=SkilletItem.contents(carrier.getItemInHand(hand),carrier.level());textureVolume=(float)(StoveSoundMix.gain(pan.work(),layer)*.47*pan.activity());tick();}
    }
    @Override public boolean canStartSilent(){return true;}
    @Override public void tick(){
        SkilletContents pan;net.minecraft.world.phys.Vec3 panPosition;
        if(carrier!=null){
            if(!carrier.isAlive()||carrier.isRemoved()||carrier.level()!=Minecraft.getInstance().level||!carrier.getItemInHand(hand).is(PrototypeStoveContent.SKILLET.get())){stop();return;}
            pan=SkilletItem.contents(carrier.getItemInHand(hand),carrier.level());panPosition=carrier.position().add(0,1,0);
        }else{
            if(owner.isRemoved()||owner.getLevel()==null||owner.getLevel()!=Minecraft.getInstance().level||owner.getLevel().getBlockEntity(owner.getBlockPos())!=owner){stop();return;}
            pan=holder.skillet();panPosition=net.minecraft.world.phys.Vec3.atCenterOf(owner.getBlockPos()).add(0,.5,0);
        }
        if(!pan.batch()||pan.warmth()==0){stop();return;}
        float target=(float)(StoveSoundMix.gain(pan.work(),layer)*.47*pan.activity());
        textureVolume+=(target-textureVolume)*.15f;
        // Head-relative, shallow stereo direction; actual distance belongs to the pan.
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        var listener=camera.getPosition();
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

package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.CargoMass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

/** A bounded representative wheelbarrow load, never one rendered object per stored item. */
public final class CargoVisuals {
    private static final int[] ORDER={2,3,0,1,4,5};
    private CargoVisuals(){}
    public static void render(ItemStack icon,float fill,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(icon.isEmpty()||fill<=0)return;
        var state=CargoMass.material(icon);if(state.isAir())return;
        int count=Math.clamp((int)Math.ceil(fill*12),1,12);
        for(int i=0;i<count;i++){
            int layer=i/6,ordered=ORDER[i%6],x=ordered%2,z=ordered/2;
            pose.pushPose();pose.translate((x-1)*.32f+.006f,.617f+layer*.20f,(z-1.5f)*.32f+.18f+.006f);pose.scale(.32f-.012f,.20f-.008f,.32f-.012f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);pose.popPose();
        }
    }
}

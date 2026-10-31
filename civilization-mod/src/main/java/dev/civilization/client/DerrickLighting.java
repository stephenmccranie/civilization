package dev.civilization.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** The solid controller is not a representative light sample for the exposed tower. */
public final class DerrickLighting {
    private DerrickLighting() {}
    public static int sample(Level level,BlockPos controller,int light){
        int sky=LightTexture.sky(light),block=LightTexture.block(light);
        // Just outside the base footprint, plus the open frame above the machinery.
        // Use actual world light, so a dark enclosure stays dark and night remains night.
        for(var side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP}){
            var at=controller.relative(side,side==Direction.UP?4:5);
            if(!level.hasChunkAt(at))continue;
            int nearby=LevelRenderer.getLightColor(level,at);
            sky=Math.max(sky,LightTexture.sky(nearby));block=Math.max(block,LightTexture.block(nearby));
        }
        return LightTexture.pack(block,sky);
    }
}

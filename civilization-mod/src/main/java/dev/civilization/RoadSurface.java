package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/** A contact-surface bonus, without timers, saved buffs or changes to friction. */
public final class RoadSurface {
    public static final double BONUS = 1.25;
    public static final TagKey<Block> BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("civilization:road_paving"));

    public static boolean supports(Player player) {
        if (!player.onGround() || player.getAbilities().flying || player.isPassenger() || player.isInWater()) return false;
        var pos = player.getOnPos();
        var level = player.level();
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.is(BLOCKS)) return true;
        if (!(state.getBlock() instanceof CutBlock) || !(level.getBlockEntity(pos) instanceof CutBlockEntity cut)) return false;
        var feet = player.getBoundingBox();
        var cells = cut.cells();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null || !cells[i].is(BLOCKS)) continue;
            var box = CutCells.box(i).move(pos);
            if (Math.abs(box.maxY - feet.minY) < .001 && box.maxX > feet.minX && box.minX < feet.maxX
                    && box.maxZ > feet.minZ && box.minZ < feet.maxZ) return true;
        }
        return false;
    }
}

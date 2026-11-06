package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A controller block entity whose machine is one native model built in sections. */
public interface ModeledController {
    /** The machine's model, or null for a controller of this type that has none, such as a plain tank. */
    MachineModel model();
    ModeledBuild build();
    Direction front();
    boolean stillValid(Player player);
    /** Whether the machine is running, which plays its work animation and shows its lit bones. */
    boolean working();
    /** How many of the model's `float_<n>` pieces are afloat in its water, such as a Tannery's steeping hides. */
    default int afloat(){return 0;}
    /** Sections were added or removed: the machine stops until it next checks itself. */
    void sectionsChanged();
    default BlockEntity entity(){return (BlockEntity)this;}
}

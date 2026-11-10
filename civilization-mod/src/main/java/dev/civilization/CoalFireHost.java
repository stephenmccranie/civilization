package dev.civilization;

import net.minecraft.world.Container;

/** The fuel slots and masonry of one coal-fired controller. */
public interface CoalFireHost extends Container {
    int[] coalFuelSlots();
    /** Blocks of masonry the fire heats, which set how much heat the machine stores. */
    int masonry();
    /** Fraction of industrial waste heat emitted into the local thermal field. */
    default double coalWasteHeatFactor(){return 1;}
    /** Whether the fire burns in the open, as the Smithy's forge does, so rain keeps it from lighting and working. A
     *  fire behind a door ignores rain. */
    default boolean openFire(){return false;}
}

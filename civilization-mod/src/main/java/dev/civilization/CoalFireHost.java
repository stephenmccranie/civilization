package dev.civilization;

import net.minecraft.world.Container;

/** The fuel slots and masonry of one coal-fired controller. */
public interface CoalFireHost extends Container {
    int[] coalFuelSlots();
    /** Blocks of masonry the fire heats, which set how much heat the machine stores. */
    int masonry();
    /** Fraction of industrial waste heat emitted into the local thermal field. */
    default double coalWasteHeatFactor(){return 1;}
    default boolean coalFireIgnoresRain(){return false;}
}

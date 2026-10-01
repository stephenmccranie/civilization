package dev.civilization;

import net.minecraft.world.Container;

/** The fuel reserve and slots owned by one coal-fired controller. */
public interface CoalFireHost extends Container {
    int fireHeat();
    void fireHeat(int value);
    int coalCapacity();
    int coalBudget();
    int[] coalFuelSlots();
    /** Fraction of industrial waste heat emitted into the local thermal field. */
    default double coalWasteHeatFactor(){return 1;}
    default boolean coalFireIgnoresRain(){return false;}
}

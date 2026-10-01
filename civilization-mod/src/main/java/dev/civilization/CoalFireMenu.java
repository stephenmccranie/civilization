package dev.civilization;

/** Synchronized fire state used by the shared controller strike button. */
public interface CoalFireMenu {
    int fireState();
    int coalRemaining();
    default boolean hasCoalFire(){return true;}
}

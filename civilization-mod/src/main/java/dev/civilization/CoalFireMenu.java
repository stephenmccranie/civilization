package dev.civilization;

/** Synchronized fire state used by the shared controller strike button. */
public interface CoalFireMenu {
    int fireState();
    /** Stored heat in thousandths of full. */
    int heat();
    default boolean hot(){return heat()>=Math.round(CoalFire.WORKING*1000);}
    /** Whole minutes the heat lasts unfed. */
    default int minutesHot(){return fireState()>>6;}
    default boolean hasCoalFire(){return true;}
}

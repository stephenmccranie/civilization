package dev.civilization;

/** Stable menu wire values shared by server work and client descriptions. */
public final class MachineStatus {
    private MachineStatus(){}
    public static final class Workshop {
        private Workshop(){}
        public static final int INCOMPLETE=0, NEEDS_MATERIALS=1, OUTPUT_FULL=2, FIRE_UNLIT=3,
                WORKING=4, AMBIGUOUS=5, WET=6, HAND_READY=7, NEEDS_FOOD=8, WARMING=9, POUR=10, HOT_METAL=11;
    }
    public static final class Industry {
        private Industry(){}
        public static final int READY=0, INCOMPLETE=1, WRONG_SITE=2, DEPLETED=3,
                OUTPUT_FULL=4, NEEDS_FUEL=5, NEEDS_INPUT=6, WORKING=7,
                UNLOADED=8, CLAIM_BLOCKED=9, SEARCHING=10, FIRE_UNLIT=11, WET=12, WARMING=13,
                NEEDS_CASING=14, BLOCKED=15, NO_ENGINE=16, NEEDS_POWER=17;
    }
}

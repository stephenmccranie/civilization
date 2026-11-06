package dev.civilization;

import net.minecraft.world.inventory.Slot;

public interface BlockCarryMenuAccess {
    Slot civilization$addCarrySlot(Slot slot);
    NearbyStores civilization$nearby();
    void civilization$setNearby(NearbyStores nearby);
}

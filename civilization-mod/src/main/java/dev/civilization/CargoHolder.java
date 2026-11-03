package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface CargoHolder {
    /** How far a player reaches a store to open it, and a parked wheelbarrow the pallet it loads from or unloads to. */
    int REACH=6;
    CargoInventory cargo();
    Level cargoLevel();
    Vec3 cargoPosition();
    /** Whether the player may use this store from up to `reach` blocks away. */
    boolean cargoValid(Player player,double reach);
    default boolean cargoValid(Player player){return cargoValid(player,REACH);}
    Component cargoName();
}

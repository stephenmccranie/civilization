package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface CargoHolder {
    CargoInventory cargo();
    Level cargoLevel();
    Vec3 cargoPosition();
    boolean cargoValid(Player player);
    Component cargoName();
}

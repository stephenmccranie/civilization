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
    /** A right-click puts the first held goods the store accepts onto it, main hand first. False when nothing fits, and the click opens the store. */
    default boolean putHeld(Player p){
        for(var hand:net.minecraft.world.InteractionHand.values()){
            var held=p.getItemInHand(hand);var name=held.getHoverName();
            int n=(int)cargo().insert(held,held.getCount());
            if(n>0){held.shrink(n);p.displayClientMessage(Component.literal("Stacked "+n+" ").append(name).append(" · "+CargoInventory.blocks(cargo().load(),cargo().capacity())),true);return true;}
        }
        return false;
    }
}

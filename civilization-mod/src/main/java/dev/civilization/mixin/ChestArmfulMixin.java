package dev.civilization.mixin;

import dev.civilization.ChestArmful;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Hoppers, droppers and every item handler ask the chest itself whether a stack fits its armful. */
@Mixin({ChestBlockEntity.class,BarrelBlockEntity.class,ChestBoat.class,MinecartChest.class})
public abstract class ChestArmfulMixin {
    public boolean canPlaceItem(int slot,ItemStack stack){return ChestArmful.takes((Container)(Object)this,slot,stack,stack.getCount());}
}

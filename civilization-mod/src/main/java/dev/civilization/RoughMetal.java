package dev.civilization;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/** Metal as the Foundry leaves it, beaten into its ingot by one hammering while hot. */
public final class RoughMetal extends Item {
    private final Supplier<? extends Item> ingot;

    RoughMetal(Supplier<? extends Item> ingot) {
        super(new Item.Properties());
        this.ingot = ingot;
    }

    public Item ingot() { return ingot.get(); }
}

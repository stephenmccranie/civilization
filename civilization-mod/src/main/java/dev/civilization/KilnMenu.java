package dev.civilization;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class KilnMenu extends AbstractFurnaceMenu {
    private final ContainerData structureData;
    private final ContainerData operatingData;
    public KilnMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(3), new SimpleContainerData(4)); }
    public KilnMenu(int id, Inventory inventory, Container container, ContainerData data) {
        this(KilnContent.MENU.get(), KilnContent.RECIPE_TYPE.get(), id, inventory, container, data);
    }
    public static KilnMenu retort(int id, Inventory inventory) {
        return new KilnMenu(KilnContent.RETORT_MENU.get(), KilnContent.RETORT_RECIPE_TYPE.get(), id, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }
    public static KilnMenu cooking(int id, Inventory inventory) {
        return new KilnMenu(CookingContent.MENU.get(), CookingContent.RECIPE_TYPE.get(), id, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }
    public KilnMenu(MenuType<KilnMenu> type, net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipes,
                    int id, Inventory inventory, Container container, ContainerData data) {
        super(type, recipes, RecipeBookType.FURNACE, id, inventory, container, data);
        structureData = container instanceof KilnBlockEntity machine ? new ContainerData() {
            public int get(int index) { return machine.structureStatus(); }
            public void set(int index, int value) {}
            public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(structureData);
        operatingData = container instanceof KilnBlockEntity machine ? new ContainerData() {
            public int get(int index) { return machine.operatingStatus(); }
            public void set(int index, int value) {}
            public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(operatingData);
        // Vanilla's furnace fuel slot also admits empty buckets. This machine has no liquid fuel.
        var fuel = new Slot(container, 1, 56, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return KilnBlockEntity.acceptsFuel(stack); }
        };
        fuel.index = 1;
        slots.set(1, fuel);
    }
    public String tooltipPrefix() {
        return getType() == CookingContent.MENU.get() ? "cooking" : getType() == KilnContent.RETORT_MENU.get() ? "retort" : "kiln";
    }
    public boolean requiresStructure() { return getType() != CookingContent.MENU.get(); }
    public int operatingStatus() { return operatingData.get(0); }
    public int structureStatus() { return structureData.get(0); }
    @Override protected boolean isFuel(ItemStack stack) { return KilnBlockEntity.acceptsFuel(stack); }
}

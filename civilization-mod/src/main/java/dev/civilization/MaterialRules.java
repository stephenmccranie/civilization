package dev.civilization;

import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Registry defaults apply equally to crafting, containers, hoppers and both network sides. */
public final class MaterialRules {
    private MaterialRules() {}

    public static void components(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(item -> Integer.valueOf(64).equals(item.components().get(DataComponents.MAX_STACK_SIZE)),
                patch -> patch.set(DataComponents.MAX_STACK_SIZE, 32));
    }
}

package dev.civilization;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Audited first material family. Unsupported items are unknown, never declared weightless. */
public final class StoneCargo {
    private StoneCargo() {}

    public record Sample(CargoVolume materialVolume, CargoVolume storageVolume, double massKg) {}
    public record InventorySample(CargoVolume materialVolume, CargoVolume storageVolume,
                                  double massKg, int supportedStacks, int unknownStacks) {}

    public static Optional<Sample> sample(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (stack.is(CuttingContent.ITEM.get())) {
            String id = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("material");
            if (!id.equals("minecraft:stone") && !id.equals("minecraft:cobblestone")) return Optional.empty();
        }
        var material = CuttingContent.material(stack);
        if (!material.is(Blocks.STONE) && !material.is(Blocks.COBBLESTONE)) return Optional.empty();
        int eighths = CuttingContent.piece(stack)
                ? switch (CuttingContent.units(stack)) { case 1 -> 2; case 3 -> 1; default -> 4; }
                : 8;
        var volume = CargoVolume.pieces(stack.getCount(), eighths);
        // Neatly packed construction pieces initially use their solid volume. Loose rubble is not modeled.
        return Optional.of(new Sample(volume, volume,
                volume.cubicMeters() * PhysicalMaterials.of(material).density()));
    }

    public static InventorySample inspect(Container inventory) {
        var material = CargoVolume.ZERO;
        var storage = CargoVolume.ZERO;
        double mass = 0;
        int supported = 0, unknown = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            var sample = sample(stack);
            if (sample.isEmpty()) { unknown++; continue; }
            var value = sample.get();
            material = material.plus(value.materialVolume());
            storage = storage.plus(value.storageVolume());
            mass += value.massKg();
            supported++;
        }
        return new InventorySample(material, storage, mass, supported, unknown);
    }
}

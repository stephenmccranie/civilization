package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Gold's high enchantability remains vanilla; its work stats sit between iron and diamond. */
final class GoldEquipment {
    private GoldEquipment() {}

    static void modify(ModifyDefaultComponentsEvent event) {
        Item[] gold = {Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD};
        Item[] iron = {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD};
        Item[] diamond = {Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD};
        for (int i = 0; i < gold.length; i++) {
            Item middle = gold[i], lower = iron[i], upper = diamond[i];
            Tool ironTool = lower.components().get(DataComponents.TOOL);
            ItemAttributeModifiers ironStats = lower.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            ItemAttributeModifiers diamondStats = upper.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            event.modify(middle, patch -> {
                patch.set(DataComponents.MAX_DAMAGE, 600);
                if (ironTool != null) {
                    List<Tool.Rule> rules = new ArrayList<>();
                    for (Tool.Rule rule : ironTool.rules()) rules.add(new Tool.Rule(rule.blocks(), rule.speed().map(speed -> 7.0F), rule.correctForDrops()));
                    patch.set(DataComponents.TOOL, new Tool(rules, ironTool.defaultMiningSpeed(), ironTool.damagePerBlock()));
                }
                if (ironStats != null && diamondStats != null) {
                    var entries = new ArrayList<ItemAttributeModifiers.Entry>();
                    for (int n = 0; n < ironStats.modifiers().size(); n++) {
                        var low = ironStats.modifiers().get(n);
                        var high = diamondStats.modifiers().get(n);
                        var modifier = low.modifier();
                        entries.add(new ItemAttributeModifiers.Entry(low.attribute(), new AttributeModifier(
                                modifier.id(), (modifier.amount() + high.modifier().amount()) / 2.0,
                                modifier.operation()), low.slot()));
                    }
                    patch.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.copyOf(entries), ironStats.showInTooltip()));
                }
            });
        }
        armor(event, Items.GOLDEN_HELMET, ArmorItem.Type.HELMET, 264, 2.5);
        armor(event, Items.GOLDEN_CHESTPLATE, ArmorItem.Type.CHESTPLATE, 384, 7);
        armor(event, Items.GOLDEN_LEGGINGS, ArmorItem.Type.LEGGINGS, 360, 5.5);
        armor(event, Items.GOLDEN_BOOTS, ArmorItem.Type.BOOTS, 312, 2.5);
    }

    private static void armor(ModifyDefaultComponentsEvent event, Item item, ArmorItem.Type type, int durability, double defense) {
        var slot = EquipmentSlotGroup.bySlot(type.getSlot());
        var id = ResourceLocation.withDefaultNamespace("armor." + type.getName());
        var stats = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, defense, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, 1, AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
        event.modify(item, patch -> {
            patch.set(DataComponents.MAX_DAMAGE, durability);
            patch.set(DataComponents.ATTRIBUTE_MODIFIERS, stats);
        });
    }
}

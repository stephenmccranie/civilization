package dev.civilization;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One manufacturing roll; repairs lower grade while ordinary damage remains separate. */
@EventBusSubscriber(modid = Civilization.MOD_ID)
public final class EquipmentGrade {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Civilization.MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> GRADE =
            COMPONENTS.registerComponentType("equipment_grade", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Long>> WEAR_SEED =
            COMPONENTS.registerComponentType("equipment_wear_seed", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    private EquipmentGrade() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
    public static boolean eligible(ItemStack stack) { return !stack.isEmpty() && stack.isDamageableItem(); }
    public static int value(ItemStack stack) { return stack.getOrDefault(GRADE.get(), 100); }
    public static boolean hasGrade(ItemStack stack) { return stack.has(GRADE.get()); }
    public static long wearSeed(ItemStack stack) { return stack.getOrDefault(WEAR_SEED.get(), 0L); }

    /** Whole-percent normal roll, mean 75 and standard deviation 20; the grade itself is uncapped. */
    public static int roll(RandomSource random) { return (int)Math.round(75 + random.nextGaussian() * 20); }

    public static void ensure(ItemStack stack, RandomSource random) {
        if (!eligible(stack)) return;
        if (!hasGrade(stack)) apply(stack, roll(random), random.nextLong());
        else if (!stack.has(WEAR_SEED.get())) stack.set(WEAR_SEED.get(), random.nextLong());
    }

    public static void ensure(ItemStack stack, RandomSource random, HolderLookup.Provider registries) {
        ensure(stack, random);
        updateTrim(stack, registries);
    }

    /** A top-grade armor trim is tied to the item's saved identity, never rerolled on inventory ticks. */
    public static void updateTrim(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.is(ItemTags.TRIMMABLE_ARMOR) || !hasGrade(stack)) return;
        if (value(stack) < 100) { stack.remove(DataComponents.TRIM); return; }
        if (stack.has(DataComponents.TRIM)) return;
        var patterns = registries.lookupOrThrow(Registries.TRIM_PATTERN).listElements()
                .filter(holder -> holder.key().location().getNamespace().equals("minecraft"))
                .sorted(java.util.Comparator.comparing(holder -> holder.key().location().toString())).toList();
        var materials = registries.lookupOrThrow(Registries.TRIM_MATERIAL).listElements()
                .filter(holder -> holder.key().location().getNamespace().equals("minecraft"))
                .sorted(java.util.Comparator.comparing(holder -> holder.key().location().toString())).toList();
        if (patterns.isEmpty() || materials.isEmpty()) return;
        var choice = RandomSource.create(wearSeed(stack));
        stack.set(DataComponents.TRIM, new ArmorTrim(materials.get(choice.nextInt(materials.size())),
                patterns.get(choice.nextInt(patterns.size()))));
    }

    public static void apply(ItemStack stack, int grade) {
        apply(stack, grade, ThreadLocalRandom.current().nextLong());
    }

    public static void apply(ItemStack stack, int grade, long newWearSeed) {
        if (!eligible(stack)) return;
        double factor = grade / 100.0;
        int oldMax = stack.getMaxDamage();
        int oldDamage = stack.getDamageValue();
        int baseMax = stack.getItem().components().getOrDefault(DataComponents.MAX_DAMAGE, oldMax);
        int max = Math.max(1, (int)Math.round(baseMax * factor));
        stack.set(GRADE.get(), grade);
        if (grade < 100 && stack.is(ItemTags.TRIMMABLE_ARMOR)) stack.remove(DataComponents.TRIM);
        if (!stack.has(WEAR_SEED.get())) stack.set(WEAR_SEED.get(), newWearSeed);
        stack.set(DataComponents.MAX_DAMAGE, max);
        stack.setDamageValue(Math.min(max - 1, (int)Math.round(oldDamage * max / (double)Math.max(1, oldMax))));

        Tool baseTool = stack.getItem().components().get(DataComponents.TOOL);
        if (baseTool != null) {
            List<Tool.Rule> rules = new ArrayList<>(baseTool.rules().size());
            // Vanilla's Tool codec requires positive speeds. A nonpositive grade remains on the
            // stack, while the engine receives its smallest representable positive tool speed.
            for (Tool.Rule rule : baseTool.rules()) rules.add(new Tool.Rule(rule.blocks(),
                    rule.speed().map(speed -> (float)Math.max(0.0001, speed * factor)), rule.correctForDrops()));
            stack.set(DataComponents.TOOL, new Tool(rules, (float)Math.max(0.0001, baseTool.defaultMiningSpeed() * factor), baseTool.damagePerBlock()));
        }

        ItemAttributeModifiers base = stack.getItem().components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (base == null || base.modifiers().isEmpty()) base = stack.getItem().getDefaultAttributeModifiers(stack);
        if (base != null && !base.modifiers().isEmpty()) {
            var baseIds = new HashSet<net.minecraft.resources.ResourceLocation>();
            for (var entry : base.modifiers()) baseIds.add(entry.modifier().id());
            var entries = new ArrayList<ItemAttributeModifiers.Entry>();
            var previous = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (previous != null) for (var entry : previous.modifiers())
                if (!baseIds.contains(entry.modifier().id())) entries.add(entry);
            for (var entry : base.modifiers()) {
                var modifier = entry.modifier();
                double amount = modifier.amount() * factor;
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    // Scale the effective player stat, not just Minecraft's negative weapon modifier.
                    if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) amount = (1 + modifier.amount()) * factor - 1;
                    if (entry.attribute().is(Attributes.ATTACK_SPEED)) amount = (4 + modifier.amount()) * factor - 4;
                }
                entries.add(new ItemAttributeModifiers.Entry(entry.attribute(),
                        new AttributeModifier(modifier.id(), amount, modifier.operation()), entry.slot()));
            }
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.copyOf(entries), base.showInTooltip()));
        }
    }

    /** A full repair erodes a weak item quickly; small repairs consume a proportional fraction. */
    public static int afterRepair(ItemStack stack) {
        int grade = value(stack);
        double restored = stack.getDamageValue() / (double)Math.max(1, stack.getMaxDamage());
        double wear = 2 * restored * Math.exp((100.0 - grade) / 25.0);
        return grade - Math.max(1, (int)Math.ceil(wear));
    }

    @SubscribeEvent public static void projectileDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getSource().getDirectEntity() instanceof Projectile projectile)) return;
        ItemStack weapon = projectile.getWeaponItem();
        if (weapon == null || !hasGrade(weapon)
                || !(weapon.getItem() instanceof ProjectileWeaponItem || weapon.is(Items.TRIDENT))) return;
        event.setAmount((float)(event.getAmount() * (value(weapon) / 100.0)));
    }

    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getEntity().level().isClientSide) ensure(event.getCrafting(), event.getEntity().getRandom(), event.getEntity().level().registryAccess());
    }

    @SubscribeEvent public static void inventory(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) ensure(inventory.getItem(i), player.getRandom(), player.level().registryAccess());
    }
}

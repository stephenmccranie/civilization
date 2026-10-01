package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.ArrayList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class FrontierGameTests {
    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void uraniumRegionsIncludeUplandsButNotFarmland(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> uranium = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("civilization", "uranium_regions"));
        for (String name : new String[] { "meadow", "grove", "snowy_slopes", "cherry_grove",
                "windswept_savanna", "savanna_plateau", "old_growth_pine_taiga", "old_growth_spruce_taiga" }) {
            var key = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(name));
            helper.assertTrue(biomes.getHolderOrThrow(key).is(uranium), name + " permits uranium");
        }
        for (String name : new String[] { "plains", "river", "ocean" }) {
            var key = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(name));
            helper.assertTrue(!biomes.getHolderOrThrow(key).is(uranium), name + " stays outside frontier uranium");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void accessoryColumnKeepsVanillaInventoryCoordinates(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = player.inventoryMenu;
        helper.assertTrue(menu.slots.size() == 49, "46 vanilla slots plus three accessory slots");
        for (int i = 0; i < 3; i++) {
            var slot = menu.getSlot(46 + i);
            helper.assertTrue(slot.x == 77 && slot.y == 8 + i * 18,
                    "Accessory should mirror armor above the offhand slot");
            helper.assertTrue(slot.mayPlace(FrontierContent.GEIGER_COUNTER.toStack()), "Counter equips");
            helper.assertTrue(!slot.mayPlace(Items.DIRT.getDefaultInstance()), "Ordinary items cannot equip");
        }
        helper.assertTrue(menu.getSlot(45).x == 77 && menu.getSlot(45).y == 62,
                "Vanilla offhand slot stays in place");
        helper.assertTrue(menu.getSlot(9).y == 84, "Main inventory stays in place");
        var inventory = player.getData(Accessories.INVENTORY);
        inventory.setStackInSlot(1, FrontierContent.GEIGER_COUNTER.toStack());
        menu.clicked(47, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(FrontierContent.GEIGER_COUNTER.get())
                && inventory.getStackInSlot(1).isEmpty(), "Player can take off the accessory");
        menu.clicked(48, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty()
                && inventory.getStackInSlot(2).is(FrontierContent.GEIGER_COUNTER.get()),
                "Player can move the accessory between slots");
        var saved = inventory.serializeNBT(helper.getLevel().registryAccess());
        var loaded = new Accessories.AccessoryInventory();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(loaded.getStackInSlot(2).is(FrontierContent.GEIGER_COUNTER.get()),
                "Equipped item survives serialization");
        player.setData(Accessories.INVENTORY, loaded);
        helper.assertTrue(menu.getSlot(48).getItem().is(FrontierContent.GEIGER_COUNTER.get()),
                "Menu follows the attachment replaced during player save loading");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void geigerSearchFindsOnlyNearbyLoadedOre(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        level.getChunkAt(origin);
        helper.assertTrue(GeigerSurvey.closestOre(level, origin) > 48 * 48, "No false signal");
        BlockPos edge = origin.above(48);
        level.setBlockAndUpdate(edge, FrontierContent.URANIUM_ORE.get().defaultBlockState());
        helper.assertTrue(GeigerSurvey.closestOre(level, origin) == 48 * 48, "Signal reaches the new boundary");
        level.removeBlock(edge, false);
        BlockPos outside = origin.above(49);
        level.setBlockAndUpdate(outside, FrontierContent.URANIUM_ORE.get().defaultBlockState());
        helper.assertTrue(GeigerSurvey.closestOre(level, origin) > 48 * 48, "Signal stops beyond the boundary");
        level.removeBlock(outside, false);
        BlockPos ore = origin.offset(4, 1, 3);
        level.setBlockAndUpdate(ore, FrontierContent.URANIUM_ORE.get().defaultBlockState());
        helper.assertTrue(GeigerSurvey.closestOre(level, origin) == 26, "Signal reports nearest ore distance");
        level.removeBlock(ore, false);
        helper.assertTrue(GeigerSurvey.closestOre(level, origin) > 48 * 48, "Signal stops after extraction");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void accessoryDropsWithOrdinaryDeathLoot(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "accessory-death-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.getData(Accessories.INVENTORY).setStackInSlot(2, FrontierContent.GEIGER_COUNTER.toStack());
        var drops = new ArrayList<ItemEntity>();
        var rule = player.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean previous = rule.get();
        try {
            rule.set(false, helper.getLevel().getServer());
            NeoForge.EVENT_BUS.post(new LivingDropsEvent(player, player.damageSources().generic(), drops, false));
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(FrontierContent.GEIGER_COUNTER.get()),
                "Worn accessory follows ordinary survival drops");
        helper.assertTrue(player.getData(Accessories.INVENTORY).getStackInSlot(2).isEmpty(),
                "Accessory must not also survive as a copied item");
        player.getData(Accessories.INVENTORY).setStackInSlot(2, FrontierContent.GEIGER_COUNTER.toStack());
        var kept = new ArrayList<ItemEntity>();
        try {
            rule.set(true, helper.getLevel().getServer());
            NeoForge.EVENT_BUS.post(new LivingDropsEvent(player, player.damageSources().generic(), kept, false));
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.assertTrue(kept.isEmpty() && player.getData(Accessories.INVENTORY).getStackInSlot(2)
                .is(FrontierContent.GEIGER_COUNTER.get()), "Keep-inventory preserves accessories");
        helper.succeed();
    }
}

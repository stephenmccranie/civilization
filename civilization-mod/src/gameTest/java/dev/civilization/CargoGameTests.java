package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class CargoGameTests {
    @GameTest(template = "industrial") public static void cutAndNativeStoneItemsConserveVolume(GameTestHelper h) {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.COBBLESTONE}) {
            var full = StoneCargo.sample(new ItemStack(block)).orElseThrow();
            h.assertTrue(full.materialVolume().eighths() == 8, "Full stone has one cubic meter");
            for (int units : new int[]{2, 1, 3}) {
                int count = units == 2 ? 2 : units == 1 ? 4 : 8;
                var pieces = StoneCargo.sample(CuttingContent.stack(block.defaultBlockState(), units, count)).orElseThrow();
                h.assertTrue(full.equals(pieces), "Saw cutting preserves volume, packed storage and material mass");
            }
        }
        h.assertTrue(StoneCargo.sample(new ItemStack(Items.STONE_SLAB, 2)).orElseThrow().materialVolume().eighths() == 8,
                "Native slabs use the same quantity rule");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void mixedInventoryReportsUnknownsWithoutEditingStock(GameTestHelper h) {
        var stock = new SimpleContainer(new ItemStack(Items.STONE, 3), new ItemStack(Items.COBBLESTONE_SLAB, 3),
                new ItemStack(Items.DIAMOND, 10), CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(), 3, 2));
        var before = stock.getItem(0).copy();
        var sample = StoneCargo.inspect(stock);
        h.assertTrue(sample.materialVolume().eighths() == 36 && sample.storageVolume().eighths() == 36,
                "Mixed supported quantities sum exactly");
        h.assertTrue(sample.supportedStacks() == 2 && sample.unknownStacks() == 2, "Unsupported items remain explicitly unknown");
        h.assertTrue(sample.massKg() == 4.5 * PhysicalMaterials.of(Blocks.STONE.defaultBlockState()).density(),
                "Cargo uses the shared current material density");
        h.assertTrue(ItemStack.matches(before, stock.getItem(0)) && stock.getItem(2).getCount() == 10,
                "Inspection does not consume or alter stock");
        h.assertTrue(StoneCargo.sample(ItemStack.EMPTY).isEmpty()
                && StoneCargo.sample(new ItemStack(CuttingContent.ITEM.get())).isEmpty(),
                "Empty or malformed custom pieces cannot become known stone through a fallback");
        stock.setItem(0, new ItemStack(Items.STONE, 1));
        h.assertTrue(StoneCargo.inspect(stock).materialVolume().eighths() == 20, "Edits cannot leave cached cargo totals");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void cargoCommandsAuditLoadedStockAndRequireOperator(GameTestHelper h)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(5, 4, 5));
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.STONE, 12));
        var source = level.getServer().createCommandSourceStack().withLevel(level);
        var dispatcher = level.getServer().getCommands().getDispatcher();
        String command = "civilization cargo container " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        h.assertTrue(dispatcher.execute(command, source) == 1 && chest.getItem(0).getCount() == 12,
                "Actual command reports a loaded container without consuming it");
        boolean rejected = false;
        try { dispatcher.execute(command, source.withPermission(0)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { rejected = true; }
        h.assertTrue(rejected, "Cargo audit requires operator permission");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "CargoAudit"));
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        h.assertTrue(dispatcher.execute("civilization cargo", source.withEntity(player)) == 1
                && player.getInventory().getItem(0).getCount() == 2, "Player audit executes without changing inventory");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void cargoInspectionDoesNotGenerateLootOrLoadChunks(GameTestHelper h) {
        var level = h.getLevel();
        var source = level.getServer().createCommandSourceStack().withLevel(level);
        var pos = h.absolutePos(new BlockPos(5, 4, 5));
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        var loot = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
        chest.setLootTable(loot);
        h.assertTrue(CargoInspector.inspectContainer(source, pos) == 0 && chest.getLootTable().equals(loot),
                "Unopened loot remains unopened");
        var unloaded = new BlockPos(29_000_000, 64, 29_000_000);
        h.assertTrue(!level.hasChunkAt(unloaded), "Fixture target is unloaded");
        h.assertTrue(CargoInspector.inspectContainer(source, unloaded) == 0 && !level.hasChunkAt(unloaded),
                "Unloaded audit target does not create a chunk ticket");
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        h.assertTrue(CargoInspector.inspectContainer(source, pos) == 0, "Non-container fails clearly");
        h.succeed();
    }
}

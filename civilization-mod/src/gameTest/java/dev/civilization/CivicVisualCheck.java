package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

/** Disposable menu/network fixture; never included in the production JAR. */
final class CivicVisualCheck {
    private static int ticks;
    private static final BlockPos LAND = new BlockPos(12, 101, 0), SHOP = new BlockPos(14, 101, 0);
    static void tick(Minecraft mc) {
        ticks++; var server = mc.getSingleplayerServer();
        if (ticks == 100) {
            mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(3); mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(() -> {
                var level = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                p.getInventory().clearContent(); p.getInventory().setItem(7,CivicContent.LAND_ITEM.toStack()); p.getInventory().setItem(8,CivicContent.SHOP_ITEM.toStack());
                level.setDayTime(6000);
                p.teleportTo(level, 13, 101, -2, java.util.Set.of(), 0, 0);
                level.setBlockAndUpdate(LAND.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(SHOP.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(LAND, CivicContent.LAND.get().defaultBlockState());
                level.setBlockAndUpdate(SHOP, CivicContent.SHOP.get().defaultBlockState());
                CivicService.placed(level, LAND, p, true); CivicService.placed(level, SHOP, p, false);
                var d = CivicData.get(server); var claim = d.claims.get(CivicService.address(level, LAND));
                claim.owner = CivicService.personal(p); claim.energy = claim.coalUnit * 64; claim.lastUpdate = System.currentTimeMillis();
                var shop = d.shops.get(CivicService.address(level, SHOP)); shop.owner = CivicService.personal(p); shop.template = Items.BREAD.getDefaultInstance(); shop.amount = 4; shop.price = 2; shop.inventory.set(0, KilnContent.MINERAL_COAL.toStack(16)); shop.inventory.set(1, Items.BREAD.getDefaultInstance().copyWithCount(8));
                CivicMenu.open(p, claim.at, true);
            });
        }
        if (ticks == 160) {
            if (mc.player.containerMenu.getType() != CivicContent.LAND_MENU.get() || !mc.player.containerMenu.getSlot(0).hasItem()) throw new IllegalStateException("Claim menu did not synchronize");
            shot(mc, "land");
            mc.screen.mouseClicked(mc.getWindow().getGuiScaledWidth() / 2.0 + 100, (mc.getWindow().getGuiScaledHeight() - 168) / 2.0 + 18, 0);
        }
        if (ticks == 180) {
            shot(mc, "whitelist");
            server.execute(() -> { var p = server.getPlayerList().getPlayers().getFirst(); CivicMenu.open(p, CivicService.address(server.overworld(), SHOP), false); });
        }
        if (ticks == 190) {
            var field=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();
            field.setFocused(true); field.setValue("65"); mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
        }
        if (ticks == 200) {
            if (!(mc.player.containerMenu instanceof CivicMenu m) || m.amount()!=65) throw new IllegalStateException("Typed quantity did not synchronize");
            if (mc.player.containerMenu.getType() != CivicContent.SHOP_MENU.get() || !mc.player.containerMenu.getSlot(27).getItem().is(Items.BREAD)) throw new IllegalStateException("Shop offer did not synchronize");
            shot(mc, "shop");
        }
        if (ticks == 205) {
            // Real client click path: pick stock onto cursor, set a ghost outside the chest bounds.
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 1, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
        }
        if (ticks == 215) {
            double x = mc.getWindow().getGuiScaledWidth()/2.0 - 88 + 222;
            double y = (mc.getWindow().getGuiScaledHeight()-168)/2.0 + 40;
            mc.screen.mouseClicked(x,y,0); mc.screen.mouseReleased(x,y,0);
        }
        if (ticks == 220) {
            double x = mc.getWindow().getGuiScaledWidth()/2.0 - 88 + 246;
            double y = (mc.getWindow().getGuiScaledHeight()-168)/2.0 + 58;
            mc.screen.mouseClicked(x,y,0); mc.screen.mouseReleased(x,y,0);
        }
        if (ticks == 230) {
            if (!mc.player.containerMenu.getSlot(28).getItem().is(Items.BREAD) || mc.player.containerMenu.getCarried().getCount()!=8) throw new IllegalStateException("Real ghost click consumed cursor or failed to configure");
            shot(mc,"ghost-click");
        }
        if (ticks == 240) server.execute(() -> {
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();
            var at=new BlockPos(12,101,-4);var level=server.overworld();level.setBlockAndUpdate(at,CivicContent.TABLE.get().defaultBlockState());
            for(var pos:SurveyTable.positions(level,at))if(!pos.equals(at))level.setBlockAndUpdate(pos,CivicContent.TABLE_PART.get().defaultBlockState());
            var table=(SurveyBlockEntity)level.getBlockEntity(at);table.sample(4096);table.complete=true;
            SurveyMenu.open(p,at);

        });
        if(ticks==260){
            if(!(mc.player.containerMenu instanceof SurveyMenu m)||m.snapshot==null||m.snapshot.markers().size()<2)throw new IllegalStateException("Survey table did not synchronize");
            mc.screen.mouseClicked((mc.getWindow().getGuiScaledWidth()-416)/2.0+140,(mc.getWindow().getGuiScaledHeight()-304)/2.0+154,0);
        }
        if(ticks==270)shot(mc,"map");
        if (ticks == 280) server.execute(() -> {
            var p = server.getPlayerList().getPlayers().getFirst(); p.closeContainer(); var d = CivicData.get(server);
            
            for(int x=10;x<=16;x++) for(int z=-1;z<=2;z++) server.overworld().setBlockAndUpdate(new BlockPos(x,100,z),net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
            p.teleportTo(server.overworld(),17,103,-5,java.util.Set.of(),38,26);
        });
        if (ticks == 285) mc.options.hideGui=true;
        if (ticks == 315) shot(mc,"blocks");
        if(ticks==320)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),14,104,-7,java.util.Set.of(),20,49);});
        if(ticks==345){var table=mc.level.getBlockEntity(new BlockPos(12,101,-4));if(!(table instanceof SurveyBlockEntity t)||!t.complete||t.markers.size()<2||java.util.Arrays.stream(t.terrain).allMatch(c->c==0))throw new IllegalStateException("In-world table survey not synchronized");shot(mc,"table");}
        if(ticks==350)server.execute(()->{var d=CivicData.get(server);d.claims.remove(CivicService.address(server.overworld(), LAND)); d.shops.remove(CivicService.address(server.overworld(), SHOP)); d.rebuildIndex(); d.setDirty();server.overworld().removeBlock(new BlockPos(13,101,-3),false);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),12.5,102,-6,java.util.Set.of(),0,30);});
        if(ticks==355)mc.options.hideGui=false;
        if(ticks==375)shot(mc,"table-guide");
        if (ticks > 385) mc.stop();
    }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "civic-" + name + ".png", mc.getMainRenderTarget(), message -> com.mojang.logging.LogUtils.getLogger().info("{}", message.getString())); }
}

package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Native 3x3 crafting, recipe-book positioning, bulk crafting onto a nearby Pallet and a shared enchanting table in a disposable hidden client. */
final class CraftingVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.hideGui=false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{
                var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();
                var at=new BlockPos(0,100,0);l.setBlockAndUpdate(at,Blocks.CRAFTING_TABLE.defaultBlockState());p.teleportTo(l,.5,101,.5,java.util.Set.of(),0,0);
                p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
                for(int i=1;i<=9;i++)if(i!=5)p.containerMenu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS,2));
                p.containerMenu.broadcastChanges();
            });
        }
        if(ticks==135){if(!(mc.screen instanceof CraftingScreen s))throw new IllegalStateException("Crafting table did not open");if(s.getRecipeBookComponent().isVisible())toggle(mc);}
        if(ticks==145){if(!mc.player.containerMenu.getSlot(0).getItem().is(Items.CHEST))throw new IllegalStateException("3x3 recipe missing");shot(mc,"closed");click(mc,0);}
        if(ticks==155)click(mc,46);
        if(ticks==170){
            if(!mc.player.getOffhandItem().is(Items.CHEST)||!mc.player.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Crafting output custody failed");
            for(int i=1;i<=9;i++)if(i!=5&&mc.player.containerMenu.getSlot(i).getItem().getCount()!=1)throw new IllegalStateException("Crafting consumed wrong quantity");
            toggle(mc);
        }
        if(ticks==185){if(!((CraftingScreen)mc.screen).getRecipeBookComponent().isVisible())throw new IllegalStateException("Recipe book failed to open");shot(mc,"recipes");}
        if(ticks==190)mc.screen.onClose();
        if(ticks==200)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var at=new BlockPos(0,100,0);
            p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
        });
        if(ticks==225){
            if(!(mc.screen instanceof CraftingScreen)||!mc.player.containerMenu.getSlot(0).getItem().is(Items.CHEST))throw new IllegalStateException("Shared crafting result missing on reopen");
            for(int i=1;i<=9;i++)if(i!=5&&mc.player.containerMenu.getSlot(i).getItem().getCount()!=1)throw new IllegalStateException("Table lost ingredients on close/reopen");
            shot(mc,"reopened");
        }
        if(ticks==230)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var at=new BlockPos(0,100,0);p.closeContainer();
            var store=new BlockPos(2,100,0);l.setBlockAndUpdate(store,CargoContent.STOCKPILE.get().defaultBlockState());((StockpileEntity)l.getBlockEntity(store)).cargo().insert(new ItemStack(Items.STONE),10);
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,BlockCarry.LIMIT));
            p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
            for(int i=1;i<=9;i++)p.containerMenu.getSlot(i).set(i==1?new ItemStack(Items.OAK_LOG,6):ItemStack.EMPTY);
            p.containerMenu.broadcastChanges();
        });
        if(ticks==260){
            if(!(mc.screen instanceof CraftingScreen)||!mc.player.containerMenu.getSlot(0).getItem().is(Items.OAK_PLANKS))throw new IllegalStateException("Planks recipe missing beside the Pallet");
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        }
        if(ticks==285){
            var near=NearbyStores.of(mc.player.containerMenu);int planks=near==null?-1:near.indexOf(new ItemStack(Items.OAK_PLANKS));
            if(planks<0||near.count(planks)!=6||!mc.player.containerMenu.getSlot(1).getItem().isEmpty()||mc.player.getOffhandItem().getCount()!=BlockCarry.LIMIT)
                throw new IllegalStateException("Shift-clicked planks did not all go onto the Pallet");
            shot(mc,"bulk");
        }
        if(ticks==290)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var at=new BlockPos(0,100,-2);p.closeContainer();
            l.setBlockAndUpdate(at,Blocks.ENCHANTING_TABLE.defaultBlockState());
            p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
            p.containerMenu.getSlot(0).set(new ItemStack(Items.BOOK));p.containerMenu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,12));
            p.closeContainer();
        });
        if(ticks==305)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var at=new BlockPos(0,100,-2);
            p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
        });
        if(ticks==330){
            if(!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.EnchantmentScreen)||!mc.player.containerMenu.getSlot(0).getItem().is(Items.BOOK)
                    ||mc.player.containerMenu.getSlot(1).getItem().getCount()!=12||mc.player.getInventory().countItem(Items.LAPIS_LAZULI)!=0)
                throw new IllegalStateException("Enchanting table lost its book or lapis on close/reopen");
            shot(mc,"enchanting");
        }
        if(ticks>335){com.mojang.logging.LogUtils.getLogger().info("CRAFTING UI VERIFIED");mc.stop();}
    }
    private static void toggle(Minecraft mc){var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.ImageButton).map(c->(net.minecraft.client.gui.components.ImageButton)c).findFirst().orElseThrow();mc.screen.mouseClicked(b.getX()+5,b.getY()+5,0);mc.screen.mouseReleased(b.getX()+5,b.getY()+5,0);}
    private static void click(Minecraft mc,int index){var s=(CraftingScreen)mc.screen;var slot=mc.player.containerMenu.getSlot(index);double x=s.getGuiLeft()+slot.x+8,y=s.getGuiTop()+slot.y+8;s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"crafting-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

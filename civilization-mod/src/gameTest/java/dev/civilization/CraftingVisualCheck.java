package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Native 3x3 crafting and recipe-book positioning in a disposable hidden client. */
final class CraftingVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.hideGui=false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{
                var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.getInventory().clearContent();
                var at=new BlockPos(0,100,0);l.setBlockAndUpdate(at,Blocks.CRAFTING_TABLE.defaultBlockState());p.teleportTo(l,.5,101,.5,java.util.Set.of(),0,0);
                p.openMenu(l.getBlockState(at).getMenuProvider(l,at));
                for(int i=1;i<=9;i++)if(i!=5)p.containerMenu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS,2));
                p.containerMenu.broadcastChanges();
            });
        }
        if(ticks==135){if(!(mc.screen instanceof CraftingScreen s))throw new IllegalStateException("Crafting table did not open");if(s.getRecipeBookComponent().isVisible())toggle(mc);}
        if(ticks==145){if(!mc.player.containerMenu.getSlot(0).getItem().is(Items.CHEST))throw new IllegalStateException("3x3 recipe missing");shot(mc,"closed");click(mc,0);}
        if(ticks==155)click(mc,10);
        if(ticks==170){
            if(!mc.player.containerMenu.getSlot(10).getItem().is(Items.CHEST)||!mc.player.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Crafting output custody failed");
            for(int i=1;i<=9;i++)if(i!=5&&mc.player.containerMenu.getSlot(i).getItem().getCount()!=1)throw new IllegalStateException("Crafting consumed wrong quantity");
            toggle(mc);
        }
        if(ticks==185){if(!((CraftingScreen)mc.screen).getRecipeBookComponent().isVisible())throw new IllegalStateException("Recipe book failed to open");shot(mc,"recipes");}
        if(ticks>190){com.mojang.logging.LogUtils.getLogger().info("CRAFTING UI VERIFIED");mc.stop();}
    }
    private static void toggle(Minecraft mc){var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.ImageButton).map(c->(net.minecraft.client.gui.components.ImageButton)c).findFirst().orElseThrow();mc.screen.mouseClicked(b.getX()+5,b.getY()+5,0);mc.screen.mouseReleased(b.getX()+5,b.getY()+5,0);}
    private static void click(Minecraft mc,int index){var s=(CraftingScreen)mc.screen;var slot=mc.player.containerMenu.getSlot(index);double x=s.getGuiLeft()+slot.x+8,y=s.getGuiTop()+slot.y+8;s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"crafting-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

package dev.civilization.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Actual Creative mouse clicks exercise its slot-count-dependent packet mapping. */
public final class CreativeInventoryCheck {
    private static volatile ItemStack picked=ItemStack.EMPTY;
    private static ItemStack[] sentinels(){return new ItemStack[]{ItemStack.EMPTY,new ItemStack(Items.STONE,32),new ItemStack(Items.DIRT,32),new ItemStack(Items.OAK_PLANKS,32),new ItemStack(Items.DIAMOND,4),new ItemStack(Items.APPLE,5),new ItemStack(Items.STICK,6),new ItemStack(Items.IRON_INGOT,7),new ItemStack(Items.STRING,8)};}
    public static void seed(Player p){p.getInventory().clearContent();var items=sentinels();for(int i=0;i<9;i++)p.getInventory().setItem(i,items[i]);}
    public static void open(Minecraft mc){
        mc.setScreen(new CreativeModeInventoryScreen(mc.player,mc.getConnection().enabledFeatures(),false));
        try{
            var select=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(mc.screen,CreativeModeTabs.getDefaultTab());
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        var screen=(CreativeModeInventoryScreen)mc.screen;
        if(screen.getMenu().slots.size()!=54)throw new IllegalStateException("Creative catalog slot count changed: "+screen.getMenu().slots.size());
        picked=screen.getMenu().getSlot(0).getItem().copyWithCount(1);
        click(mc,0,0);
        if(!ItemStack.matches(screen.getMenu().getCarried(),picked))throw new IllegalStateException("Catalog click did not pick exactly one block");
    }
    public static void click(Minecraft mc,int index,int button){
        var screen=(CreativeModeInventoryScreen)mc.screen;var slot=screen.getMenu().getSlot(index);
        double x=screen.getGuiLeft()+slot.x+8,y=screen.getGuiTop()+slot.y+8;
        screen.mouseClicked(x,y,button);screen.mouseReleased(x,y,button);
    }
    public static void verify(Player p,boolean swapped){
        if(!p.isCreative())throw new IllegalStateException("Creative exemption fixture lost game mode");
        var expected=sentinels();expected[0]=picked.copy();if(swapped){var stone=expected[1];expected[1]=expected[3];expected[3]=stone;}
        for(int i=0;i<9;i++)if(!ItemStack.matches(expected[i],p.getInventory().getItem(i)))throw new IllegalStateException("Creative hotbar changed unexpectedly at "+i+": expected="+expected[i]+" actual="+p.getInventory().getItem(i));
        if(!p.getOffhandItem().isEmpty())throw new IllegalStateException("Creative blocks were moved into offhand");
    }
}

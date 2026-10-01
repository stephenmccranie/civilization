package dev.civilization.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.InventoryMenu;

/** Skin only: vanilla owns equipment, crafting, recipe-book behavior and the live player portrait. */
public final class PlayerInventoryUi {
    private PlayerInventoryUi() {}
    public static void draw(GuiGraphics g,InventoryMenu menu,int x,int y,int width,int height){
        MachineUi.panel(g,x,y,width,height);
        MachineUi.slots(g,menu,x,y);
        // Recessed dressing mirror, behind vanilla's mouse-following character render.
        g.fill(x+25,y+7,x+76,y+79,0xff5b5547);
        g.fill(x+26,y+8,x+75,y+78,0xff343833);
        g.fill(x+27,y+9,x+74,y+77,0xff4d5149);
        // Keep the native localized Crafting label and all coordinates intact.
        g.fill(x+94,y+3,x+165,y+16,0xff645335);
        g.fill(x+95,y+4,x+164,y+15,0xffb49b65);
        g.fill(x+96,y+4,x+163,y+5,0xffd5bd80);
        // A fixed direction arrow, not a fake timed progress meter.
        g.fill(x+135,y+33,x+148,y+38,0xff756a53);
        for(int i=0;i<6;i++)g.fill(x+146+i,y+29+i,x+147+i,y+42-i,0xff756a53);
    }
}

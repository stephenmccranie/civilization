package dev.civilization.client;

import dev.civilization.BlockCarry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Real client layout and mouse actions, including the non-slot hand-well frame. */
public final class CarryUiCheck {
    private static AbstractContainerScreen<?> screen(Minecraft mc){return (AbstractContainerScreen<?>)mc.screen;}
    private static Slot hand(Minecraft mc){return mc.player.containerMenu.slots.stream().filter(s->s.x<0&&s.container==mc.player.getInventory()&&s.getContainerSlot()==BlockCarry.OFFHAND).findFirst().orElseThrow();}
    public static void assertLayout(Minecraft mc,boolean recipes){
        var s=screen(mc);var hand=hand(mc);var bounds=MachineUi.handBounds(s,hand);int x=bounds.getX(),y=bounds.getY();
        if(hand.x!=MachineUi.handSlotX(s)||x<0||y<0||x+bounds.getWidth()>mc.getWindow().getGuiScaledWidth()||y+bounds.getHeight()>mc.getWindow().getGuiScaledHeight())throw new IllegalStateException("Hand well is clipped or differs from its native mouse slot: "+s.getClass()+" / "+x+","+y);
        if(recipes){int boundary=s instanceof MachineScreen<?> machine?machine.recipeDrawerBounds().getX():s.getGuiLeft()-177;
            if(x+bounds.getWidth()>boundary)throw new IllegalStateException("Hand well overlaps the open recipe controls");}
    }
    public static void clickSlot(Minecraft mc,int index){var s=screen(mc);var slot=s.getMenu().getSlot(index);click(mc,s.getGuiLeft()+slot.x+8,s.getGuiTop()+slot.y+8);}
    public static void clickLabel(Minecraft mc){var s=screen(mc);var hand=hand(mc);click(mc,s.getGuiLeft()+hand.x+8,s.getGuiTop()+hand.y-8);}
    private static void click(Minecraft mc,double x,double y){var s=screen(mc);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    public static void openRecipes(Minecraft mc){
        var s=(net.minecraft.client.gui.screens.inventory.CraftingScreen)mc.screen;
        if(!s.getRecipeBookComponent().isVisible())s.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.ImageButton).map(w->(net.minecraft.client.gui.components.ImageButton)w).findFirst().orElseThrow().onPress();
    }
}

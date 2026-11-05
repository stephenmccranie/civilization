package dev.civilization.client;

import dev.civilization.BlockCarry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

/** Real client layout and mouse actions, including the non-slot hand-well frame. */
public final class CarryUiCheck {
    private static AbstractContainerScreen<?> screen(Minecraft mc){return (AbstractContainerScreen<?>)mc.screen;}
    private static Slot hand(Minecraft mc){return mc.player.containerMenu.slots.stream().filter(s->s instanceof BlockCarry.HandSlot).findFirst().orElseThrow();}
    /**
     * The Hand and any Pack panel stand within the window, a gap's width from the menu's side, clear of the menu and of
     * the open recipe panel.
     */
    public static void assertLayout(Minecraft mc,boolean recipes){
        var s=screen(mc);var hand=hand(mc);var bounds=MachineUi.handBounds(s,hand);
        if(MachineUi.recipesOpen(s)!=recipes)throw new IllegalStateException("Recipe panel open is "+MachineUi.recipesOpen(s)+", expected "+recipes);
        if(hand.x!=MachineUi.handSlotX(s))throw new IllegalStateException("Hand well differs from its native mouse slot: "+s.getClass());
        var menu=new Rect2i(s.getGuiLeft(),s.getGuiTop(),s.getXSize(),s.getYSize());
        int reach=Math.max(menu.getX()-bounds.getX()-bounds.getWidth(),bounds.getX()-menu.getX()-menu.getWidth());
        if(reach!=MachineUi.HAND_GAP)throw new IllegalStateException("Hand well stands "+reach+" px from the menu's side");
        var panels=new java.util.ArrayList<Rect2i>(java.util.List.of(bounds));
        if(MachineUi.packLayout(s.getMenu()) instanceof Rect2i pack)panels.add(new Rect2i(s.getGuiLeft()+pack.getX(),s.getGuiTop()+pack.getY(),pack.getWidth(),pack.getHeight()));
        var recipe=!recipes?null:s instanceof MachineScreen<?> machine?machine.recipeDrawerBounds():new Rect2i(s.getGuiLeft()-177,s.getGuiTop(),177,s.getYSize());
        for(var r:panels){
            if(r.getX()<0||r.getY()<0||r.getX()+r.getWidth()>mc.getWindow().getGuiScaledWidth()||r.getY()+r.getHeight()>mc.getWindow().getGuiScaledHeight())throw new IllegalStateException("Hand or Pack panel is clipped: "+s.getClass()+" / "+r.getX()+","+r.getY());
            if(overlap(r,menu))throw new IllegalStateException("Hand or Pack panel overlaps the menu");
            if(recipe!=null&&overlap(r,recipe))throw new IllegalStateException("Hand or Pack panel overlaps the open recipe controls");
        }
    }
    private static boolean overlap(Rect2i a,Rect2i b){return a.getX()<b.getX()+b.getWidth()&&b.getX()<a.getX()+a.getWidth()&&a.getY()<b.getY()+b.getHeight()&&b.getY()<a.getY()+a.getHeight();}
    public static void clickSlot(Minecraft mc,int index){var s=screen(mc);var slot=s.getMenu().getSlot(index);click(mc,s.getGuiLeft()+slot.x+8,s.getGuiTop()+slot.y+8);}
    public static void clickLabel(Minecraft mc){var s=screen(mc);var hand=hand(mc);click(mc,s.getGuiLeft()+hand.x+8,s.getGuiTop()+hand.y-8);}
    private static void click(Minecraft mc,double x,double y){var s=screen(mc);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    public static void openRecipes(Minecraft mc){
        var s=(net.minecraft.client.gui.screens.inventory.CraftingScreen)mc.screen;
        if(!s.getRecipeBookComponent().isVisible())s.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.ImageButton).map(w->(net.minecraft.client.gui.components.ImageButton)w).findFirst().orElseThrow().onPress();
    }
}

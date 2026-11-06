package dev.civilization.client;

import dev.civilization.BlockCarry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

/** Real client layout and mouse actions, including the column's non-slot frames. */
public final class CarryUiCheck {
    private static AbstractContainerScreen<?> screen(Minecraft mc){return (AbstractContainerScreen<?>)mc.screen;}
    private static Slot hand(Minecraft mc){return BlockCarry.handSlot(mc.player.containerMenu);}
    private static boolean recipesOpen(AbstractContainerScreen<?> s){
        return s instanceof MachineScreen<?> machine?machine.recipesOpen()
                :s instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener book&&book.getRecipeBookComponent().isVisible();
    }
    /**
     * The side column stands a gap's width from the menu's right side whether or not a recipe panel is open, centred on
     * the menu's height with the Hand at the bottom, every panel within the window and clear of the menu, the recipe panel and each other.
     */
    public static void assertLayout(Minecraft mc,boolean recipes){
        var s=screen(mc);var hand=hand(mc);var layout=SidePanels.layout(s);
        if(recipesOpen(s)!=recipes)throw new IllegalStateException("Recipe panel open is "+recipesOpen(s)+", expected "+recipes);
        if(layout==null||hand.x!=SidePanels.reach(s)+SidePanels.GAP+SidePanels.HAND_LEFT)throw new IllegalStateException("The Hand left the column on the menu's right: "+s.getClass());
        var menu=new Rect2i(s.getGuiLeft(),s.getGuiTop(),s.getXSize(),s.getYSize());
        var panels=SidePanels.areas(s);
        var recipe=!recipes?null:s instanceof MachineScreen<?> machine?machine.recipeDrawerBounds():new Rect2i(s.getGuiLeft()-177,s.getGuiTop(),177,s.getYSize());
        int top=panels.stream().mapToInt(Rect2i::getY).min().orElseThrow(),bottom=panels.stream().mapToInt(r->r.getY()+r.getHeight()).max().orElseThrow();
        if(Math.abs(top-menu.getY()-(menu.getY()+menu.getHeight()-bottom))>1||bottom!=s.getGuiTop()+hand.y-SidePanels.HAND_TOP+SidePanels.HAND_HEIGHT)
            throw new IllegalStateException("The side column is not centred on the menu with the Hand at its foot: "+s.getClass());
        for(var r:panels){
            if(r.getX()<0||r.getY()<0||r.getX()+r.getWidth()>mc.getWindow().getGuiScaledWidth()||r.getY()+r.getHeight()>mc.getWindow().getGuiScaledHeight())throw new IllegalStateException("A side panel is clipped: "+s.getClass()+" / "+r.getX()+","+r.getY());
            if(r.getX()!=menu.getX()+SidePanels.reach(s)+SidePanels.GAP)throw new IllegalStateException("A side panel stands off the column");
            if(overlap(r,menu))throw new IllegalStateException("A side panel overlaps the menu");
            if(recipe!=null&&overlap(r,recipe))throw new IllegalStateException("A side panel overlaps the open recipe controls");
            for(var other:panels)if(other!=r&&overlap(r,other))throw new IllegalStateException("Side panels overlap each other");
        }
    }
    private static boolean overlap(Rect2i a,Rect2i b){return a.getX()<b.getX()+b.getWidth()&&b.getX()<a.getX()+a.getWidth()&&a.getY()<b.getY()+b.getHeight()&&b.getY()<a.getY()+a.getHeight();}
    public static void clickSlot(Minecraft mc,int index){var s=screen(mc);var slot=s.getMenu().getSlot(index);click(mc,s.getGuiLeft()+slot.x+8,s.getGuiTop()+slot.y+8);}
    public static void clickHand(Minecraft mc){var s=screen(mc);var hand=hand(mc);click(mc,s.getGuiLeft()+hand.x+8,s.getGuiTop()+hand.y+8);}
    public static void clickLabel(Minecraft mc){var s=screen(mc);var hand=hand(mc);click(mc,s.getGuiLeft()+hand.x+8,s.getGuiTop()+hand.y-8);}
    /** Vanilla reads a second click on a slot within this many real milliseconds as a double-click, which gathers instead of placing. */
    private static final long DOUBLE_CLICK=250;
    private static long lastClick;
    /**
     * One single click. Scene ticks run faster than real time while the client catches up, so a click waits out what
     * is left of the double-click window since the last one.
     */
    public static void click(Minecraft mc,double x,double y){
        long wait=lastClick+DOUBLE_CLICK-net.minecraft.Util.getMillis();
        if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){throw new IllegalStateException(e);}
        var s=screen(mc);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);lastClick=net.minecraft.Util.getMillis();
    }
    /** Presses the screen's recipe panel switch: the recipe book's button, or a machine's Recipes tab. */
    public static void toggleRecipes(Minecraft mc){
        screen(mc).children().stream().filter(w->w instanceof net.minecraft.client.gui.components.ImageButton||w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Recipes"))
                .map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow().onPress();
    }
}

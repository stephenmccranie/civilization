package dev.civilization.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Cabinet skin shared by every controller. Slots and all interaction remain native. */
public final class MachineUi {
    public static final int INK=0xff35332e, MUTED=0xff5c584e;
    public static final int HAND_LEFT=12,HAND_TOP=24,HAND_WIDTH=40,HAND_HEIGHT=46,PACK_COLUMNS=3;
    private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("civilization","textures/gui/cabinet.png");
    private MachineUi() {}
    public static void panel(GuiGraphics g,int x,int y,int w,int h) {
        // Larger corner slices thicken the authored frame and brass heads together.
        x-=3;y-=3;w+=6;h+=6;
        int b=Math.min(18,Math.min(w,h)/2);
        int[] sx={0,48,464}, sy={0,48,464}, sw={48,416,48};
        int[] dx={x,x+b,x+w-b}, dy={y,y+b,y+h-b}, dw={b,w-2*b,b}, dh={b,h-2*b,b};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            g.blit(PANEL,dx[col],dy[row],dw[col],dh[row],sx[col],sy[row],sw[col],sw[row],512,512);
    }
    /**
     * Lays the worn pack's slots out above the Hand panel, three across under a "Pack" label, and returns the panel,
     * or null when no pack is shown beside this menu.
     */
    public static net.minecraft.client.renderer.Rect2i packLayout(AbstractContainerMenu menu){
        net.minecraft.world.inventory.Slot hand=null;int n=0;
        for(var s:menu.slots){if(s instanceof dev.civilization.Backpack.PackSlot)n++;else if(s.container instanceof net.minecraft.world.entity.player.Inventory&&s.getContainerSlot()==dev.civilization.BlockCarry.OFFHAND&&s.x<0)hand=s;}
        if(hand==null||n==0)return null;
        int rows=(n+PACK_COLUMNS-1)/PACK_COLUMNS,width=PACK_COLUMNS*18+12,height=rows*18+20,left=hand.x-HAND_LEFT+HAND_WIDTH-width,top=hand.y-HAND_TOP-4-height,i=0;
        for(var s:menu.slots)if(s instanceof dev.civilization.Backpack.PackSlot){
            ((dev.civilization.mixin.SlotPositionAccess)s).civilization$setX(left+7+i%PACK_COLUMNS*18);((dev.civilization.mixin.SlotPositionAccess)s).civilization$setY(top+15+i/PACK_COLUMNS*18);i++;
        }
        return new net.minecraft.client.renderer.Rect2i(left,top,width,height);
    }
    public static void packPanel(GuiGraphics g,net.minecraft.client.renderer.Rect2i r){
        panel(g,r.getX()+3,r.getY()+3,r.getWidth()-6,r.getHeight()-6);
        var font=Minecraft.getInstance().font;
        g.drawString(font,"Pack",r.getX()+(r.getWidth()-font.width("Pack"))/2,r.getY()+5,INK,false);
    }
    public static void handPanel(GuiGraphics g,int x,int y){
        panel(g,x-HAND_LEFT+3,y-HAND_TOP+3,HAND_WIDTH-6,HAND_HEIGHT-6);
        var font=Minecraft.getInstance().font;
        g.drawString(font,"Hand",x+8-font.width("Hand")/2,y-12,INK,false);
        slot(g,x,y);
    }
    public static net.minecraft.client.renderer.Rect2i handBounds(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,net.minecraft.world.inventory.Slot slot){
        return new net.minecraft.client.renderer.Rect2i(screen.getGuiLeft()+handSlotX(screen)-HAND_LEFT,screen.getGuiTop()+slot.y-HAND_TOP,HAND_WIDTH,HAND_HEIGHT);
    }
    public static int handSlotX(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen){
        if(screen instanceof MachineScreen<?> machine)return machine.handSlotX();
        return dev.civilization.BlockCarry.HAND_SLOT_X-(screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener book&&book.getRecipeBookComponent().isVisible()?177:0);
    }
    public static void slot(GuiGraphics g,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xffe0d8c6);
        g.fill(x-1,y-1,x+16,y+16,0xff45453f);
        g.fill(x,y,x+16,y+16,0xff77776f);
        g.fill(x+1,y+1,x+16,y+16,0xff828078);
    }
    public static void slots(GuiGraphics g,AbstractContainerMenu menu,int x,int y) {
        for(var s:menu.slots)if(s.isActive())slot(g,x+s.x,y+s.y);
    }
    public static void ghost(GuiGraphics g,ItemStack item,int x,int y) {
        if(item.isEmpty())return;
        g.renderItem(item,x,y);
        // Foreground veil distinguishes a plan from owned inventory, including its count.
        g.pose().pushPose();g.pose().translate(0,0,200);g.fill(x,y,x+16,y+16,0xa0828078);
        g.renderItemDecorations(Minecraft.getInstance().font,item,x,y);g.pose().popPose();
    }
    public static void progress(GuiGraphics g,int x,int y,int w,float fraction) {
        g.fill(x,y,x+w,y+6,0xff504c41);g.fill(x+1,y+1,x+w-1,y+5,0xff777166);
        int fill=Math.round((w-2)*Math.clamp(fraction,0,1));
        if(fill>0){g.fill(x+1,y+1,x+1+fill,y+5,0xffb48b43);g.fill(x+1,y+1,x+1+fill,y+2,0xffe4c16d);}
    }
    public static void gauge(GuiGraphics g,int x,int y,int amount,int capacity,int color){
        g.fill(x-2,y-2,x+20,y+42,0xff595446);g.fill(x-1,y-1,x+19,y+41,0xffc4b38b);
        g.fill(x,y,x+18,y+40,0xff484841);
        int h=40*Math.clamp(amount,0,capacity)/Math.max(1,capacity);
        if(h>0)g.fill(x,y+40-h,x+18,y+40,color);
        g.fill(x+2,y+1,x+4,y+39,0x33ffffff);
        for(int i=10;i<40;i+=10)g.fill(x,y+i,x+5,y+i+1,0xffd6ccb6);
    }
    /** The fire gauge's words: how full the machine's stored heat is, and how long it lasts. */
    public static net.minecraft.network.chat.Component heat(dev.civilization.CoalFireMenu fire) {
        String state = fire.hot() ? "Hot: works for about " + fire.minutesHot() + " min unfed"
                : (fire.fireState() & 1) != 0 ? "Warming up: works from " + Math.round(dev.civilization.CoalFire.WORKING * 100) + "%" : "Cold";
        return net.minecraft.network.chat.Component.literal("Heat " + Math.round(fire.heat() / 10f) + "% · " + state);
    }
    public static void fire(GuiGraphics g,int x,int y,int w,int h,boolean burning) {
        g.fill(x,y,x+w,y+h,0xff353631);g.fill(x+1,y+1,x+w-1,y+h-1,0xff68665c);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xff221e19);
        if(burning){
            var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/fire_0"));
            g.blit(x+3,y+3,0,w-6,h-6,sprite);
        }
        for(int bar=x+10;bar<x+w-3;bar+=10)g.fill(bar,y+2,bar+2,y+h-2,0xff383932);
        g.fill(x+2,y+h-4,x+w-2,y+h-2,0xff484840);
    }
    public static void title(GuiGraphics g,String text,int x,int y,int width) {
        // Raised plaque clears the native chest first row and industrial gauge labels.
        y-=6;
        var font=Minecraft.getInstance().font;
        int w=Math.min(width-20,Math.max(80,font.width(text)+30)),left=x+(width-w)/2;
        g.fill(left+1,y+4,left+w-1,y+23,0xff37352d);
        g.fill(left,y+6,left+w,y+21,0xff37352d);
        g.fill(left+1,y+5,left+w-1,y+21,0xff74603b);
        g.fill(left+2,y+6,left+w-2,y+20,0xffb49b65);
        g.fill(left+3,y+6,left+w-3,y+7,0xffe4ca8d);
        g.fill(left+3,y+19,left+w-3,y+20,0xff8f753f);
        for(int rx:new int[]{left+4,left+w-7})for(int ry:new int[]{y+8,y+16}){
            g.fill(rx,ry,rx+3,ry+3,0xff665031);g.fill(rx,ry,rx+2,ry+2,0xffeed297);g.fill(rx+1,ry+1,rx+2,ry+2,0xffb7954b);
        }
        String label=font.plainSubstrByWidth(text,w-24);
        g.drawString(font,label,x+(width-font.width(label))/2,y+10,INK,false);
    }
}

package dev.civilization.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared pixel-aligned frame and inset controls for civic inventories. */
public final class CivicWidget extends Button {
    public boolean selected;
    public String glyph = "";
    public CivicWidget(String text, int x, int y, int width, int height, Runnable action) {
        super(x, y, width, height, Component.literal(text), ignored -> action.run(), DEFAULT_NARRATION);
    }
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        MachineUi.panel(g,x,y,w,h);
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        g.fill(x,y,x+w,y+h,selected?0xFF95703C:0xFF373737);
        g.fill(x+1,y+1,x+w-1,y+h-1,selected?0xFFD8B577:0xFFD6CFBC);
        g.fill(x+2,y+2,x+w-1,y+h-1,0xFF555555);
        g.fill(x+2,y+2,x+w-2,y+h-2,selected?0xFFBDA574:isHoveredOrFocused()&&active?0xFFC5BDA9:0xFFA69F8D);
        int color=active||selected?0xFF303030:0xFF666666;
        String[] pixels=switch(glyph) {
            case "access" -> new String[]{"00110000","00110000","00000110","01110110","11111000","11110111","00000111","00000000"};
            case "buy" -> new String[]{"00011000","00111100","01100110","01011010","01011010","01100110","00111100","00011000"};
            case "collect" -> new String[]{"00011000","00011000","01111110","00111100","00011000","00000000","01000010","01111110"};
            default -> null;
        };
        if(pixels != null) {
            int ox=x+(w-16)/2,oy=y+(h-16)/2;
            for(int row=0;row<8;row++) for(int col=0;col<8;col++) if(pixels[row].charAt(col)=='1') g.fill(ox+col*2,oy+row*2,ox+col*2+2,oy+row*2+2,color);
        } else {
            var font=Minecraft.getInstance().font;
            g.drawString(font,getMessage(),x+(w-font.width(getMessage()))/2,y+(h-8)/2,color,false);
        }
    }
}

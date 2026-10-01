package dev.civilization.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Native focus/click behavior with compact cabinet rows and item-icon tabs. */
final class RecipeWidget extends Button {
    ItemStack icon=ItemStack.EMPTY;
    boolean selected, dark;
    final boolean row;
    String symbol="";
    RecipeWidget(int x,int y,int width,int height,boolean row,Component name,Runnable action){
        super(x,y,width,height,name,b->action.run(),DEFAULT_NARRATION);this.row=row;
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(row){
            if(selected||isHoveredOrFocused())g.fill(x,y,x+w,y+h,selected?0xffbba477:0xffb9b09b);
            g.fill(x,y+h-1,x+w,y+h,0xff928b7b);
            if(selected)g.fill(x,y,x+2,y+h-1,0xffe3bb66);
        }else if(dark){
            g.fill(x,y,x+w,y+h,0xff282a29);
            g.fill(x+1,y+1,x+w-1,y+h-1,selected?0xffcfab64:0xff9b9d96);
            g.fill(x+2,y+2,x+w-2,y+h-2,0xff262925);
            g.fill(x+3,y+3,x+w-3,y+h-3,selected?0xffad925d:isHoveredOrFocused()?0xff666862:0xff454842);
            g.fill(x+3,y+h-4,x+w-3,y+h-3,selected?0xff7d663e:0xff343630);
            for(int px:new int[]{x+2,x+w-3})for(int py:new int[]{y+2,y+h-3})g.fill(px,py,px+1,py+1,0xffc2b48b);
        }else{
            g.fill(x,y,x+w,y+h,selected?0xffd5b367:0xff45453f);
            g.fill(x+1,y+1,x+w-1,y+h-1,selected?0xffbba477:isHoveredOrFocused()?0xffb9b09b:0xff999587);
            g.fill(x+1,y+1,x+w-1,y+2,selected?0xffead095:0xffd6cfbc);
            g.fill(x+1,y+1,x+2,y+h-1,selected?0xffead095:0xffd6cfbc);
            g.fill(x+w-2,y+2,x+w-1,y+h-1,0xff656258);
            g.fill(x+2,y+h-2,x+w-1,y+h-1,0xff656258);
        }
        if(!icon.isEmpty())g.renderItem(icon,x+(row?4:(w-16)/2),y+(h-16)/2);
        var font=Minecraft.getInstance().font;
        if(row)g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),w-26),x+24,y+(h-8)/2,MachineUi.INK,false);
        else if(!symbol.isEmpty()){
            // Small pixel glyphs stay legible at native GUI scale, without font-dependent symbols.
            String[] pixels=switch(symbol){
                case "auto"->new String[]{"00111101","01100011","01000111","01000000","00000010","11100010","11000110","10111100"};
                case "close"->new String[]{"00010000","00110000","01100000","11000000","01100000","00110000","00010000","00000000"};
                default->new String[]{"11011011","11011011","00000000","11011011","11011011","00000000","11011011","11011011"};
            };
            for(int j=0;j<8;j++)for(int i=0;i<8;i++)if(pixels[j].charAt(i)=='1')g.fill(x+(w-8)/2+i,y+(h-8)/2+j,x+(w-8)/2+i+1,y+(h-8)/2+j+1,dark&&!selected?0xffd0c9b7:MachineUi.INK);
        }
    }
}

package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PrototypeStoveScreen extends MachineScreen<PrototypeStoveMenu> {
    private boolean turning;private double preview=-1;
    private Button start,finish;
    public PrototypeStoveScreen(PrototypeStoveMenu m,Inventory i,Component t){super(m,i,t);imageHeight=238;inventoryLabelY=144;}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.literal("Load batch"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+9,topPos+118,76,20).build());finish=addRenderableWidget(Button.builder(Component.literal("Serve"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+91,topPos+118,76,20).build());}
    @Override protected void containerTick(){super.containerTick();start.active=!menu.batch()&&!menu.getSlot(4).hasItem();finish.active=menu.batch();}
    @Override protected void drawMachine(GuiGraphics g,float dt,int mx,int my){
        g.drawString(font,"2 potato / 2 carrot / 1 bread",leftPos+10,topPos+28,MachineUi.INK,false);
        Item[] inputs={Items.POTATO,Items.CARROT,Items.BREAD};for(int i=0;i<3;i++)if(!menu.getSlot(i).hasItem())MachineUi.ghost(g,new ItemStack(inputs[i],i<2?2:1),leftPos+17+i*24,topPos+43);
        g.drawString(font,"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
        if(!menu.getSlot(3).hasItem())MachineUi.ghost(g,KilnContent.MINERAL_COAL.toStack(),leftPos+17,topPos+88);
        var stove=minecraft.level.getBlockEntity(menu.pos) instanceof PrototypeStoveEntity s?s:null;
        int x=leftPos+88,y=topPos+41;g.fill(x,y,x+42,y+23,0xff343432);g.fill(x+2,y+2,x+40,y+21,0xff575751);
        if(menu.batch()&&stove!=null)for(int i=0;i<15;i++){int px=x+4+(i%5)*7,py=y+3+(i/5)*6;g.fill(px,py,px+6,py+5,StoveCooking.color(stove.work(),i%3==0));}
        g.drawString(font,menu.batch()?"Watch the pan, then serve":"Load ingredients, then light",leftPos+9,topPos+65,MachineUi.MUTED,false);
        double value=turning?preview:menu.dial();int cx=leftPos+76,cy=topPos+96;
        for(int py=-14;py<=14;py++)for(int px=-14;px<=14;px++){int r=px*px+py*py;if(r<196)g.fill(cx+px,cy+py,cx+px+1,cy+py+1,r>144?0xff8d8980:0xff383a37);}
        double a=Math.toRadians(-135+270*value-90);for(int n=1;n<=11;n++){int px=(int)Math.round(Math.cos(a)*n),py=(int)Math.round(Math.sin(a)*n);g.fill(cx+px-1,cy+py-1,cx+px+2,cy+py+2,0xffd5b66e);}
        g.drawString(font,Math.round(value*100)+"%",leftPos+61,topPos+77,MachineUi.INK,false);
    }
    private void turn(double x,double y){double a=Math.toDegrees(Math.atan2(y-(topPos+96),x-(leftPos+76)))+90;if(a>180)a-=360;preview=Math.clamp((a+135)/270,0,1);PacketDistributor.sendToServer(new StoveDialPayload(menu.pos,preview));}
    @Override public boolean mouseClicked(double x,double y,int b){if(b==0&&Math.hypot(x-leftPos-76,y-topPos-96)<=16){turning=true;turn(x,y);return true;}return super.mouseClicked(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){if(turning){turn(x,y);return true;}return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){if(turning){turning=false;return true;}return super.mouseReleased(x,y,b);}
    @Override public boolean keyPressed(int k,int scan,int mods){if(k==org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT||k==org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT){PacketDistributor.sendToServer(new StoveDialPayload(menu.pos,Math.clamp(menu.dial()+(k==org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT?-.005:.005),0,1)));return true;}return super.keyPressed(k,scan,mods);}
}

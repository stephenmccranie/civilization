package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ArenaScreen extends AbstractContainerScreen<ArenaMenu> {
    private final Map<Integer,Button> buttons=new LinkedHashMap<>();
    public ArenaScreen(ArenaMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=300;imageHeight=276;inventoryLabelY=182;inventoryLabelX=64;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(ArenaContent.MENU.get(),ArenaScreen::new);}
    private void button(int id,String text,int x,int y,int width){buttons.put(id,addRenderableWidget(Button.builder(Component.literal(text),b->net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArenaPayload(menu.containerId,menu.values.get(1),id,menu.values.get(11)))).bounds(leftPos+x,topPos+y,width,18).build()));}
    @Override protected void init(){super.init();buttons.clear();button(0,"Join A",8,65,62);button(1,"Join B",230,65,62);button(2,"Stake held",77,65,70);button(3,"Refund stake",151,65,72);button(4,"Accept terms",77,86,100);button(5,"Ready / enter prep",181,86,111);button(13,"Withdraw",8,86,65);
        button(6,"Back A: held",8,145,90);button(7,"Back B: held",102,145,90);button(8,"Match bet",196,145,96);button(9,"Cancel offer",8,166,90);button(10,"<",70,122,22);button(11,">",177,122,22);button(12,"Collect",200,166,60);button(14,"X",264,166,28);buttons.get(14).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Host: cancel match and refund every stake")));}
    @Override protected void containerTick(){super.containerTick();boolean lobby=menu.values.get(0)==ArenaData.LOBBY;int side=menu.values.get(2);for(var e:buttons.entrySet())e.getValue().active=lobby;
        buttons.get(0).active=lobby&&side<0&&menu.values.get(3)==0;buttons.get(1).active=lobby&&side<0&&menu.values.get(4)==0;
        for(int id:new int[]{2,3,4,5})buttons.get(id).active=lobby&&side>=0;
        buttons.get(5).active=lobby&&side>=0&&menu.values.get(5)!=0&&menu.values.get(6)!=0;
        for(int id:new int[]{6,7,8,9})buttons.get(id).active=lobby&&side<0&&menu.values.get(3)!=0&&menu.values.get(4)!=0;
        buttons.get(8).active=buttons.get(8).active&&menu.values.get(11)!=0&&menu.values.get(13)==0;
        buttons.get(12).active=menu.values.get(17)>0;buttons.get(13).active=side>=0&&menu.values.get(0)!=ArenaData.RESULT;buttons.get(14).active=menu.values.get(16)!=0&&menu.values.get(0)!=ArenaData.RESULT;}
    private String fighter(int side){int id=menu.values.get(3+side);var entity=minecraft.level.getEntity(id);return id==0?"Open fighter slot":entity==null?(side==0?"Fighter A":"Fighter B"):entity.getName().getString();}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);MachineUi.title(g,"Gladiator Pit",leftPos,topPos,imageWidth);MachineUi.slots(g,menu,leftPos,topPos);
        g.drawString(font,font.plainSubstrByWidth(fighter(0),135),leftPos+8,topPos+29,MachineUi.INK,false);g.drawString(font,font.plainSubstrByWidth(fighter(1),135),leftPos+159,topPos+29,MachineUi.INK,false);
        g.drawString(font,menu.values.get(5)!=0?"Accepted":"Review stakes",leftPos+65,topPos+47,MachineUi.MUTED,false);g.drawString(font,menu.values.get(6)!=0?"Accepted":"Review stakes",leftPos+150,topPos+47,MachineUi.MUTED,false);
        String state=switch(menu.values.get(0)){case ArenaData.COUNTDOWN->"Countdown: "+menu.values.get(14)+"s";case ArenaData.LIVE->"Fighting: "+menu.values.get(14)+"s left";case ArenaData.RESULT->menu.values.get(15)<0?"Match aborted / draw: refunded":"Knockout: "+(menu.values.get(15)==0?"A":"B")+" wins";default->"Deposit stakes, both accept, then Ready";};g.drawString(font,state,leftPos+8,topPos+108,MachineUi.INK,false);
        String bet=menu.values.get(9)==0?"No bets":menu.values.get(10)+"/"+menu.values.get(9)+" backs "+(menu.values.get(12)==0?"A":"B");g.drawString(font,bet,leftPos+98,topPos+122,MachineUi.INK,false);g.drawString(font,menu.values.get(9)==0?"":menu.values.get(13)!=0?"Matched":"Open offer",leftPos+98,topPos+134,MachineUi.MUTED,false);g.drawString(font,"Owed: "+menu.values.get(17)+" stacks",leftPos+103,topPos+170,MachineUi.MUTED,false);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}

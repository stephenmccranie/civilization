package dev.civilization.client;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class OilEngineScreen extends MachineScreen<OilEngineMenu> {
 private CivicWidget start,test;
 @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(OilEngineContent.MENU.get(),OilEngineScreen::new);}
 public OilEngineScreen(OilEngineMenu m,Inventory i,Component t){super(m,i,t);imageHeight=214;inventoryLabelY=120;}
 @Override protected void init(){super.init();start=addRenderableWidget(new CivicWidget("Start / Stop",leftPos+39,topPos+30,98,20,()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)));test=addRenderableWidget(new CivicWidget("Test run · 10s",leftPos+39,topPos+54,98,20,()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)));}
 @Override protected void containerTick(){super.containerTick();start.setMessage(Component.literal(menu.data.get(5)==1?"Stop engine":"Start engine"));test.active=menu.data.get(5)==1&&menu.data.get(3)>=100&&menu.data.get(6)==0;}
 @Override protected void drawMachine(GuiGraphics g,float dt,int x,int y){MachineUi.gauge(g,leftPos+14,topPos+32,menu.data.get(0),4000,0xffcb923f);MachineUi.gauge(g,leftPos+144,topPos+32,menu.data.get(1),2000,0xff786938);g.drawString(font,"Fuel",leftPos+10,topPos+21,MachineUi.INK,false);g.drawString(font,"Oil",leftPos+146,topPos+21,MachineUi.INK,false);MachineUi.progress(g,leftPos+39,topPos+79,98,menu.data.get(3)/100f);g.drawString(font,"Lubrication: "+menu.data.get(4)/10+"%",leftPos+12,topPos+94,MachineUi.INK,false);}
 @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);String s=switch(menu.data.get(2)){case 1->"Complete the build";case 2->"Needs refined fuel";case 3->"Heating the bulb";case 4->"Ready · waiting for load";case 5->"Running under load";default->"Stopped";};g.drawString(font,s,leftPos+9,topPos+110,MachineUi.MUTED,false);if(isHovering(13,31,20,42,x,y))g.renderTooltip(font,Component.literal("Refined fuel: "+menu.data.get(0)+" / 4,000 mB"),x,y);if(isHovering(143,31,20,42,x,y))g.renderTooltip(font,Component.literal("Lubricating oil: "+menu.data.get(1)+" / 2,000 mB"),x,y);renderTooltip(g,x,y);}
}

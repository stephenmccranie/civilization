package dev.civilization.client;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BulkScreen extends MachineScreen<BulkMenu> {
    private CivicWidget load,take;
    public BulkScreen(BulkMenu m,Inventory i,Component t){super(m,i,t);imageHeight=198;inventoryLabelY=104;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(BulkContent.MENU.get(),BulkScreen::new);}
    @Override protected void init(){super.init();load=addRenderableWidget(new CivicWidget("Store 32",leftPos+13,topPos+69,72,20,()->send(0)));take=addRenderableWidget(new CivicWidget("Take 32",leftPos+91,topPos+69,72,20,()->send(1)));}
    private void send(int i){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i);}
    @Override protected void containerTick(){super.containerTick();load.setMessage(Component.literal(menu.liquid()?"Use canister":"Store 32"));load.setWidth(menu.liquid()?150:72);take.visible=!menu.liquid();load.active=take.active=menu.data.get(3)==1;}
    private String contents(){return menu.liquid()?switch(menu.data.get(4)){case 1->"Crude oil";case 2->"Refined fuel";case 5->"Lubricating oil";default->"Empty tank";}:"Mineral Coal";}
    @Override protected void drawMachine(GuiGraphics g,float dt,int x,int y){
        g.drawString(font,contents(),leftPos+13,topPos+26,MachineUi.INK,false);
        g.drawString(font,String.format(java.util.Locale.ROOT,"%,d / %,d%s",menu.amount(),menu.liquid()?64000:8192,menu.liquid()?" mB":""),leftPos+13,topPos+40,MachineUi.INK,false);
        MachineUi.progress(g,leftPos+13,topPos+55,150,menu.amount()/(menu.liquid()?64000f:8192f));
        if(menu.data.get(3)!=1)g.drawString(font,"Complete the shell",leftPos+13,topPos+92,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);if(isHovering(13,25,150,40,x,y))g.renderTooltip(font,Component.literal(contents()+": "+menu.amount()+(menu.liquid()?" mB":" items")),x,y);if(menu.liquid()&&isHovering(13,69,150,20,x,y))g.renderTooltip(font,Component.literal("Exchange the canister held in your selected hotbar slot (1,000 mB)"),x,y);renderTooltip(g,x,y);}
}

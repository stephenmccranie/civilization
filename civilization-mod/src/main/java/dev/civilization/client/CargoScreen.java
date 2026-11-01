package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CargoScreen extends MachineScreen<CargoMenu> {
    private CivicWidget load,unload,previous,next;
    public CargoScreen(CargoMenu m,Inventory inv,Component title){super(m,inv,title);imageHeight=251;inventoryLabelY=157;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(CargoContent.MENU.get(),CargoScreen::new);}
    @Override protected void init(){
        super.init();
        load=addRenderableWidget(new CivicWidget("Load nearby",leftPos+8,topPos+116,78,20,()->send(0)));
        unload=addRenderableWidget(new CivicWidget("Unload nearby",leftPos+90,topPos+116,78,20,()->send(1)));
        previous=addRenderableWidget(new CivicWidget("<",leftPos+8,topPos+140,20,14,()->send(2)));
        next=addRenderableWidget(new CivicWidget(">",leftPos+148,topPos+140,20,14,()->send(3)));
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void containerTick(){super.containerTick();load.active=unload.active=menu.data.get(118)>0;previous.active=menu.page()>0;next.active=menu.page()+1<menu.pageCount();}
    @Override protected void drawMachine(GuiGraphics g,float partial,int mouseX,int mouseY){
        String mass=Double.isFinite(menu.mass())?String.format(java.util.Locale.ROOT,"%,.0f / %,.0f kg",menu.mass(),menu.capacity()):"Unknown load: unload to recover";
        g.drawString(font,mass,leftPos+8,topPos+26,MachineUi.INK,false);
        MachineUi.progress(g,leftPos+8,topPos+40,160,(float)(menu.mass()/Math.max(1,menu.capacity())));
        String target=switch(menu.data.get(118)){case 1->"Stockpile";case 2->"Wheelbarrow";default->"No nearby store";};
        String label=target+" · "+(menu.page()+1)+"/"+Math.max(1,menu.pageCount());
        g.drawString(font,label,leftPos+(imageWidth-font.width(label))/2,topPos+143,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        super.render(g,x,y,partial);
        for(int i=0;i<CargoMenu.ROWS;i++)if(menu.count(i)>1){
            var slot=menu.slots.get(i);String n=quantity(menu.count(i));
            g.pose().pushPose();g.pose().translate(leftPos+slot.x+16,topPos+slot.y+10,300);
            float scale=n.length()>3?.6f:n.length()==3?.8f:1;g.pose().scale(scale,scale,1);g.drawString(font,n,-font.width(n),0,0xffffffff,true);g.pose().popPose();
        }
        if(hoveredSlot!=null&&hoveredSlot.index<CargoMenu.ROWS&&hoveredSlot.container!=minecraft.player.getInventory()&&hoveredSlot.hasItem()){
            int i=menu.slots.indexOf(hoveredSlot);
            g.renderComponentTooltip(font,java.util.List.of(hoveredSlot.getItem().getHoverName(),Component.literal(String.format(java.util.Locale.ROOT,"%,d items · click to take · Shift-click to move",menu.count(i)))),x,y);
        }
        if(menu.palletCount()>0&&x>=leftPos+8&&x<leftPos+168&&y>=topPos+25&&y<topPos+46)
            g.renderTooltip(font,Component.literal("Shared stockpile: "+menu.palletCount()+" connected pallet"+(menu.palletCount()==1?"":"s")),x,y);
    }
    private static String quantity(long n){
        if(n<1000)return Long.toString(n);
        double divisor=n<1_000_000?1e3:n<1_000_000_000?1e6:n<1_000_000_000_000L?1e9:1e12;
        String suffix=n<1_000_000?"k":n<1_000_000_000?"M":n<1_000_000_000_000L?"b":"T";
        return String.format(java.util.Locale.ROOT,"%.1f%s",n/divisor,suffix);
    }
}

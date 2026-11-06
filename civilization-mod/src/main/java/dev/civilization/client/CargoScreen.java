package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CargoScreen extends MachineScreen<CargoMenu> {
    private CivicWidget load,unload,previous,next,bind;
    private int target;
    public CargoScreen(CargoMenu m,Inventory inv,Component title){super(m,inv,title);imageHeight=251;inventoryLabelY=157;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(CargoContent.MENU.get(),CargoScreen::new);}
    @Override protected void init(){
        super.init();
        load=addRenderableWidget(new CivicWidget("Load nearby",leftPos+8,topPos+116,78,20,()->send(0)));
        unload=addRenderableWidget(new CivicWidget("Unload nearby",leftPos+90,topPos+116,78,20,()->send(1)));
        previous=addRenderableWidget(new CivicWidget("<",leftPos+8,topPos+140,20,14,()->send(2)));
        next=addRenderableWidget(new CivicWidget(">",leftPos+148,topPos+140,20,14,()->send(3)));
        bind=addRenderableWidget(new CivicWidget("Bind backpack",leftPos+54,topPos+140,92,14,()->send(4)));
        target=-1;
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void containerTick(){super.containerTick();load.active=unload.active=menu.data.get(118)>0;previous.active=menu.page()>0;next.active=menu.page()+1<menu.pageCount();
        bind.visible=menu.binding()>0;bind.setMessage(Component.literal(menu.binding()==2?"Unbind backpack":"Bind backpack"));
        if(target!=menu.data.get(118)){
            target=menu.data.get(118);String partner=switch(target){case 1->"the nearest Pallet";case 2->"the nearest parked wheelbarrow";default->null;};
            load.setTooltip(Tooltip.create(Component.literal(partner==null?"No wheelbarrow or Pallet within 6 blocks":"Load from "+partner)));
            unload.setTooltip(Tooltip.create(Component.literal(partner==null?"No wheelbarrow or Pallet within 6 blocks":"Unload to "+partner)));
        }
    }
    @Override protected void drawMachine(GuiGraphics g,float partial,int mouseX,int mouseY){
        String load=CargoInventory.blocks(menu.load(),menu.capacity());
        g.drawString(font,load,leftPos+8,topPos+26,MachineUi.INK,false);
        MachineUi.progress(g,leftPos+8,topPos+40,160,(float)(menu.load()/Math.max(1,menu.capacity())));
        String page=(menu.page()+1)+"/"+Math.max(1,menu.pageCount());
        g.drawString(font,page,leftPos+40-font.width(page)/2,topPos+143,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        super.render(g,x,y,partial);
        for(int i=0;i<CargoMenu.ROWS;i++)if(menu.count(i)>1){
            var slot=menu.slots.get(i);String n=MachineUi.quantity(menu.count(i));
            g.pose().pushPose();g.pose().translate(leftPos+slot.x+16,topPos+slot.y+10,300);
            float scale=n.length()>3?.6f:n.length()==3?.8f:1;g.pose().scale(scale,scale,1);g.drawString(font,n,-font.width(n),0,0xffffffff,true);g.pose().popPose();
        }
        if(hoveredSlot!=null&&hoveredSlot.index<CargoMenu.ROWS&&hoveredSlot.container!=minecraft.player.getInventory()&&hoveredSlot.hasItem()){
            int i=menu.slots.indexOf(hoveredSlot);
            g.renderComponentTooltip(font,java.util.List.of(hoveredSlot.getItem().getHoverName(),Component.literal(String.format(java.util.Locale.ROOT,"%,d items · click to take · Shift-click to move",menu.count(i)))),x,y);
        }
        if(menu.palletCount()>0&&x>=leftPos+8&&x<leftPos+168&&y>=topPos+25&&y<topPos+46){
            var lines=new java.util.ArrayList<Component>();int high=StockpileEntity.TWO_HIGH.capacity();
            lines.add(Component.literal(menu.palletCount()==1?"A pallet holds up to "+high+" blocks, two high":"Shared by "+menu.palletCount()+" connected pallets, up to "+high+" blocks each"));
            if(menu.capacity()<menu.palletCount()*(double)high)lines.add(Component.literal("A pallet with something above it holds "+StockpileEntity.ONE_HIGH.capacity()));
            if(!(menu.load()<=menu.capacity()))lines.add(Component.literal("Accepts nothing until goods are taken off"));
            g.renderComponentTooltip(font,lines,x,y);
        }
    }
}

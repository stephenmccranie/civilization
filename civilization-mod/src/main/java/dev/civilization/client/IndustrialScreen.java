package dev.civilization.client;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import static dev.civilization.IndustrialBlock.Kind;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class IndustrialScreen extends MachineScreen<IndustrialMenu> {
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(IndustrialContent.MENU.get(),IndustrialScreen::new);}
    public IndustrialScreen(IndustrialMenu m,Inventory inv,Component title){super(m,inv,title);imageHeight=214;inventoryLabelY=120;}
    private void gauge(GuiGraphics g,int x,int amount,int capacity,int color,String name){
        MachineUi.gauge(g,x,topPos+32,amount,capacity,color);
        g.drawString(font,name,x+9-font.width(name)/2,topPos+21,0xff404040,false);
    }
    @Override protected void drawMachine(GuiGraphics g,float partial,int mx,int my){
        var k=menu.kind();
        if(k!=Kind.DERRICK&&k!=Kind.COAL_ENGINE)gauge(g,leftPos+14,menu.data.get(1),k==Kind.TANK?16000:4000,color(menu.data.get(3)),switch(k){case TANK->"Tank";case DRILL->"Fuel";case COLUMN->"Hot oil";case CONDENSER->"Vapor";default->"Crude";});
        if(IndustrialStructure.remote(k))gauge(g,leftPos+144,menu.data.get(2),4000,color(menu.data.get(10)),switch(k){case DERRICK->"Crude";case HEATER->"Hot oil";case COLUMN->"Vapor";default->"Fuel";});
        if(k==Kind.COLUMN||k==Kind.DRILL)gauge(g,leftPos+78,menu.data.get(7),4000,color(5),"Oil");
        if(k==Kind.DERRICK)MachineUi.progress(g,leftPos+60,topPos+76,74,menu.data.get(4)/(float)Math.max(1,menu.data.get(9)));
        else if(k!=Kind.TANK&&k!=Kind.COAL_ENGINE)MachineUi.progress(g,leftPos+(menu.hasCoalFire()?54:42),topPos+(menu.hasCoalFire()?49:76),menu.hasCoalFire()?68:92,menu.data.get(4)/(float)Math.max(1,menu.data.get(9)));
        if(menu.hasCoalFire())MachineUi.fire(g,leftPos+66,topPos+85,44,23,(menu.fireState()&1)!=0);
        var supply=IndustrialBlockEntity.supply(k);
        if(supply!=null){
            g.drawString(font,k==Kind.DERRICK?"Casing":"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
            for(int i=0;i<2;i++){var slot=menu.getSlot(i);if(!slot.hasItem())MachineUi.ghost(g,new net.minecraft.world.item.ItemStack(supply),leftPos+slot.x,topPos+slot.y);}
        }

    }
    private static String fluidName(int id,int fallback){return PipeFlowEntity.name(id==0?fallback:id);}
    public static int color(int id){return switch(id){case 1->0xff473931;case 3->0xff925533;case 4->0xffc4c9bf;case 5->0xff786938;default->0xffcb923f;};}
    @Override public void render(GuiGraphics g,int x,int y,float delta){super.render(g,x,y,delta);var k=menu.kind();
        String state=k==Kind.TANK?"Right-click with a canister":switch(menu.data.get(5)){case MachineStatus.Industry.INCOMPLETE->IndustrialBlock.model(k)!=null?(menu.data.get(12)==0?"Supply construction materials":"Machine is damaged"):"Complete the build";case MachineStatus.Industry.WRONG_SITE->"Wrong site - use prospecting rod";case MachineStatus.Industry.DEPLETED->"Deposit depleted";case MachineStatus.Industry.OUTPUT_FULL->"Output full";case MachineStatus.Industry.NEEDS_FUEL->k==Kind.DRILL?"Needs refined fuel":"Needs Coal";case MachineStatus.Industry.NEEDS_INPUT->k==Kind.COLUMN?"Needs heated crude":k==Kind.CONDENSER?"Needs distillate vapor":"Needs crude oil";case MachineStatus.Industry.WORKING->k==Kind.DERRICK?(menu.data.get(15)!=0?"Pumping crude":"Drilling"):k==Kind.COAL_ENGINE?"Driving a machine":"Processing";case MachineStatus.Industry.UNLOADED->"Deposit chunks not loaded";case MachineStatus.Industry.CLAIM_BLOCKED->"Resource protected by another claim";case MachineStatus.Industry.SEARCHING->"Locating remaining blocks";case MachineStatus.Industry.FIRE_UNLIT->"Strike to light the fire";case MachineStatus.Industry.WARMING->"Warming up";case MachineStatus.Industry.NEEDS_CASING->"Load Iron Plates to case the well";case MachineStatus.Industry.BLOCKED->"The bit has stopped at bedrock";case MachineStatus.Industry.NO_ENGINE->"Needs an engine within 8 blocks";case MachineStatus.Industry.NEEDS_POWER->"Waiting for the engine";default->k==Kind.COAL_ENGINE?"Ready to drive a machine within 8 blocks":"Ready";};
        g.drawString(font,font.plainSubstrByWidth(state,158),leftPos+9,topPos+110,MachineUi.MUTED,false);
        if(isHovering(8,109,160,10,x,y))g.renderTooltip(font,Component.literal(state),x,y);
        String detail=switch(k){case DERRICK->menu.data.get(15)!=0?"Well "+menu.data.get(13)+" m":"Bit "+menu.data.get(13)+" m · cased "+menu.data.get(14)+" m";case COAL_ENGINE->"";case DRILL->"Lubrication: "+(menu.data.get(8)/10)+"%";case TANK->String.format(java.util.Locale.ROOT,"%,d / 16,000 mB",menu.data.get(1));case HEATER->"Coal heats crude oil";case COLUMN->"Fuel vapor, oil + sulfur";case CONDENSER->"Air-cooled fuel recovery";};
        if(!menu.hasCoalFire())g.drawString(font,detail,leftPos+(k==Kind.DERRICK?60:8),topPos+90,0xff404040,false);renderTooltip(g,x,y);
        if(k!=Kind.DERRICK&&k!=Kind.COAL_ENGINE&&isHovering(13,31,20,42,x,y))g.renderTooltip(font,Component.literal(fluidName(menu.data.get(3),switch(k){case TANK->0;case DRILL->2;case COLUMN->3;case CONDENSER->4;default->1;})+": "+menu.data.get(1)+" / "+(k==Kind.TANK?16000:4000)+" mB"),x,y);
        if(IndustrialStructure.remote(k)&&isHovering(143,31,20,42,x,y))g.renderTooltip(font,Component.literal(fluidName(menu.data.get(10),switch(k){case DERRICK->1;case HEATER->3;case COLUMN->4;default->2;})+": "+menu.data.get(2)+" / 4,000 mB"),x,y);
        if(isHovering(77,31,20,42,x,y)&&(k==Kind.COLUMN||k==Kind.DRILL))g.renderTooltip(font,Component.literal("Lubricating oil: "+menu.data.get(7)+" / 4,000 mB"),x,y);
        if(isHovering(8,89,160,12,x,y)&&k==Kind.DRILL)g.renderTooltip(font,Component.literal("Oil restores speed. Dry work gradually slows to 20%. Idle time causes no wear."),x,y);
        if(isHovering(16,87,36,18,x,y)&&menu.hasCoalFire())g.renderTooltip(font,MachineUi.heat(menu),x,y);
        if(isHovering(16,87,36,18,x,y)&&k==Kind.DERRICK)g.renderTooltip(font,Component.literal("One Iron Plate cases four blocks of well"),x,y);
    }
}

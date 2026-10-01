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
    private net.minecraft.client.gui.components.Button assemble;
    @Override protected void init(){super.init();if(menu.kind()==Kind.PUMP){assemble=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Assemble"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,ModeledDerrick.BUILD_BUTTON)).bounds(leftPos+51,topPos+39,76,20).build());assemble.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(ModeledDerrick.BILL+". Materials come from your inventory. Clear the full tower space first.")));}}
    private void gauge(GuiGraphics g,int x,int amount,int capacity,int color,String name){
        MachineUi.gauge(g,x,topPos+32,amount,capacity,color);
        g.drawString(font,name,x+9-font.width(name)/2,topPos+21,0xff404040,false);
    }
    @Override protected void drawMachine(GuiGraphics g,float partial,int mx,int my){
        var k=menu.kind();
        if(k!=Kind.PUMP)gauge(g,leftPos+14,menu.data.get(1),k==Kind.TANK?16000:4000,color(menu.data.get(3)),switch(k){case TANK->"Tank";case DRILL->"Fuel";case COLUMN->"Hot oil";case CONDENSER->"Vapor";default->"Crude";});
        if(k==Kind.PUMP||IndustrialStructure.remote(k))gauge(g,leftPos+144,menu.data.get(2),4000,color(menu.data.get(10)),switch(k){case PUMP->"Crude";case REFINERY->"Hot oil";case COLUMN->"Vapor";default->"Fuel";});
        if(k==Kind.COLUMN||k==Kind.DRILL)gauge(g,leftPos+78,menu.data.get(7),4000,color(5),"Oil");
        if(k!=Kind.TANK)MachineUi.progress(g,leftPos+(menu.hasCoalFire()?54:42),topPos+(menu.hasCoalFire()?49:76),menu.hasCoalFire()?68:92,menu.data.get(4)/(float)Math.max(1,menu.data.get(9)));
        if(k==Kind.PUMP||k==Kind.REFINERY){
            g.drawString(font,"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
            MachineUi.fire(g,leftPos+66,topPos+85,44,23,(menu.fireState()&1)!=0);
            for(int i=0;i<2;i++){var slot=menu.getSlot(i);if(!slot.hasItem())MachineUi.ghost(g,KilnContent.MINERAL_COAL.toStack(),leftPos+slot.x,topPos+slot.y);}
        }

    }
    private static String fluidName(int id,int fallback){return PipeFlowEntity.name(id==0?fallback:id);}
    public static int color(int id){return switch(id){case 1->0xff473931;case 3->0xff925533;case 4->0xffc4c9bf;case 5->0xff786938;default->0xffcb923f;};}
    @Override public void render(GuiGraphics g,int x,int y,float delta){if(assemble!=null)assemble.visible=assemble.active=menu.data.get(13)==0;super.render(g,x,y,delta);var k=menu.kind();
        String state=k==Kind.TANK?"Right-click with a canister":switch(menu.data.get(5)){case MachineStatus.Industry.INCOMPLETE->k==Kind.PUMP?(menu.data.get(13)==0?"Supply construction materials":"Derrick is damaged"):"Complete the build";case MachineStatus.Industry.WRONG_SITE->"Wrong site - use prospecting rod";case MachineStatus.Industry.DEPLETED->"Deposit depleted";case MachineStatus.Industry.OUTPUT_FULL->"Output full";case MachineStatus.Industry.NEEDS_FUEL->k==Kind.DRILL?"Needs refined fuel":"Needs Coal";case MachineStatus.Industry.NEEDS_INPUT->k==Kind.COLUMN?"Needs heated crude":k==Kind.CONDENSER?"Needs distillate vapor":"Needs crude oil";case MachineStatus.Industry.WORKING->"Processing";case MachineStatus.Industry.UNLOADED->"Deposit chunks not loaded";case MachineStatus.Industry.CLAIM_BLOCKED->"Resource protected by another claim";case MachineStatus.Industry.SEARCHING->"Locating remaining blocks";case MachineStatus.Industry.FIRE_UNLIT->"Strike to light the fire";case MachineStatus.Industry.WET->"Shelter the work face from rain";default->"Ready";};
        g.drawString(font,font.plainSubstrByWidth(state,158),leftPos+9,topPos+110,MachineUi.MUTED,false);
        if(isHovering(8,109,160,10,x,y))g.renderTooltip(font,Component.literal(state),x,y);
        String detail=switch(k){case PUMP->"Drains physical oil levels";case DRILL->"Lubrication: "+(menu.data.get(8)/10)+"%";case TANK->String.format(java.util.Locale.ROOT,"%,d / 16,000 mB",menu.data.get(1));case REFINERY->"Coal heats crude oil";case COLUMN->"Fuel vapor, oil + sulfur";case CONDENSER->"Air-cooled fuel recovery";};
        if(!menu.hasCoalFire())g.drawString(font,detail,leftPos+8,topPos+90,0xff404040,false);renderTooltip(g,x,y);
        if(k!=Kind.PUMP&&isHovering(13,31,20,42,x,y))g.renderTooltip(font,Component.literal(fluidName(menu.data.get(3),switch(k){case TANK->0;case DRILL->2;case COLUMN->3;case CONDENSER->4;default->1;})+": "+menu.data.get(1)+" / "+(k==Kind.TANK?16000:4000)+" mB"),x,y);
        if((k==Kind.PUMP||IndustrialStructure.remote(k))&&isHovering(143,31,20,42,x,y))g.renderTooltip(font,Component.literal(fluidName(menu.data.get(10),switch(k){case PUMP->1;case REFINERY->3;case COLUMN->4;default->2;})+": "+menu.data.get(2)+" / 4,000 mB"),x,y);
        if(isHovering(77,31,20,42,x,y)&&(k==Kind.COLUMN||k==Kind.DRILL))g.renderTooltip(font,Component.literal("Lubricating oil: "+menu.data.get(7)+" / 4,000 mB"),x,y);
        if(isHovering(8,89,160,12,x,y)&&k==Kind.DRILL)g.renderTooltip(font,Component.literal("Oil restores speed. Dry work gradually slows to 20%. Idle time causes no wear."),x,y);
        if(isHovering(16,87,36,18,x,y)&&(k==Kind.PUMP||k==Kind.REFINERY))g.renderTooltip(font,Component.literal("Coal | remaining heat: "+menu.data.get(6)+" mB of processing"),x,y);
    }
}

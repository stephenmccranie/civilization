package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class AirshipScreen extends AbstractContainerScreen<AirshipMenu> {
    private EditBox power;
    private CivicWidget inspect,assemble,pilot,disassemble;
    private boolean initialized;
    private String error="";
    public AirshipScreen(AirshipMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=310;imageHeight=230;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(AirshipContent.MENU.get(),AirshipScreen::new);}
    @Override protected void init(){
        String previous=power==null?null:power.getValue();super.init();
        power=addRenderableWidget(new EditBox(font,leftPos+15,topPos+43,185,20,Component.literal("Power in prototype watts")));power.setMaxLength(64);power.setValue(previous==null?"1e7":previous);
        addRenderableWidget(new CivicWidget("Apply",leftPos+210,topPos+43,84,20,()->action(0)));
        inspect=addRenderableWidget(new CivicWidget("Inspect",leftPos+15,topPos+106,84,20,()->action(1)));
        assemble=addRenderableWidget(new CivicWidget("Assemble",leftPos+111,topPos+106,84,20,()->action(2)));
        pilot=addRenderableWidget(new CivicWidget("Pilot",leftPos+207,topPos+106,87,20,()->action(3)));
        disassemble=addRenderableWidget(new CivicWidget("Disassemble",leftPos+111,topPos+106,84,20,()->action(4)));
        disassemble.visible=false;
    }
    private void action(int action){
        try{double value=action==0?AirshipFlight.parsePower(power.getValue()):0;error="";PacketDistributor.sendToServer(new AirshipPayload.Action(menu.containerId,action,value));}
        catch(IllegalArgumentException ex){error="Enter a finite number >= 0 (for example 1e9).";}
    }
    @Override protected void containerTick(){super.containerTick();var s=menu.snapshot;if(s==null){disassemble.visible=false;return;}if(!initialized){power.setValue(Double.toString(s.power()));initialized=true;}inspect.active=assemble.active=!s.assembled();assemble.visible=!s.assembled();disassemble.visible=s.assembled();disassemble.active=s.assembled();pilot.active=s.assembled();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(power.isFocused()&&key!=256){if(key==257||key==335){action(0);return true;}return power.keyPressed(key,scan,modifiers)||key!=256;}return super.keyPressed(key,scan,modifiers);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){CivicWidget.panel(g,leftPos,topPos,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int ink=0xFF302A24;g.drawString(font,title,15,12,ink,false);
        g.drawString(font,"Available power (W) - no fuel or power cap",15,29,ink,false);
        var s=menu.snapshot;
        g.drawString(font,s==null?"Connecting...":String.format(Locale.ROOT,"Applied: %.4g W | Speed: %.1f blocks/s",s.power(),s.speed()),15,72,ink,false);
        g.drawString(font,s!=null&&s.assembled()?String.format(Locale.ROOT,"Assembled | Hull mass: %.0f engine units",s.mass()):"Free-standing build | 1024 blocks maximum",15,87,ink,false);
        String message=!error.isEmpty()?error:s==null?"":s.message();g.drawWordWrap(font,Component.literal(message),15,134,280,ink);
        g.drawString(font,"W/S thrust  A/D turn  Space/Ctrl up/down",15,170,ink,false);
        g.drawString(font,"Shift: leave controls; powered hover continues",15,183,ink,false);
        g.drawString(font,"No speed cap; pauses at unloaded terrain",15,200,ink,false);
        g.drawString(font,"Cargo mass and nuclear fuel are not simulated",15,213,ink,false);
    }
}

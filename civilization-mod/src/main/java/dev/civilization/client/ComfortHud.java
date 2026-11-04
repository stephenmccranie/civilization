package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Native whole-pixel instrument: one readout, no backing panel or extra meter. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class ComfortHud {
    private ComfortHud() {}
    private static double comfort(){var mc=Minecraft.getInstance();var data=ThermalVision.current();
        return data==null||mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||CalorieFoodData.of(mc.player).isDepleted()?0:Math.clamp(data.comfort(),0,1);}
    private static int mix(int a,int b,double t){int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t),g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t),v=(int)((a&255)*(1-t)+(b&255)*t);return 0xff000000|r<<16|g<<8|v;}
    public static void draw(GuiGraphics g,int x,int y){
        var mc=Minecraft.getInstance();var data=ThermalVision.current();if(mc.player==null||data==null)return;
        double c=comfort();int edge=data.playerTemperature()<ThermalRules.COMFORT_CENTER_C?0xff719ac0:0xffce7854;
        int tint=mix(edge,0xffadd5a0,Math.clamp(data.comfort(),0,1));
        double phase=(net.minecraft.Util.getMillis()%4000)/4000.0*Math.PI*2;
        double pulse=(.5+.5*Math.sin(phase))*c;
        // Tight highlights breathe slowly; they never obscure the world or flash.
        int glow=((int)(18+45*pulse)<<24)|(tint&0xffffff);
        if(c>0){g.fill(x-4,y-7,x+4,y+4,glow);g.fill(x-5,y-1,x+5,y+3,glow);}
        g.fill(x-2,y-8,x+2,y+1,0xff29251c);
        g.fill(x-1,y-9,x+1,y-8,0xff29251c);
        g.fill(x-3,y-1,x+3,y+4,0xff29251c);
        g.fill(x-2,y+4,x+2,y+5,0xff29251c);
        g.fill(x-1,y-8,x+1,y+1,0xffc6a05b);
        g.fill(x-2,y,x+2,y+3,0xffb28a49);
        g.fill(x-1,y-7,x+1,y+2,tint);
        g.fill(x-2,y+1,x+2,y+3,tint);
        g.fill(x-1,y-7,x,y-2,mix(tint,0xfff5efce,.4+.2*pulse));
        if(RoadSurface.supports(mc.player)){
            g.fill(x-4,y+4,x+4,y+7,0xff34231c);
            g.fill(x-3,y+4,x+3,y+6,0xffac7051);
            g.fill(x,y+4,x+1,y+6,0xff654638);
            g.fill(x-1,y+3,x+1,y+4,0xfff0ddac);
            g.fill(x-2,y+4,x-1,y+5,0xfff0ddac);g.fill(x+1,y+4,x+2,y+5,0xfff0ddac);
        }
        String label=String.format(Locale.ROOT,"%.0f°F",ThermalDisplay.fahrenheit(data.playerTemperature()));
        g.pose().pushPose();g.pose().translate(x,y-18,0);g.pose().scale(.75f,.75f,1);
        g.drawString(mc.font,label,-mc.font.width(label)/2,0,mix(0xffeeeeee,tint,.25),true);g.pose().popPose();
    }
    @SubscribeEvent public static void inventory(ScreenEvent.Render.Post e){
        if(!(e.getScreen() instanceof InventoryScreen screen)||ThermalVision.current()==null)return;
        renderInventory(screen,e.getGuiGraphics(),e.getMouseX(),e.getMouseY());
    }
    public static void renderInventory(InventoryScreen screen,GuiGraphics g,int mouseX,int mouseY){
        if(ThermalVision.current()==null)return;
        int x=screen.getGuiLeft()+screen.getXSize()/2,y=Math.max(19,screen.getGuiTop()-9);
        draw(g,x,y);
        if(Math.abs(mouseX-x)>18||mouseY<y-19||mouseY>y+8)return;
        var data=ThermalVision.current();double c=comfort();var lines=new ArrayList<Component>();
        lines.add(Component.literal(String.format(Locale.ROOT,"Air temperature: %.1f°F",ThermalDisplay.fahrenheit(data.temperature()))));
        lines.add(Component.literal(String.format(Locale.ROOT,"Felt temperature: %.1f°F",ThermalDisplay.fahrenheit(data.playerTemperature()))));
        lines.add(Component.literal(String.format(Locale.ROOT,"Comfort: %.0f%%",data.comfort()*100)));
        double cold=ThermalRules.coldKcalPerSecond(data.playerTemperature(),CalorieConfig.COLD_EXPOSURE.get());
        if(cold>0)lines.add(Component.literal(CalorieFoodData.of(Minecraft.getInstance().player).isDepleted()?"Cold drain: no calories left":String.format(Locale.ROOT,"Cold drain: %.2f kcal/s",cold)));
        lines.add(Component.literal(String.format(Locale.ROOT,"Work / travel calories: −%.0f%%",20*c)));
        lines.add(Component.literal(String.format(Locale.ROOT,"Mining speed: +%.0f%%",15*c)));
        lines.add(Component.literal(String.format(Locale.ROOT,"Comfort movement: +%.0f%%",5*c)));
        boolean road=RoadSurface.supports(Minecraft.getInstance().player);
        lines.add(Component.literal(road?String.format(Locale.ROOT,"Brick road movement: +%.0f%%",(RoadSurface.BONUS-1)*100):"Brick road: inactive"));
        if(CalorieFoodData.of(Minecraft.getInstance().player).isDepleted())lines.add(Component.literal("Comfort bonuses paused while depleted."));
        g.renderComponentTooltip(Minecraft.getInstance().font,lines,mouseX,mouseY);
    }
}

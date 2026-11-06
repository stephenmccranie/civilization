package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One column against the right side of every menu that shows the Hand, the same everywhere: the Nearby stores on top,
 * the worn Pack, and the Hand, stacked and centred on the menu's height. The recipe book and machine recipe drawers keep
 * the left side, so nothing here moves when they open. Positions are relative to the menu, like its slots.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class SidePanels {
    public static final int GAP=8,COLUMNS=4,WIDTH=COLUMNS*18+12,SPACING=4;
    public static final int HAND_LEFT=12,HAND_TOP=24,HAND_WIDTH=40,HAND_HEIGHT=46;
    /** How far the column reaches out from the menu's side. */
    public static final int ROOM=GAP+WIDTH;
    /** TITLE insets a panel's title row clear of the frame's corner rivets. */
    private static final int CELLS=COLUMNS*3,BINDING=17,TITLE=12;
    /** The Nearby panel's page, from the screen's first opening. */
    private static int page;
    public record Layout(Slot hand,Rect2i handPanel,Rect2i pack,Rect2i nearby){
        public List<Rect2i> panels(){var all=new ArrayList<Rect2i>();all.add(handPanel);if(pack!=null)all.add(pack);if(nearby!=null)all.add(nearby);return all;}
    }
    /** A screen whose own wings reach past its menu's right side: the column stands beside them instead. */
    public interface Wide{int rightReach();}
    private SidePanels(){}

    /** How far right of the menu's left side the screen's own parts reach. */
    public static int reach(AbstractContainerScreen<?> screen){return screen instanceof Wide wide?wide.rightReach():screen.getXSize();}

    /** Places the Hand and pack slots and returns the panels, or null where the menu shows no Hand. */
    public static Layout layout(AbstractContainerScreen<?> screen){
        var menu=screen.getMenu();var hand=BlockCarry.handSlot(menu);
        if(hand==null||screen instanceof CreativeModeInventoryScreen)return null;
        var packSlots=menu.slots.stream().filter(s->s instanceof Backpack.PackSlot&&s.isActive()).toList();var near=NearbyStores.of(menu);
        int nearbyHeight=near==null||near.stores()==0?0:cells(CELLS);
        int packHeight=packSlots.isEmpty()?0:cells(packSlots.size())+(binding()==null?0:BINDING);
        int left=reach(screen)+GAP,top=(screen.getYSize()-HAND_HEIGHT-stacked(nearbyHeight)-stacked(packHeight))/2;
        Rect2i nearby=null,pack=null;
        if(nearbyHeight>0){nearby=new Rect2i(left,top,WIDTH,nearbyHeight);top+=stacked(nearbyHeight);}
        if(packHeight>0){
            pack=new Rect2i(left,top,WIDTH,packHeight);top+=stacked(packHeight);
            for(int i=0;i<packSlots.size();i++)place(packSlots.get(i),left+7+i%COLUMNS*18,pack.getY()+15+i/COLUMNS*18);
        }
        place(hand,left+HAND_LEFT,top+HAND_TOP);
        return new Layout(hand,new Rect2i(left,top,HAND_WIDTH,HAND_HEIGHT),pack,nearby);
    }
    /** A panel's height around this many slots: its title row, the slot rows and the frame. */
    private static int cells(int slots){return 20+(slots+COLUMNS-1)/COLUMNS*18;}
    /** The column height a panel takes above the next, or nothing for an absent one. */
    private static int stacked(int height){return height==0?0:height+SPACING;}
    private static void place(Slot s,int x,int y){((dev.civilization.mixin.SlotPositionAccess)s).civilization$setX(x);((dev.civilization.mixin.SlotPositionAccess)s).civilization$setY(y);}
    /** Slots drawn in the column rather than by the menu's own skin. */
    public static boolean inColumn(Slot s){return s instanceof Backpack.PackSlot||s.container instanceof Inventory&&s.getContainerSlot()==BlockCarry.OFFHAND;}
    /** The panels in screen coordinates, kept clear by JEI and counted as inside the menu. */
    public static List<Rect2i> areas(AbstractContainerScreen<?> screen){
        var layout=layout(screen);if(layout==null)return List.of();
        return layout.panels().stream().map(r->new Rect2i(screen.getGuiLeft()+r.getX(),screen.getGuiTop()+r.getY(),r.getWidth(),r.getHeight())).toList();
    }
    private static Backpack.Supply binding(){
        var pack=Backpack.worn(Minecraft.getInstance().player);return pack==null?null:pack.pack().get(Backpack.SUPPLY.get());
    }
    private static int pages(NearbyStores near){return Math.max(1,(near.size()+CELLS-1)/CELLS);}
    /** The Nearby cell under a point relative to the menu, or -1. */
    private static int cell(Rect2i nearby,int x,int y){
        int col=(x-nearby.getX()-7)/18,row=(y-nearby.getY()-15)/18;
        return x>=nearby.getX()+7&&y>=nearby.getY()+15&&col<COLUMNS&&row<3?row*COLUMNS+col:-1;
    }
    /** The page arrows, in the Nearby title row between its name and the frame's rivet. */
    private static Rect2i previous(Rect2i r){return new Rect2i(r.getX()+r.getWidth()-TITLE-20,r.getY()+4,9,9);}
    private static Rect2i next(Rect2i r){return new Rect2i(r.getX()+r.getWidth()-TITLE-9,r.getY()+4,9,9);}
    /** The Pack panel's Unbind button, shown while the pack is bound. */
    public static Rect2i unbind(Rect2i pack){return new Rect2i(pack.getX()+pack.getWidth()-16,pack.getY()+pack.getHeight()-BINDING-2,10,10);}
    private static Rect2i bindingRow(Rect2i pack){return new Rect2i(pack.getX()+5,pack.getY()+pack.getHeight()-BINDING-4,pack.getWidth()-22,16);}

    @SubscribeEvent public static void opened(ScreenEvent.Init.Post e){page=0;}

    @SubscribeEvent public static void background(ContainerScreenEvent.Render.Background e){
        var screen=e.getContainerScreen();var layout=layout(screen);if(layout==null)return;
        var g=e.getGuiGraphics();g.pose().pushPose();g.pose().translate(screen.getGuiLeft(),screen.getGuiTop(),0);
        for(var r:layout.panels())MachineUi.panel(g,r.getX()+3,r.getY()+3,r.getWidth()-6,r.getHeight()-6);
        MachineUi.slot(g,layout.hand.x,layout.hand.y);
        for(var s:screen.getMenu().slots)if(s instanceof Backpack.PackSlot&&s.isActive())MachineUi.slot(g,s.x,s.y);
        if(layout.nearby!=null)for(int i=0;i<CELLS;i++)MachineUi.slot(g,layout.nearby.getX()+7+i%COLUMNS*18,layout.nearby.getY()+15+i/COLUMNS*18);
        g.pose().popPose();
    }

    @SubscribeEvent public static void foreground(ContainerScreenEvent.Render.Foreground e){
        var screen=e.getContainerScreen();var layout=layout(screen);if(layout==null)return;
        var g=e.getGuiGraphics();var font=Minecraft.getInstance().font;
        int mx=e.getMouseX()-screen.getGuiLeft(),my=e.getMouseY()-screen.getGuiTop();
        g.drawString(font,"Hand",layout.hand.x+8-font.width("Hand")/2,layout.hand.y-12,MachineUi.INK,false);
        if(layout.pack!=null){
            var r=layout.pack;var worn=Backpack.worn(Minecraft.getInstance().player);
            g.drawString(font,"Pack",r.getX()+TITLE,r.getY()+5,MachineUi.INK,false);
            String count=(Backpack.BLOCKS-worn.blockRoom())+"/"+Backpack.BLOCKS;
            g.drawString(font,count,r.getX()+r.getWidth()-TITLE-font.width(count),r.getY()+5,MachineUi.MUTED,false);
            var s=binding();
            if(s!=null){
                var row=bindingRow(r);double distance=BackpackClient.distance(Minecraft.getInstance().player,s);
                g.renderItem(new ItemStack(s.store().left().isPresent()?CargoContent.STOCKPILE.get().asItem():WheelbarrowContent.ITEM.get()),row.getX(),row.getY());
                String where=Double.isNaN(distance)?"far":Math.round(distance)+" m";
                g.drawString(font,where,row.getX()+18,row.getY()+4,distance<=Backpack.RADIUS?0xff3f7a2c:MachineUi.MUTED,false);
                var x=unbind(r);g.drawString(font,"×",x.getX()+2,x.getY()+1,x.contains(mx,my)?0xffa83222:MachineUi.INK,false);
            }
        }
        var near=NearbyStores.of(screen.getMenu());
        if(layout.nearby!=null){
            var r=layout.nearby;page=Math.min(page,pages(near)-1);
            g.drawString(font,"Nearby",r.getX()+TITLE,r.getY()+5,MachineUi.INK,false);
            for(int i=0;i<CELLS&&page*CELLS+i<near.size();i++){
                var e2=near.entry(page*CELLS+i);int x=r.getX()+7+i%COLUMNS*18,y=r.getY()+15+i/COLUMNS*18;
                g.renderItem(e2.item(),x,y);
                if(e2.count()>1){
                    String n=MachineUi.quantity(e2.count());float scale=n.length()>3?.6f:n.length()==3?.8f:1;
                    g.pose().pushPose();g.pose().translate(x+16,y+10,200);g.pose().scale(scale,scale,1);g.drawString(font,n,-font.width(n),0,0xffffffff,true);g.pose().popPose();
                }
            }
            int hovered=cell(r,mx,my);
            if(hovered>=0){int x=r.getX()+7+hovered%COLUMNS*18,y=r.getY()+15+hovered/COLUMNS*18;AbstractContainerScreen.renderSlotHighlight(g,x,y,0);}
            if(pages(near)>1){
                g.drawString(font,"<",previous(r).getX()+2,previous(r).getY()+1,page>0?MachineUi.INK:MachineUi.MUTED,false);
                g.drawString(font,">",next(r).getX()+2,next(r).getY()+1,page+1<pages(near)?MachineUi.INK:MachineUi.MUTED,false);
            }
        }
    }

    @SubscribeEvent public static void tooltip(ScreenEvent.Render.Post e){
        if(!(e.getScreen() instanceof AbstractContainerScreen<?> screen)||!screen.getMenu().getCarried().isEmpty())return;
        var layout=layout(screen);if(layout==null)return;
        int mx=e.getMouseX()-screen.getGuiLeft(),my=e.getMouseY()-screen.getGuiTop();var g=e.getGuiGraphics();var font=Minecraft.getInstance().font;
        if(layout.nearby!=null){
            var near=NearbyStores.of(screen.getMenu());int i=cell(layout.nearby,mx,my);
            if(i>=0&&page*CELLS+i<near.size()){var entry=near.entry(page*CELLS+i);
                g.renderComponentTooltip(font,List.of(entry.item().getHoverName(),Component.literal(String.format(Locale.ROOT,"%,d nearby · click to take · Shift-click to move",entry.count())).withStyle(ChatFormatting.GRAY)),e.getMouseX(),e.getMouseY());}
            else if(layout.nearby.contains(mx,my)&&my<layout.nearby.getY()+14)
                g.renderComponentTooltip(font,List.of(Component.literal("Pallets and parked wheelbarrows within "+CargoHolder.REACH+" blocks"),
                        Component.literal("Page "+(page+1)+" of "+pages(near)+" · click with goods to stack them onto the nearest").withStyle(ChatFormatting.GRAY)),e.getMouseX(),e.getMouseY());
        }
        var s=binding();
        if(layout.pack!=null&&s!=null){
            if(unbind(layout.pack).contains(mx,my))g.renderTooltip(font,Component.literal("Unbind the backpack"),e.getMouseX(),e.getMouseY());
            else if(bindingRow(layout.pack).contains(mx,my))g.renderComponentTooltip(font,List.of(Component.literal("Bound to a "+s.storeName()),Component.literal("Building within "+(int)Backpack.RADIUS+" blocks of it draws from it").withStyle(ChatFormatting.GRAY)),e.getMouseX(),e.getMouseY());
        }
    }

    /**
     * A press on the column: Unbind, the Nearby pager and cells, and the panels' frames, which never count as a click
     * outside that drops the cursor. Presses on the Hand and pack slots stay the menu's own. True when consumed.
     */
    public static boolean mouseClicked(AbstractContainerScreen<?> screen,double x,double y,int button){
        var layout=layout(screen);if(layout==null)return false;
        int mx=(int)x-screen.getGuiLeft(),my=(int)y-screen.getGuiTop();
        if(layout.pack!=null&&binding()!=null&&unbind(layout.pack).contains(mx,my)){PacketDistributor.sendToServer(new Backpack.Unbind());return true;}
        if(layout.nearby!=null&&layout.nearby.contains(mx,my)){
            var near=NearbyStores.of(screen.getMenu());int i=cell(layout.nearby,mx,my),at=page*CELLS+i;
            if(previous(layout.nearby).contains(mx,my))page=Math.max(0,page-1);
            else if(next(layout.nearby).contains(mx,my))page=Math.min(pages(near)-1,page+1);
            else if(i>=0&&button<=1)PacketDistributor.sendToServer(new NearbyStores.Click(screen.getMenu().containerId,at<near.size()?near.entry(at).item():ItemStack.EMPTY,button,Screen.hasShiftDown()));
            return true;
        }
        return frame(screen,layout,mx,my);
    }
    /** A release on the column's own parts belongs to it too. */
    public static boolean mouseReleased(AbstractContainerScreen<?> screen,double x,double y){
        var layout=layout(screen);return layout!=null&&frame(screen,layout,(int)x-screen.getGuiLeft(),(int)y-screen.getGuiTop());
    }
    /** A point on a panel but on none of the menu's slots: the column's own, never a click outside that drops the cursor. */
    private static boolean frame(AbstractContainerScreen<?> screen,Layout layout,int x,int y){
        if(layout.panels().stream().noneMatch(r->r.contains(x,y)))return false;
        for(var s:screen.getMenu().slots)if(s.isActive()&&x>=s.x-1&&x<s.x+17&&y>=s.y-1&&y<s.y+17)return false;
        return true;
    }
    @SubscribeEvent public static void scroll(ScreenEvent.MouseScrolled.Pre e){
        if(!(e.getScreen() instanceof AbstractContainerScreen<?> screen)||!(layout(screen) instanceof Layout layout)||layout.nearby==null)return;
        if(!layout.nearby.contains((int)e.getMouseX()-screen.getGuiLeft(),(int)e.getMouseY()-screen.getGuiTop()))return;
        page=Math.clamp(page-(int)Math.signum(e.getScrollDeltaY()),0,pages(NearbyStores.of(screen.getMenu()))-1);e.setCanceled(true);
    }
}

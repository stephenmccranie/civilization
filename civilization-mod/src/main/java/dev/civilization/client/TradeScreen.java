package dev.civilization.client;
import dev.civilization.CivicMenu;
import dev.civilization.TradePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class TradeScreen extends AbstractContainerScreen<CivicMenu> {
    private final EditBox[] quantities = new EditBox[2];
    private boolean syncing, dirty, awaiting;
    private int sentRevision, waitTicks;
    private CivicWidget trade;
    public TradeScreen(CivicMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); imageHeight=168; inventoryLabelY=74; }
    private void action(int button) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TradePayload(menu.containerId,button,menu.revision())); }
    @Override protected void init() {
        super.init();
        for(int i=0;i<2;i++) {
            quantities[i]=addRenderableWidget(new EditBox(font,leftPos+(i==0?-73:195),topPos+62,54,18,Component.literal(i==0?"Received per trade":"Paid per trade")));
            quantities[i].setMaxLength(4); quantities[i].setFilter(text->text.matches("[0-9]{0,4}"));
            quantities[i].setTooltip(Tooltip.create(Component.literal("Items per trade. Enter to save; multiple stacks allowed.")));
            quantities[i].setResponder(text->{if(!syncing) dirty=true;});
        }
        trade=addRenderableWidget(new CivicWidget("Trade",leftPos+194,topPos+109,58,20,()->{ if(!dirty && !awaiting) action(10); else save(); }));
    }
    private void save() {
        if(!dirty || awaiting || !menu.manager()) return;
        try {
            int amount=Integer.parseInt(quantities[0].getValue()),price=Integer.parseInt(quantities[1].getValue());
            if(amount<1 || price<1) return;
            sentRevision=menu.revision(); waitTicks=0; awaiting=true; dirty=false;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TradePayload(menu.containerId,20,sentRevision,amount,price));
        } catch(NumberFormatException ignored) { }
    }
    @Override protected void containerTick() {
        for(var field:quantities) field.visible=menu.manager();
        if(awaiting && (menu.revision()!=sentRevision || ++waitTicks>20)) awaiting=false;
        if(dirty && !quantities[0].isFocused() && !quantities[1].isFocused()) save();
        if(!awaiting && !dirty) {
            syncing=true;
            if(!quantities[0].isFocused()) quantities[0].setValue(Integer.toString(menu.amount()));
            if(!quantities[1].isFocused()) quantities[1].setValue(Integer.toString(menu.price()));
            syncing=false;
        }
        trade.active=!dirty && !awaiting && menu.batches()>0 && !menu.getSlot(27).getItem().isEmpty();
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        for(var field:quantities) if(field.visible && field.isFocused() && key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) { save(); field.setFocused(false); return true; }
            if(field.keyPressed(key,scan,mods) || field.canConsumeInput()) return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public void onClose() { save(); super.onClose(); }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        return x < left-88 || x >= left+264 || y < top || y >= top+imageHeight+18;
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        CivicWidget.panel(g,leftPos-88,topPos+4,92,100);
        CivicWidget.panel(g,leftPos+imageWidth-4,topPos+4,92,132);
        CivicWidget.panel(g,leftPos,topPos+imageHeight-4,imageWidth,22);
        MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);
        MachineUi.slots(g,menu,leftPos,topPos);
        MachineUi.title(g,title.getString(),leftPos,topPos,imageWidth);
        for(int sx:new int[]{-55,213}) { g.fill(leftPos+sx-2,topPos+29,leftPos+sx+20,topPos+51,0xFFC6C6C6); g.fill(leftPos+sx,topPos+31,leftPos+sx+18,topPos+49,0xFF444444); g.fill(leftPos+sx+1,topPos+32,leftPos+sx+17,topPos+48,0xFF8B8B8B); }
    }
    private void center(GuiGraphics g, String text, int x, int y, int color) { g.drawString(font,text,x-font.width(text)/2,y,color,false); }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        super.render(g,x,y,partial);
        center(g,menu.manager()?"Receive":"You give",leftPos-46,topPos+14,0xFF303030);
        center(g,menu.manager()?"Pay":"You get",leftPos+222,topPos+14,0xFF303030);
        if(!menu.manager()) {
            center(g,"× "+menu.amount(),leftPos-46,topPos+65,0xFF303030);
            center(g,"× "+menu.price(),leftPos+222,topPos+65,0xFF303030);
        } else {
            center(g,dirty?"Enter to save":"Per trade",leftPos-46,topPos+87,0xFF404040);
            center(g,"Per trade",leftPos+222,topPos+87,0xFF404040);
        }
        
        center(g,menu.batches()+" funded trades",leftPos+88,topPos+imageHeight+3,0xFF404040);
        renderTooltip(g,x,y);
        if(menu.manager() && (isHovering(-54,32,16,16,x,y) || isHovering(214,32,16,16,x,y)))
            g.renderTooltip(font,Component.literal("Click a carried item to copy it. Empty cursor clears."),x,y);
    }
}

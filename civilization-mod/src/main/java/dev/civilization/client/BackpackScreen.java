package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The worn pack: its binding and supply controls above its slots. */
public final class BackpackScreen extends MachineScreen<BackpackMenu> {
    private CivicWidget supply,unbind,unload;
    public BackpackScreen(BackpackMenu m,Inventory inv,Component title){
        super(m,inv,title);inventoryLabelY=BackpackMenu.playerY(m.size())-11;imageHeight=BackpackMenu.playerY(m.size())+83;
    }
    @Override protected void init(){
        super.init();
        supply=addRenderableWidget(new CivicWidget("Supply on",leftPos+8,topPos+36,62,20,()->send(0)));
        unbind=addRenderableWidget(new CivicWidget("Unbind",leftPos+72,topPos+36,46,20,()->send(1)));
        unload=addRenderableWidget(new CivicWidget("Unload",leftPos+120,topPos+36,48,20,()->send(2)));
        unload.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Put the pack's blocks onto the bound store")));
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private boolean bound(){return menu.data.get(0)>0;}
    @Override protected void containerTick(){
        super.containerTick();boolean on=menu.data.get(1)>0;
        supply.setMessage(Component.literal(on?"Supply on":"Supply off"));supply.selected=on;
        supply.active=unbind.active=bound();unload.active=bound()&&on&&menu.data.get(2)>=0&&menu.data.get(2)<=Backpack.RADIUS;
    }
    @Override protected void drawMachine(GuiGraphics g,float partial,int mouseX,int mouseY){
        String store=menu.data.get(0)==1?"Pallet":"wheelbarrow";int distance=menu.data.get(2);
        String status=!bound()?"Not bound: press Bind in a store's menu":"Bound to a "+store+" · "+(distance<0?"not nearby":distance+" m away");
        g.drawString(font,status,leftPos+8,topPos+24,MachineUi.INK,false);
        var pack=Backpack.worn(minecraft.player);
        if(pack!=null){
            int blocks=0;for(int i=0;i<menu.size();i++)if(BlockCarry.block(menu.getSlot(i).getItem()))blocks+=menu.getSlot(i).getItem().getCount();
            g.drawString(font,blocks+" / "+Backpack.blocks(pack.pack())+" blocks",leftPos+8,topPos+BackpackMenu.PACK_Y-12,MachineUi.MUTED,false);
        }
    }
}

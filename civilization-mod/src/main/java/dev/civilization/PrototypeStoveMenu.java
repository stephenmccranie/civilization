package dev.civilization;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class PrototypeStoveMenu extends AbstractContainerMenu implements CoalFireMenu {
    public final Container inventory;public final ContainerData data;public net.minecraft.core.BlockPos pos;
    public PrototypeStoveMenu(int id,Inventory inv,net.minecraft.core.BlockPos p){this(id,inv,new SimpleContainer(5),new SimpleContainerData(5));pos=p;}
    public PrototypeStoveMenu(int id,Inventory inv,Container c,ContainerData d){super(PrototypeStoveContent.MENU.get(),id);inventory=c;data=d;pos=c instanceof PrototypeStoveEntity s?s.getBlockPos():net.minecraft.core.BlockPos.ZERO;
        for(int i=0;i<4;i++){final int slot=i;addSlot(new Slot(c,i,i<3?17+i*24:17,i<3?43:88){@Override public boolean mayPlace(ItemStack s){return switch(slot){case 0->s.is(Items.POTATO);case 1->s.is(Items.CARROT);case 2->s.is(Items.BREAD);default->s.is(KilnContent.MINERAL_COAL.get());};}});}
        addSlot(new Slot(c,4,143,43){@Override public boolean mayPlace(ItemStack s){return false;}});MachineMenus.playerSlots(this::addSlot,inv,156);addDataSlots(d);
    }
    public boolean hasSkillet(){return data.get(4)!=0;}
    public double dial(){return data.get(0)/10000.0;} public boolean batch(){return data.get(1)!=0;}
    public int fireState(){return data.get(2);}public int coalRemaining(){return data.get(3);}
    @Override public boolean stillValid(Player p){return inventory.stillValid(p);}
    @Override public void clicked(int s,int b,ClickType t,Player p){if(!stillValid(p)){p.closeContainer();return;}super.clicked(s,b,t,p);}
    @Override public ItemStack quickMoveStack(Player p,int i){return MachineMenus.quickMove(this,p,i,5,new int[]{3,0,1,2},this::moveItemStackTo);}
    @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||!(inventory instanceof PrototypeStoveEntity s))return false;boolean ok=switch(id){case 0->s.start();case 1->s.finish();case CoalFire.BUTTON->s.fire.strike(p,true);default->false;};broadcastChanges();return ok;}
}

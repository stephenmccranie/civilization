package dev.civilization;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class WorkshopMenu extends AbstractContainerMenu implements CoalFireMenu {
    public final Container inventory;public final ContainerData data;
    public WorkshopMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(8),new SimpleContainerData(7));}
    public WorkshopMenu(int id,Inventory inv,Container container,ContainerData data){
        super(WorkshopContent.MENU.get(),id);this.inventory=container;this.data=data;
        for(int i=0;i<4;i++){final int index=i;addSlot(new Slot(container,i,17+i*18,43){@Override public boolean mayPlace(ItemStack s){return container.canPlaceItem(index,s);}@Override public boolean isActive(){return index<2||kind()==2;}});}
        addSlot(new Slot(container,4,17,88){@Override public boolean mayPlace(ItemStack s){return s.is(KilnContent.MINERAL_COAL.get());}});
        addSlot(new WorkshopResultSlot(container,5,125,43));
        addSlot(new Slot(container,6,35,88){@Override public boolean mayPlace(ItemStack s){return s.is(KilnContent.MINERAL_COAL.get());}});
        addSlot(new WorkshopResultSlot(container,7,143,43){@Override public boolean isActive(){return kind()!=2;}});
        MachineMenus.playerSlots(this::addSlot,inv,132);
        addDataSlots(data);
    }
    public int fireState(){return data.get(5);}
    public int coalRemaining(){return data.get(6);}
    public int kind(){return data.get(0);}public int selection(){return data.get(1);}
    public WorkshopJobs.Job job(){return MachineWork.job(inventory,kind(),selection());}
    @Override public boolean stillValid(Player p){return inventory.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||!(inventory instanceof WorkshopBlockEntity w))return false;if(id==CoalFire.BUTTON){boolean ok=w.fire.strike(p,MachineStructure.check(w.getLevel(),w.getBlockPos(),w.getBlockState().getValue(WorkshopBlock.FACING)).status()==MachineStructure.COMPLETE);broadcastChanges();return ok;}w.select(id);broadcastChanges();return true;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)){p.closeContainer();return;}super.clicked(slot,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){return MachineMenus.quickMove(this,p,index,8,new int[]{4,6,0,1,2,3},this::moveItemStackTo);}
}

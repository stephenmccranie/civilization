package dev.civilization;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class BulkMenu extends AbstractContainerMenu {
    public final ContainerData data;private final BulkEntity store;
    public BulkMenu(int id,Inventory inv){this(id,inv,null,new SimpleContainerData(5));}
    public BulkMenu(int id,Inventory inv,BulkEntity store,ContainerData data){super(BulkContent.MENU.get(),id);this.store=store;this.data=data;MachineMenus.playerSlots(this::addSlot,inv,116);addDataSlots(data);}
    public int amount(){return (data.get(0)&65535)|((data.get(1)&65535)<<16);}
    public boolean liquid(){return data.get(2)==1;}
    @Override public boolean stillValid(Player p){return store==null||store.valid(p);}
    @Override public boolean clickMenuButton(Player p,int id){return store!=null&&store.control(p,id);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(store==null||store.liquid||!store.valid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var s=slots.get(index).getItem();if(!s.is(KilnContent.MINERAL_COAL.get()))return ItemStack.EMPTY;var before=s.copy();int n=store.insertCoal(s.getCount(),false);if(n==0)return ItemStack.EMPTY;s.shrink(n);slots.get(index).setChanged();return before;}
}

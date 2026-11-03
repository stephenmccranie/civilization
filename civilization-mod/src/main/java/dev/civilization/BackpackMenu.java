package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The worn backpack's slots above the player's inventory and Hand, with its supply controls. */
public final class BackpackMenu extends AbstractContainerMenu {
    public static final int PACK_Y=72;
    private final Container pack;
    public final ContainerData data;
    public BackpackMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){this(id,inv,new SimpleContainer(buf.readVarInt()),new SimpleContainerData(3));}
    BackpackMenu(int id,Inventory inv,Container pack,ContainerData data){
        super(Backpack.MENU.get(),id);this.pack=pack;this.data=data;
        for(int i=0;i<pack.getContainerSize();i++)addSlot(new Backpack.PackSlot(pack,i,8+i%9*18,PACK_Y+i/9*18));
        int y=playerY(pack.getContainerSize());
        MachineMenus.playerSlots(this::addSlot,inv,y);
        if(BlockCarry.restricted(inv.player))addSlot(new Slot(inv,BlockCarry.OFFHAND,BlockCarry.HAND_SLOT_X,y+58));
        addDataSlots(data);
    }
    /** Where the player's inventory starts below a pack of this many slots. */
    public static int playerY(int slots){return PACK_Y+Math.max(1,(slots+8)/9)*18+15;}
    public int size(){return pack.getContainerSize();}
    @Override public boolean stillValid(Player p){return !(pack instanceof Backpack.Contents)||Backpack.worn(p)==pack;}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!(p instanceof ServerPlayer sp)||!stillValid(p))return false;
        switch(id){case 0->Backpack.toggle(sp);case 1->Backpack.unbind(sp);case 2->Backpack.unload(sp,(Backpack.Contents)pack);default->{return false;}}
        return true;
    }
    /** Pack goods go to the pockets, building blocks to the Hand; anything else goes into the pack. */
    @Override public ItemStack quickMoveStack(Player p,int index){
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(index<size()){if(BlockCarry.block(stack)&&BlockCarry.restricted(p))BlockCarry.hold(p.getInventory(),stack);else moveItemStackTo(stack,size(),size()+36,true);}
        else moveItemStackTo(stack,0,size(),false);
        if(stack.getCount()==before.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return before;
    }
}

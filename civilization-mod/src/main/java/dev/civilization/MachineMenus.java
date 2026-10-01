package dev.civilization;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Common inventory geometry and conservative native shift-click routing. */
public final class MachineMenus {
    private MachineMenus(){}
    public static void playerSlots(Consumer<Slot> add,Inventory inv,int y){
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)add.accept(new Slot(inv,9+row*9+col,8+col*18,y+row*18));
        for(int col=0;col<9;col++)add.accept(new Slot(inv,col,8+col*18,y+58));
    }
    @FunctionalInterface public interface Mover {boolean move(ItemStack stack,int start,int end,boolean reverse);}
    public static ItemStack quickMove(AbstractContainerMenu menu,Player p,int index,int machineSlots,int[] inputs,Mover mover){
        if(!menu.stillValid(p)||index<0||index>=menu.slots.size())return ItemStack.EMPTY;
        var slot=menu.getSlot(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<machineSlots){if(!mover.move(stack,machineSlots,menu.slots.size(),true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int target:inputs)if(menu.getSlot(target).isActive()&&menu.getSlot(target).mayPlace(stack))moved|=mover.move(stack,target,target+1,false);if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onQuickCraft(stack,copy);slot.onTake(p,stack);return copy;
    }
}

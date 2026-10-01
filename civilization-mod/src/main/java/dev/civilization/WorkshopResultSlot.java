package dev.civilization;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Native result-slot accounting, including successful shift-click collection. */
public class WorkshopResultSlot extends Slot {
    private int taken;
    public WorkshopResultSlot(Container inventory,int index,int x,int y){super(inventory,index,x,y);}
    @Override public boolean mayPlace(ItemStack stack){return false;}
    @Override public ItemStack remove(int count){var result=super.remove(count);taken+=result.getCount();return result;}
    @Override protected void onQuickCraft(ItemStack stack,int count){taken+=count;}
    @Override public void onTake(Player player,ItemStack stack){
        if(taken>0 && container instanceof WorkshopBlockEntity workshop)workshop.awardExperience(player.position());
        taken=0;super.onTake(player,stack);
    }
}

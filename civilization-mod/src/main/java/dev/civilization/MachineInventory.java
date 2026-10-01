package dev.civilization;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
/** Small shared operations for machine banks; no separate queue or stored stock. */
public final class MachineInventory {
 public static final int[] KILN_INPUT={0,4},KILN_FUEL={1,3},KILN_PRIMARY_OUTPUT={2},KILN_OUTPUT={2,5};
 public static final int[] WORKSHOP_FUEL={4,6},WORKSHOP_OUTPUT={5,7},SMITHY_OUTPUT={5};
 public static final int[] INDUSTRIAL_INPUT={0,2},INDUSTRIAL_OUTPUT={1,5,6,7};
 public static boolean contains(int[] bank,int slot){for(int i:bank)if(i==slot)return true;return false;}
 public static boolean room(Container c,int[] bank,ItemStack result){int free=0;for(int i:bank){var s=c.getItem(i);if(s.isEmpty())free+=Math.min(c.getMaxStackSize(),result.getMaxStackSize());else if(ItemStack.isSameItemSameComponents(s,result))free+=Math.min(c.getMaxStackSize(),s.getMaxStackSize())-s.getCount();}return free>=result.getCount();}
 public static void insert(Container c,int[] bank,ItemStack result){if(!room(c,bank,result))throw new IllegalStateException("Machine output overflow");var left=result.copy();for(int i:bank){var s=c.getItem(i);if(!s.isEmpty()&&!ItemStack.isSameItemSameComponents(s,left))continue;int count=Math.min(left.getCount(),Math.min(c.getMaxStackSize(),left.getMaxStackSize())-s.getCount());if(count>0){c.setItem(i,s.isEmpty()?left.copyWithCount(count):s.copyWithCount(s.getCount()+count));left.shrink(count);}if(left.isEmpty())return;}}
 public static void refill(Container c,int[] bank){if(!c.getItem(bank[0]).isEmpty())return;for(int j=1;j<bank.length;j++)if(!c.getItem(bank[j]).isEmpty()){swap(c,bank[0],bank[j]);return;}}
 public static int count(Container c,int[] bank){int count=0;for(int slot:bank)count+=c.getItem(slot).getCount();return count;}
 public static void swap(Container c,int a,int b){var sa=c.getItem(a);var sb=c.getItem(b);c.setItem(a,sb);c.setItem(b,sa);}
}

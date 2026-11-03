package dev.civilization;

import net.minecraft.world.item.ItemStack;

/** Server-side aggregate cargo; quantities never become oversized ItemStacks. Load and capacity share the store's measure. */
public interface CargoInventory {
    double capacity();
    double load();
    int size();
    long revision();
    long count(int index);
    MassCargo.Entry entry(int index);
    long insert(ItemStack item,long offered);
    long remove(int index,long requested);
    default boolean isEmpty(){return size()==0;}
    default long transferTo(CargoInventory destination){
        if(destination==this)return 0;
        long moved=0;
        for(int i=size()-1;i>=0;i--){
            var e=entry(i);long n=destination.insert(e.item(),e.count());
            if(n>0){remove(i,n);moved=moved>Long.MAX_VALUE-n?Long.MAX_VALUE:moved+n;}
        }
        return moved;
    }
}

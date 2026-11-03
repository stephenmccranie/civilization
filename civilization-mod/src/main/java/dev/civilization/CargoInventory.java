package dev.civilization;

import java.text.*;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/** Server-side aggregate cargo; quantities never become oversized ItemStacks. Load and capacity are solid volume in blocks. */
public interface CargoInventory {
    double capacity();
    double load();
    int size();
    long revision();
    long count(int index);
    CargoLedger.Entry entry(int index);
    long insert(ItemStack item,long offered);
    long remove(int index,long requested);
    default boolean isEmpty(){return size()==0;}
    /** Load against capacity as players read it. */
    static String blocks(double load,double capacity){
        if(!Double.isFinite(load))return "Unknown goods: take them off";
        var format=new DecimalFormat("#,##0.###",DecimalFormatSymbols.getInstance(Locale.ROOT));
        return load>capacity?"Overloaded: "+format.format(load)+" / "+format.format(capacity):format.format(load)+" / "+format.format(capacity)+" blocks";
    }
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

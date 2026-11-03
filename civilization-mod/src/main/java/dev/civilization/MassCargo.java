package dev.civilization;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** One authoritative ledger, limited by mass or, on pallets, by solid volume. ItemStack is a one-item identity, never an oversized saved stack. */
public final class MassCargo implements CargoInventory {
    public record Entry(ItemStack item,long count){}
    private final List<Entry> entries=new ArrayList<>();
    private final ToDoubleFunction<ItemStack> unit;
    private final DoubleSupplier capacity;
    private final Runnable changed;
    private double cachedLoad;
    private int materialRevision=-1;
    private long revision;
    public MassCargo(DoubleSupplier capacity,Runnable changed){this(CargoMass::unitKg,capacity,changed);}
    public MassCargo(ToDoubleFunction<ItemStack> unit,DoubleSupplier capacity,Runnable changed){this.unit=unit;this.capacity=capacity;this.changed=changed;}
    public double capacity(){return capacity.getAsDouble();}
    public boolean isEmpty(){return entries.isEmpty();}
    public int size(){return entries.size();}
    public long revision(){return revision;}
    public long count(int index){return index>=0&&index<entries.size()?entries.get(index).count:0;}
    public Entry entry(int index){var e=entries.get(index);return new Entry(e.item.copy(),e.count);}
    public int indexOf(ItemStack item){
        for(int i=0;i<entries.size();i++)if(ItemStack.isSameItemSameComponents(entries.get(i).item,item))return i;
        return -1;
    }
    /** Unknown goods count as infinite, so a store holding them accepts nothing until they are unloaded. */
    public double load(){
        if(materialRevision!=PhysicalMaterials.revision()){
            cachedLoad=0;
            for(var e:entries){double u=unit.applyAsDouble(e.item);cachedLoad+=Double.isFinite(u)?u*e.count:Double.POSITIVE_INFINITY;}
            materialRevision=PhysicalMaterials.revision();
        }
        return cachedLoad;
    }
    private void dirty(){materialRevision=-1;revision++;changed.run();}
    public long insert(ItemStack item,long offered){
        double u=unit.applyAsDouble(item);
        if(offered<=0||!Double.isFinite(u)||u<=0)return 0;
        double free=Math.max(0,capacity()-load());
        long n=Math.min(offered,(long)Math.floor(free/u));
        while(n>0&&n*u>free)n--; // Never round a capacity boundary upward.
        if(n==0)return 0;
        int i=indexOf(item);
        if(i<0)entries.add(new Entry(item.copyWithCount(1),n));
        else{var e=entries.get(i);n=Math.min(n,Long.MAX_VALUE-e.count);if(n==0)return 0;entries.set(i,new Entry(e.item,e.count+n));}
        dirty();return n;
    }
    public long remove(int index,long requested){
        if(index<0||index>=entries.size()||requested<=0)return 0;
        var e=entries.get(index);long n=Math.min(e.count,requested);
        if(n==e.count)entries.remove(index);else entries.set(index,new Entry(e.item,e.count-n));
        dirty();return n;
    }
    public ListTag save(HolderLookup.Provider registry){
        var list=new ListTag();
        for(var e:entries){var tag=new CompoundTag();tag.put("item",e.item.save(registry));tag.putLong("count",e.count);list.add(tag);}
        return list;
    }
    public void load(ListTag list,HolderLookup.Provider registry){
        entries.clear();
        for(int i=0;i<list.size();i++){
            var tag=list.getCompound(i);var item=ItemStack.parseOptional(registry,tag.getCompound("item"));long count=tag.getLong("count");
            if(!item.isEmpty()&&count>0)entries.add(new Entry(item.copyWithCount(1),count));
        }
        // Retain existing overfull/unsupported stock for recovery after capacity, tuning or datapack changes.
        dirty();
    }
}

package dev.civilization;

import java.util.*;
import java.util.function.DoubleSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** One authoritative ledger. ItemStack is a one-item identity, never an oversized saved stack. */
public final class MassCargo {
    public record Entry(ItemStack item,long count){}
    private final List<Entry> entries=new ArrayList<>();
    private final DoubleSupplier capacity;
    private final Runnable changed;
    private double cachedMass;
    private int materialRevision=-1;
    private long revision;
    public MassCargo(DoubleSupplier capacity,Runnable changed){this.capacity=capacity;this.changed=changed;}
    public double capacity(){return capacity.getAsDouble();}
    public boolean isEmpty(){return entries.isEmpty();}
    public int size(){return entries.size();}
    public long revision(){return revision;}
    public long count(int index){return index>=0&&index<entries.size()?entries.get(index).count:0;}
    public Entry entry(int index){var e=entries.get(index);return new Entry(e.item.copy(),e.count);}
    public double mass(){
        if(materialRevision!=PhysicalMaterials.revision()){
            cachedMass=0;
            for(var e:entries){double unit=CargoMass.unitKg(e.item);cachedMass+=Double.isFinite(unit)?unit*e.count:Double.POSITIVE_INFINITY;}
            materialRevision=PhysicalMaterials.revision();
        }
        return cachedMass;
    }
    private void dirty(){materialRevision=-1;revision++;changed.run();}
    public long insert(ItemStack item,long offered){
        double unit=CargoMass.unitKg(item);
        if(offered<=0||!Double.isFinite(unit)||unit<=0)return 0;
        double free=Math.max(0,capacity()-mass());
        long n=Math.min(offered,(long)Math.floor(free/unit));
        while(n>0&&n*unit>free)n--; // Never round a mass boundary upward.
        if(n==0)return 0;
        for(int i=0;i<entries.size();i++)if(ItemStack.isSameItemSameComponents(entries.get(i).item,item)){
            var e=entries.get(i);n=Math.min(n,Long.MAX_VALUE-e.count);if(n==0)return 0;long total=e.count+n;
            entries.set(i,new Entry(e.item,total));dirty();return n;
        }
        entries.add(new Entry(item.copyWithCount(1),n));dirty();return n;
    }
    public long remove(int index,long requested){
        if(index<0||index>=entries.size()||requested<=0)return 0;
        var e=entries.get(index);long n=Math.min(e.count,requested);
        if(n==e.count)entries.remove(index);else entries.set(index,new Entry(e.item,e.count-n));
        dirty();return n;
    }
    public long transferTo(MassCargo destination){
        if(destination==this)return 0;
        long moved=0;
        for(int i=entries.size()-1;i>=0;i--){var e=entries.get(i);long n=destination.insert(e.item,e.count);if(n>0){remove(i,n);moved=moved>Long.MAX_VALUE-n?Long.MAX_VALUE:moved+n;}}
        return moved;
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
        // Retain existing overfull/unsupported stock for recovery after tuning or datapack changes.
        dirty();
    }
}

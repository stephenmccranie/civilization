package dev.civilization;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** A controller's progress on its native model: the sections built and the exact stacks each cost, refunded once. */
public final class ModeledBuild {
    public int sections;
    /** Set while the machine itself changes its cells, so their removal is not read as damage. */
    public boolean changing;
    public final Map<Integer,List<ItemStack>> paid=new HashMap<>();

    public boolean has(int part){return (sections&(1<<part))!=0;}
    public void save(CompoundTag t,HolderLookup.Provider r){
        var build=new CompoundTag();build.putInt("sections",sections);var list=new ListTag();
        for(var entry:paid.entrySet()){
            var section=new CompoundTag();section.putInt("part",entry.getKey());var stacks=new ListTag();
            for(var stack:entry.getValue())if(!stack.isEmpty())stacks.add(stack.save(r));
            section.put("stacks",stacks);list.add(section);
        }
        build.put("paid",list);t.put("modeled",build);
    }
    public void load(CompoundTag t,HolderLookup.Provider r,MachineModel model){
        var build=t.getCompound("modeled");sections=build.getInt("sections")&model.all;paid.clear();
        for(var entry:build.getList("paid",Tag.TAG_COMPOUND)){
            var section=(CompoundTag)entry;int part=section.getInt("part");if(part<0||part>=model.sections.size())continue;
            var stacks=new ArrayList<ItemStack>();for(var stack:section.getList("stacks",Tag.TAG_COMPOUND))stacks.add(ItemStack.parseOptional(r,(CompoundTag)stack));
            paid.put(part,stacks);
        }
    }
}

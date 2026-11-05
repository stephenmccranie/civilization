package dev.civilization;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Migration-only custody. Ordinary pickups never deposit here. */
public final class BlockCarryRecovery extends SavedData {
    private record Entry(ItemStack item,long count){}
    private final Map<UUID,List<Entry>> balances=new HashMap<>();
    public static BlockCarryRecovery get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BlockCarryRecovery::new,BlockCarryRecovery::load,null),"civilization_block_carry_recovery");}
    public void store(UUID owner,ItemStack stack){
        if(stack.isEmpty())return;
        var entries=balances.computeIfAbsent(owner,k->new ArrayList<>());
        for(int i=0;i<entries.size();i++)if(ItemStack.isSameItemSameComponents(entries.get(i).item,stack)){
            var e=entries.get(i);entries.set(i,new Entry(e.item,Math.addExact(e.count,stack.getCount())));setDirty();return;
        }
        entries.add(new Entry(stack.copyWithCount(1),stack.getCount()));setDirty();
    }
    public long pending(UUID owner){return balances.getOrDefault(owner,List.of()).stream().mapToLong(Entry::count).sum();}
    public int recover(ServerPlayer player){
        var entries=balances.get(player.getUUID());if(entries==null)return 0;
        for(int i=0;i<entries.size();i++){
            var original=entries.get(i);
            if(BlockCarry.filledContainer(original.item)){
                // Unpack one saved container layer per request, never recursively
                // scan inventories or expose hidden blocks as carried luggage.
                var shell=original.item.copyWithCount(1);
                var contents=new ArrayList<ItemStack>(shell.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyStream().toList());
                boolean bundle=contents.isEmpty();
                if(bundle)contents.addAll(shell.getOrDefault(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,net.minecraft.world.item.component.BundleContents.EMPTY).itemCopyStream().toList());
                var child=contents.removeFirst();
                if(bundle)shell.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,new net.minecraft.world.item.component.BundleContents(contents));
                else shell.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(contents));
                if(original.count==1)entries.remove(i);else entries.set(i,new Entry(original.item,original.count-1));
                entries.add(new Entry(child.copyWithCount(1),child.getCount()));entries.add(new Entry(shell,1));setDirty();return -1;
            }
            var e=entries.get(i);var offered=e.item.copyWithCount((int)Math.min(32,e.count));int n=BlockCarry.insert(player.getInventory(),offered);
            if(!BlockCarry.carried(e.item)){player.getInventory().add(offered);n=(int)Math.min(32,e.count)-offered.getCount();}
            if(n==0)continue;
            if(n==e.count)entries.remove(i);else entries.set(i,new Entry(e.item,e.count-n));
            if(entries.isEmpty())balances.remove(player.getUUID());setDirty();player.containerMenu.broadcastChanges();return n;
        }return 0;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider r){
        var list=new ListTag();balances.forEach((owner,entries)->{for(var e:entries){var t=new CompoundTag();t.putUUID("Owner",owner);t.put("Item",e.item.save(r));t.putLong("Count",e.count);list.add(t);}});tag.put("Blocks",list);return tag;
    }
    public static BlockCarryRecovery load(CompoundTag tag,HolderLookup.Provider r){
        var data=new BlockCarryRecovery();var list=tag.getList("Blocks",10);
        for(int i=0;i<list.size();i++){var t=list.getCompound(i);var item=ItemStack.parseOptional(r,t.getCompound("Item"));long n=t.getLong("Count");if(!item.isEmpty()&&n>0&&t.hasUUID("Owner"))data.balances.computeIfAbsent(t.getUUID("Owner"),k->new ArrayList<>()).add(new Entry(item.copyWithCount(1),n));}
        return data;
    }
}

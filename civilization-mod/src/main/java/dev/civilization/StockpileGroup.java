package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One cached menu over side-connected pallets. Every pallet keeps its own saved stack; the group only
 * chooses which stack goods go to or come from, so merging and splitting never copy stock.
 */
final class StockpileGroup implements CargoInventory {
    private final List<StockpileEntity> members;
    private final List<CargoLedger.Entry> entries=new ArrayList<>();
    private boolean valid=true,dirty=true;
    private long revision;
    private StockpileGroup(List<StockpileEntity> members){this.members=members;}
    static StockpileGroup find(StockpileEntity start){
        var found=new HashMap<BlockPos,StockpileEntity>();var pending=new ArrayDeque<StockpileEntity>();pending.add(start);found.put(start.getBlockPos(),start);
        while(!pending.isEmpty()){
            var current=pending.removeFirst();
            for(var direction:Direction.Plane.HORIZONTAL){
                var at=current.getBlockPos().relative(direction);
                if(!found.containsKey(at)&&current.getLevel().hasChunkAt(at)&&current.getLevel().getBlockEntity(at) instanceof StockpileEntity next&&!next.isRemoved()){
                    found.put(at,next);pending.add(next);
                }
            }
        }
        var members=new ArrayList<>(found.values());members.sort(Comparator.comparing(StockpileEntity::getBlockPos));
        var group=new StockpileGroup(List.copyOf(members));
        for(var member:members)if(member.group!=null)member.group.invalidate();
        for(var member:members)member.group=group;
        return group;
    }
    static void invalidateAround(Level level,BlockPos at){
        if(level==null||level.isClientSide)return;
        invalidateAt(level,at);
        for(var direction:Direction.Plane.HORIZONTAL)invalidateAt(level,at.relative(direction));
    }
    private static void invalidateAt(Level level,BlockPos at){
        if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof StockpileEntity s&&s.group!=null)s.group.invalidate();
    }
    void invalidate(){
        if(!valid)return;valid=false;
        for(var member:members)if(member.group==this)member.group=null;
    }
    void changed(){dirty=true;revision++;}
    boolean allowed(Player p){return valid&&members.stream().allMatch(s->!s.isRemoved()&&CivicAccess.allowed(s.getLevel(),s.getBlockPos(),p));}
    int pallets(){return members.size();}
    private void refresh(){
        if(!dirty)return;
        entries.clear();
        for(var member:members)for(int i=0;i<member.stack().size();i++){
            var e=member.stack().entry(i);long left=e.count();
            for(int j=0;j<entries.size()&&left>0;j++){
                var existing=entries.get(j);
                if(ItemStack.isSameItemSameComponents(existing.item(),e.item())){
                    long n=Math.min(left,Long.MAX_VALUE-existing.count());
                    if(n>0){entries.set(j,new CargoLedger.Entry(existing.item(),existing.count()+n));left-=n;}
                }
            }
            if(left>0)entries.add(new CargoLedger.Entry(e.item(),left));
        }
        dirty=false;
    }
    @Override public double capacity(){return members.size()*(double)StockpileEntity.CAPACITY;}
    @Override public double load(){double load=0;for(var member:members)load+=member.stack().load();return load;}
    @Override public int size(){refresh();return entries.size();}
    @Override public long revision(){return revision;}
    @Override public long count(int index){refresh();return index>=0&&index<entries.size()?entries.get(index).count():0;}
    @Override public CargoLedger.Entry entry(int index){refresh();var e=entries.get(index);return new CargoLedger.Entry(e.item().copy(),e.count());}
    /** Pallets already holding the item, then empty pallets, then on top of other goods; each fills to a full stack before the next. */
    @Override public long insert(ItemStack item,long offered){
        if(!valid||offered<=0)return 0;
        var order=new ArrayList<>(members);
        order.sort(Comparator.comparingInt(s->s.stack().indexOf(item)>=0?0:s.stack().isEmpty()?1:2));
        long left=offered;
        for(var member:order){left-=member.stack().insert(item,left);if(left==0)break;}
        return offered-left;
    }
    /** Overloaded pallets give up goods first, so pre-limit stock drains; otherwise the last pallet empties first. */
    @Override public long remove(int index,long requested){
        if(!valid||requested<=0||index<0||index>=size())return 0;
        var item=entries.get(index).item();long left=Math.min(requested,entries.get(index).count()),wanted=left;
        var order=new ArrayList<>(members.reversed());
        order.sort(Comparator.comparing(s->s.stack().load()<=s.stack().capacity()));
        for(var member:order){
            int at=member.stack().indexOf(item);
            if(at>=0)left-=member.stack().remove(at,left);
            if(left==0)break;
        }
        return wanted-left;
    }
    @Override public long transferTo(CargoInventory destination){return valid?CargoInventory.super.transferTo(destination):0;}
}

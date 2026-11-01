package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One cached view over independently saved pallets. Merging never copies stored stock. */
final class StockpileGroup implements CargoInventory {
    private final List<StockpileEntity> members;
    private final List<MassCargo.Entry> entries=new ArrayList<>();
    private boolean valid=true,dirty=true;
    private long revision;
    private int materialRevision=-1;
    private double mass;
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
        if(dirty){
            entries.clear();
            for(var member:members)for(int i=0;i<member.localCargo().size();i++){
                var e=member.localCargo().entry(i);long left=e.count();
                for(int j=0;j<entries.size()&&left>0;j++){
                    var existing=entries.get(j);
                    if(ItemStack.isSameItemSameComponents(existing.item(),e.item())){
                        long n=Math.min(left,Long.MAX_VALUE-existing.count());
                        if(n>0){entries.set(j,new MassCargo.Entry(existing.item(),existing.count()+n));left-=n;}
                    }
                }
                if(left>0)entries.add(new MassCargo.Entry(e.item(),left));
            }
        }
        if(dirty||materialRevision!=PhysicalMaterials.revision()){
            mass=0;for(var member:members)mass+=member.localCargo().mass();
            materialRevision=PhysicalMaterials.revision();
        }
        dirty=false;
    }
    @Override public double capacity(){return members.size()*CargoConfig.STOCKPILE.get();}
    @Override public double mass(){refresh();return mass;}
    @Override public int size(){refresh();return entries.size();}
    @Override public long revision(){return revision;}
    @Override public long count(int index){refresh();return index>=0&&index<entries.size()?entries.get(index).count():0;}
    @Override public MassCargo.Entry entry(int index){refresh();var e=entries.get(index);return new MassCargo.Entry(e.item().copy(),e.count());}
    @Override public long insert(ItemStack item,long offered){
        if(!valid||offered<=0)return 0;
        double unit=CargoMass.unitKg(item);if(!Double.isFinite(unit)||unit<=0)return 0;
        double free=Math.max(0,capacity()-mass());long accepted=Math.min(offered,(long)Math.floor(free/unit));
        while(accepted>0&&accepted*unit>free)accepted--;
        long left=accepted;
        for(var member:members){left-=member.localCargo().insert(item,left);if(left==0)break;}
        // Capacity is shared, including small remainders across pallets. One
        // indivisible item may straddle those mass allowances; never add a
        // hidden per-pallet packing limit. A later split preserves overfull stock.
        if(left>0)for(var member:members){left-=member.localCargo().store(item,left);if(left==0)break;}
        return accepted-left;
    }
    @Override public long remove(int index,long requested){
        if(!valid||requested<=0||index<0||index>=size())return 0;
        var e=entry(index);long left=Math.min(requested,e.count()),wanted=left;
        for(var member:members){
            var local=member.localCargo();
            for(int i=local.size()-1;i>=0&&left>0;i--)if(ItemStack.isSameItemSameComponents(local.entry(i).item(),e.item()))left-=local.remove(i,left);
            if(left==0)break;
        }
        return wanted-left;
    }
    @Override public long transferTo(CargoInventory destination){return valid?CargoInventory.super.transferTo(destination):0;}
}

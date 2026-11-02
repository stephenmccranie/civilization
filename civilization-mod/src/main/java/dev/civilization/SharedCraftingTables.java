package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

/** The vanilla table's nine inputs belong to its world position, never to a viewer. */
public final class SharedCraftingTables extends SavedData {
    private final Map<BlockPos,Board> tables=new HashMap<>();
    public static SharedCraftingTables get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(new Factory<>(SharedCraftingTables::new,SharedCraftingTables::load,null),"civilization_crafting_tables");
    }
    public Grid open(ServerLevel level,BlockPos pos,AbstractContainerMenu menu,Player player){
        var board=tables.computeIfAbsent(pos.immutable(),p->new Board(p));
        var grid=new Grid(level,board,menu,player);board.viewers.add(grid);return grid;
    }
    private final class Board {
        final BlockPos pos;
        final NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
        final Set<Grid> viewers=new HashSet<>();
        boolean removed;
        Board(BlockPos pos){this.pos=pos;}
        void changed(){
            setDirty();
            for(var grid:List.copyOf(viewers)){
                grid.menu.slotsChanged(grid);
                grid.menu.broadcastChanges();
            }
        }
        void release(){if(viewers.isEmpty()&&items.stream().allMatch(ItemStack::isEmpty))tables.remove(pos,this);}
    }
    public final class Grid extends TransientCraftingContainer {
        private final ServerLevel level;
        private final Board board;
        private final AbstractContainerMenu menu;
        private final Player player;
        private Grid(ServerLevel level,Board board,AbstractContainerMenu menu,Player player){
            super(menu,3,3,board.items);this.level=level;this.board=board;this.menu=menu;this.player=player;
        }
        public void close(){board.viewers.remove(this);board.release();}
        @Override public void setChanged(){board.changed();}
        @Override public void setItem(int slot,ItemStack stack){board.items.set(slot,stack);setChanged();}
        @Override public ItemStack removeItem(int slot,int amount){
            var stack=ContainerHelper.removeItem(board.items,slot,amount);if(!stack.isEmpty())setChanged();return stack;
        }
        @Override public ItemStack removeItemNoUpdate(int slot){
            var stack=ContainerHelper.takeItem(board.items,slot);if(!stack.isEmpty())setChanged();return stack;
        }
        @Override public void clearContent(){board.items.clear();setChanged();}
        @Override public boolean stillValid(Player player){
            return !board.removed&&player.level()==level&&level.hasChunkAt(board.pos)
                    &&level.getBlockState(board.pos).is(Blocks.CRAFTING_TABLE)
                    &&player.distanceToSqr(CivicAccess.world(level,board.pos))<=64&&CivicAccess.allowed(level,board.pos,player);
        }
    }
    public void removed(ServerLevel level,BlockPos pos){
        var board=tables.remove(pos);if(board==null)return;
        board.removed=true;setDirty();
        Containers.dropContents(level,pos,board.items);
        board.items.clear();board.changed();
        for(var grid:List.copyOf(board.viewers)){
            if(grid.player.containerMenu==grid.menu)grid.player.closeContainer();else grid.close();
        }
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registry){
        var list=new ListTag();
        for(var board:tables.values()){
            if(board.items.stream().allMatch(ItemStack::isEmpty))continue;
            var entry=new CompoundTag();entry.putLong("Pos",board.pos.asLong());
            ContainerHelper.saveAllItems(entry,board.items,registry);list.add(entry);
        }
        tag.put("Tables",list);return tag;
    }
    public static SharedCraftingTables load(CompoundTag tag,HolderLookup.Provider registry){
        var data=new SharedCraftingTables();var list=tag.getList("Tables",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            var entry=list.getCompound(i);var pos=BlockPos.of(entry.getLong("Pos"));var board=data.new Board(pos);
            ContainerHelper.loadAllItems(entry,board.items,registry);data.tables.put(pos,board);
        }
        return data;
    }
}

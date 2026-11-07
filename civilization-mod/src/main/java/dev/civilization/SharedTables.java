package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A vanilla table's input slots belong to its world position, never to a viewer: a crafting table's nine inputs and an
 * enchanting table's item and lapis. Every viewer's menu works on the same stacks, and each kind of table is saved with
 * the dimension.
 */
public final class SharedTables extends SavedData {
    public enum Kind {
        CRAFTING(Blocks.CRAFTING_TABLE,3,3,"civilization_crafting_tables"),
        ENCHANTING(Blocks.ENCHANTING_TABLE,2,1,"civilization_enchanting_tables");
        final Block block;
        final int width,height;
        final String storage;
        Kind(Block block,int width,int height,String storage){this.block=block;this.width=width;this.height=height;this.storage=storage;}
        static Kind of(BlockState state){for(var kind:values())if(state.is(kind.block))return kind;return null;}
    }
    private final Kind kind;
    private final Map<BlockPos,Board> tables=new HashMap<>();
    private SharedTables(Kind kind){this.kind=kind;}
    public static SharedTables get(ServerLevel level,Kind kind){
        return level.getDataStorage().computeIfAbsent(new Factory<>(()->new SharedTables(kind),(tag,registry)->load(kind,tag,registry),null),kind.storage);
    }
    /** The shared slots of the table the menu was opened on, or null where the menu has no placed table of this kind. */
    public static Grid open(ContainerLevelAccess access,Kind kind,AbstractContainerMenu menu,Player player){
        return access.evaluate((level,pos)->level instanceof ServerLevel server&&level.getBlockState(pos).is(kind.block)?get(server,kind).open(server,pos,menu,player):null,null);
    }
    public Grid open(ServerLevel level,BlockPos pos,AbstractContainerMenu menu,Player player){
        var board=tables.computeIfAbsent(pos.immutable(),Board::new);
        var grid=new Grid(level,board,menu,player);board.viewers.add(grid);return grid;
    }
    private final class Board {
        final BlockPos pos;
        final NonNullList<ItemStack> items=NonNullList.withSize(kind.width*kind.height,ItemStack.EMPTY);
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
            super(menu,kind.width,kind.height,board.items);this.level=level;this.board=board;this.menu=menu;this.player=player;
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
            return !board.removed&&player.level()==level&&Chunks.loaded(level,board.pos)
                    &&level.getBlockState(board.pos).is(kind.block)
                    &&player.distanceToSqr(CivicAccess.world(level,board.pos))<=64&&CivicAccess.allowed(level,board.pos,player);
        }
    }
    /** A table broken or replaced by another block spills its slots once and closes every open menu on it. */
    public static void removed(ServerLevel level,BlockPos pos,BlockState state,BlockState next){
        var kind=Kind.of(state);if(kind!=null&&!next.is(kind.block))get(level,kind).removed(level,pos);
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
    public static SharedTables load(Kind kind,CompoundTag tag,HolderLookup.Provider registry){
        var data=new SharedTables(kind);var list=tag.getList("Tables",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            var entry=list.getCompound(i);var pos=BlockPos.of(entry.getLong("Pos"));var board=data.new Board(pos);
            ContainerHelper.loadAllItems(entry,board.items,registry);data.tables.put(pos,board);
        }
        return data;
    }
}

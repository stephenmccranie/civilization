package dev.civilization;

import dev.civilization.mixin.CompoundContainerAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * A chest, barrel, chest boat or chest minecart holds one armful of what the hand carries ({@link BlockCarry#carried}):
 * at most {@link BlockCarry#LIMIT} in all, each container block on its own, so a double chest holds two. Other goods keep
 * their slots. One holding more keeps it all and can be emptied, never added to. Menu slots and automation both ask
 * {@link #room}, which counts the chest only when an insertion is evaluated.
 */
public final class ChestArmful {
    private ChestArmful(){}
    /** A slot of one chest, a double chest resolved to its half. */
    private record Place(Container chest,int slot){
        /** Carried goods in the chest's other slots. */
        int others(){
            int n=0;
            for(int i=0;i<chest.getContainerSize();i++)if(i!=slot&&BlockCarry.carried(chest.getItem(i)))n+=chest.getItem(i).getCount();
            return n;
        }
    }
    private static Place place(Container c,int slot){
        if(c instanceof CompoundContainerAccess pair){
            var first=pair.civilization$first();int n=first.getContainerSize();
            return slot<n?place(first,slot):place(pair.civilization$second(),slot-n);
        }
        return c instanceof ChestBlockEntity||c instanceof BarrelBlockEntity||c instanceof ChestBoat||c instanceof MinecartChest?new Place(c,slot):null;
    }
    /** How many of stack this slot may hold: the armful less the carried goods in the chest's other slots. */
    public static int room(Container c,int slot,ItemStack stack){
        var at=BlockCarry.carried(stack)?place(c,slot):null;
        return at==null?Integer.MAX_VALUE:Math.max(0,BlockCarry.LIMIT-at.others());
    }
    /** Whether the slot can take count more of stack within the armful. */
    public static boolean takes(Container c,int slot,ItemStack stack,int count){
        var held=c.getItem(slot);
        return room(c,slot,stack)>=count+(ItemStack.isSameItemSameComponents(held,stack)?held.getCount():0);
    }

    /** The carried goods a server-side click offers a chest: the cursor, a number-key or offhand stack, or a stack shift-clicked from outside it. */
    public static ItemStack offered(AbstractContainerMenu menu,int index,int button,ClickType type,Player player){
        if(!(player instanceof ServerPlayer))return ItemStack.EMPTY;
        var slot=index>=0&&index<menu.slots.size()?menu.slots.get(index):null;
        var stack=switch(type){
            case PICKUP->menu.getCarried();
            case QUICK_CRAFT->AbstractContainerMenu.getQuickcraftHeader(button)==2?menu.getCarried():ItemStack.EMPTY;
            case QUICK_MOVE->slot!=null&&place(slot.container,slot.getContainerSlot())==null?slot.getItem():ItemStack.EMPTY;
            case SWAP->button>=0&&button<player.getInventory().getContainerSize()?player.getInventory().getItem(button):ItemStack.EMPTY;
            default->ItemStack.EMPTY;
        };
        return BlockCarry.carried(stack)?stack:ItemStack.EMPTY;
    }
    /** After a click, says why offered goods stayed with the player when every chest it aimed at is full of carried goods. */
    public static void explain(AbstractContainerMenu menu,int index,int button,ClickType type,Player player,ItemStack offered){
        if(offered.isEmpty())return;
        var left=offered(menu,index,button,type,player);
        if(!ItemStack.isSameItemSameComponents(offered,left))return;
        var aimed=type==ClickType.PICKUP||type==ClickType.SWAP?index>=0&&index<menu.slots.size()?java.util.List.of(menu.slots.get(index)):java.util.List.<Slot>of():menu.slots;
        var chests=new java.util.HashSet<Container>();
        for(var slot:aimed){var at=place(slot.container,slot.getContainerSlot());if(at!=null)chests.add(at.chest());}
        if(chests.isEmpty())return;
        int held=0;
        for(var chest:chests){int n=new Place(chest,-1).others();if(n<BlockCarry.LIMIT)return;held+=n;}
        player.displayClientMessage(Component.literal("Building blocks "+held+"/"+chests.size()*BlockCarry.LIMIT
                +" · a chest or barrel holds "+BlockCarry.LIMIT+"; stack more on a Pallet"),true);
    }
}

package dev.civilization;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** One physical building stack: the offhand. Other inventory goods retain native behavior. */
@EventBusSubscriber(modid="civilization")
public final class BlockCarry {
    public static final int OFFHAND=40,LIMIT=32;
    private BlockCarry(){}
    public static boolean block(ItemStack stack){return !stack.isEmpty()&&stack.getItem() instanceof BlockItem;}
    public static boolean filledContainer(ItemStack stack){
        if(stack.isEmpty())return false;
        var container=stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if(container!=null&&container.nonEmptyItems().iterator().hasNext())return true;
        var bundle=stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
        return bundle!=null&&!bundle.isEmpty();
    }
    public static boolean restricted(Player player){return !player.isCreative()&&!player.isSpectator();}
    public static int insert(Inventory inv,ItemStack offered){
        if(!block(offered)||filledContainer(offered))return 0;
        var hand=inv.getItem(OFFHAND);
        if(offered==hand)return 0;
        if(!hand.isEmpty()&&!ItemStack.isSameItemSameComponents(hand,offered))return 0;
        int amount=Math.min(offered.getCount(),Math.max(0,Math.min(LIMIT,offered.getMaxStackSize())-hand.getCount()));
        if(amount==0)return 0;
        if(hand.isEmpty())inv.setItem(OFFHAND,offered.copyWithCount(amount));else hand.grow(amount);
        offered.shrink(amount);inv.setChanged();return amount;
    }
    public static boolean forbiddenClick(AbstractContainerMenu menu,int index,int button,ClickType type,Player player){
        if(!restricted(player)||index<0||index>=menu.slots.size())return false;
        var slot=menu.slots.get(index);boolean wrong=slot.container==player.getInventory()&&slot.getContainerSlot()!=OFFHAND;
        if(type==ClickType.PICKUP&&slot.container==player.getInventory()&&filledContainer(menu.getCarried()))return true;
        if(type==ClickType.PICKUP&&wrong&&block(menu.getCarried()))return true;
        if(type==ClickType.SWAP){
            if(button>=0&&button<9&&(block(slot.getItem())||filledContainer(slot.getItem())))return true;
            if(button==OFFHAND&&filledContainer(slot.getItem()))return true;
            if(button==OFFHAND&&wrong&&block(player.getOffhandItem()))return true;
            if(button>=0&&button<9&&wrong&&block(player.getInventory().getItem(button)))return true;
        }
        return false;
    }
    /** Expose the same hand slot in native chests, crafting and machine menus. */
    public static void attachMenu(AbstractContainerMenu menu,Inventory inv){
        if(!restricted(inv.player))return;
        for(var slot:menu.slots)if(slot.container==inv&&slot.getContainerSlot()==OFFHAND)return;
        for(var slot:menu.slots)if(slot.container==inv&&slot.getContainerSlot()==0){
            ((BlockCarryMenuAccess)menu).civilization$addCarrySlot(new Slot(inv,OFFHAND,slot.x-33,slot.y));return;
        }
    }
    /** Runs only between transactions; old/foreign misplaced stock is saved, never silently deleted. */
    public static long normalize(ServerPlayer player){
        if(!restricted(player)||!player.isAlive())return 0;
        var inv=player.getInventory();long recovered=0;boolean changed=false;
        var hand=inv.getItem(OFFHAND);
        if(filledContainer(hand)){
            inv.setItem(OFFHAND,ItemStack.EMPTY);recovered+=hand.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),hand);changed=true;
        }else if(block(hand)&&hand.getCount()>Math.min(LIMIT,hand.getMaxStackSize())){
            var extra=hand.split(hand.getCount()-Math.min(LIMIT,hand.getMaxStackSize()));
            recovered+=extra.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),extra);changed=true;
        }
        for(int i=0;i<OFFHAND;i++)if(block(inv.getItem(i))||filledContainer(inv.getItem(i))){
            var item=inv.getItem(i);inv.setItem(i,ItemStack.EMPTY);insert(inv,item);changed=true;
            if(!item.isEmpty()){recovered+=item.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),item);}
        }
        if(changed){inv.setChanged();player.containerMenu.broadcastChanges();}
        if(recovered>0)player.displayClientMessage(Component.literal("Blocks now travel in one offhand stack. Extra blocks were saved safely; free your offhand and use /civilization recoverblocks."),false);
        return recovered;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p)normalize(p);}
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&normalize(p)==0&&BlockCarryRecovery.get(p.server).pending(p.getUUID())>0)
            p.displayClientMessage(Component.literal("Saved block recovery is available: /civilization recoverblocks"),false);
    }
    @SubscribeEvent public static void swap(LivingSwapItemsEvent.Hands event){
        if(event.getEntity() instanceof Player p&&restricted(p)&&(block(event.getItemSwappedToMainHand())||filledContainer(event.getItemSwappedToOffHand())))event.setCanceled(true);
    }
}

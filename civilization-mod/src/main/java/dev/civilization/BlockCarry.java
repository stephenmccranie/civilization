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

/** One physical stack in the arms, the offhand: building blocks and buckets. Other inventory goods retain native behavior. */
@EventBusSubscriber(modid="civilization")
public final class BlockCarry {
    public static final int OFFHAND=40;
    /** An armful: an eighth of a block of space at cargo density (CargoGrid), so eight blocks, cut pieces or buckets. */
    public static final int LIMIT=8;
    public static final net.minecraft.tags.TagKey<Item> INVENTORY_PLACEABLES=net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("civilization:inventory_placeables"));
    private BlockCarry(){}
    /** Building cargo, not every item Minecraft happens to implement as a BlockItem, such as a bucket of powder snow. */
    public static boolean block(ItemStack stack){
        if(stack.isEmpty()||!(stack.getItem() instanceof BlockItem item)||stack.getItem() instanceof ItemNameBlockItem
                ||stack.is(CargoContent.STOCKPILE.get().asItem())||stack.is(INVENTORY_PLACEABLES)||stack.is(net.neoforged.neoforge.common.Tags.Items.BUCKETS))return false;
        var block=item.getBlock();
        return !(block instanceof net.minecraft.world.level.block.BushBlock)
                &&!(block instanceof net.minecraft.world.level.block.GrowingPlantBlock);
    }
    /** What travels in the arms, the offhand or a worn backpack: building blocks and buckets, empty or filled. */
    public static boolean carried(ItemStack stack){return block(stack)||stack.is(net.neoforged.neoforge.common.Tags.Items.BUCKETS);}
    public static boolean filledContainer(ItemStack stack){
        if(stack.isEmpty())return false;
        var container=stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if(container!=null&&container.nonEmptyItems().iterator().hasNext())return true;
        var bundle=stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
        return bundle!=null&&!bundle.isEmpty();
    }
    public static boolean restricted(Player player){return !player.isCreative()&&!player.isSpectator();}
    /** Into the hand, then on the server into the worn backpack. */
    public static int insert(Inventory inv,ItemStack offered){
        int n=hold(inv,offered);
        if(!offered.isEmpty()&&inv.player instanceof ServerPlayer p&&Backpack.worn(p) instanceof Backpack.Contents pack){int before=offered.getCount();pack.insert(offered);n+=before-offered.getCount();}
        return n;
    }
    /** Into the hand alone. */
    public static int hold(Inventory inv,ItemStack offered){
        if(!carried(offered)||filledContainer(offered))return 0;
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
        if(type==ClickType.PICKUP&&wrong&&carried(menu.getCarried()))return true;
        if(type==ClickType.SWAP){
            if(button>=0&&button<9&&(carried(slot.getItem())||filledContainer(slot.getItem())))return true;
            if(button==OFFHAND&&filledContainer(slot.getItem()))return true;
            if(button==OFFHAND&&wrong&&carried(player.getOffhandItem()))return true;
            if(button>=0&&button<9&&wrong&&carried(player.getInventory().getItem(button)))return true;
        }
        return false;
    }
    /** The offhand shown beside a menu at hotbar height; the client stands it against the menu's side. */
    public static final class HandSlot extends Slot {
        public HandSlot(Inventory inv,int y){super(inv,OFFHAND,0,y);}
    }
    /** Expose the same hand slot in native chests, crafting and machine menus. */
    public static void attachMenu(AbstractContainerMenu menu,Inventory inv){
        if(!restricted(inv.player))return;
        for(var slot:menu.slots)if(slot.container==inv&&slot.getContainerSlot()==OFFHAND)return;
        for(var slot:menu.slots)if(slot.container==inv&&slot.getContainerSlot()==0){
            ((BlockCarryMenuAccess)menu).civilization$addCarrySlot(new HandSlot(inv,slot.y));return;
        }
    }
    /** Runs only between transactions; old/foreign misplaced stock is saved, never silently deleted. */
    public static long normalize(ServerPlayer player){
        if(!restricted(player)||!player.isAlive())return 0;
        var inv=player.getInventory();long recovered=0;boolean changed=false;
        var hand=inv.getItem(OFFHAND);
        if(filledContainer(hand)){
            inv.setItem(OFFHAND,ItemStack.EMPTY);recovered+=hand.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),hand);changed=true;
        }else if(carried(hand)&&hand.getCount()>Math.min(LIMIT,hand.getMaxStackSize())){
            var extra=hand.split(hand.getCount()-Math.min(LIMIT,hand.getMaxStackSize()));
            recovered+=extra.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),extra);changed=true;
        }
        for(int i=0;i<OFFHAND;i++)if(carried(inv.getItem(i))||filledContainer(inv.getItem(i))){
            var item=inv.getItem(i);inv.setItem(i,ItemStack.EMPTY);insert(inv,item);changed=true;
            if(!item.isEmpty()){recovered+=item.getCount();BlockCarryRecovery.get(player.server).store(player.getUUID(),item);}
        }
        if(changed){inv.setChanged();player.containerMenu.broadcastChanges();}
        if(recovered>0)player.displayClientMessage(Component.literal("Blocks and buckets travel in one offhand stack of up to "+LIMIT+". Extra ones were saved safely; free your offhand and use /civilization recoverblocks."),false);
        return recovered;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p)normalize(p);}
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&normalize(p)==0&&BlockCarryRecovery.get(p.server).pending(p.getUUID())>0)
            p.displayClientMessage(Component.literal("Saved block recovery is available: /civilization recoverblocks"),false);
    }
    /** Drop throws from the hand's carried stack before the main hand: one item, or the whole stack with Ctrl. */
    public static boolean dropsFromHand(Player p){return restricted(p)&&carried(p.getOffhandItem());}
    public static ItemStack takeDrop(Player p,boolean all){var hand=p.getOffhandItem();return hand.split(all?hand.getCount():1);}
    @SubscribeEvent public static void swap(LivingSwapItemsEvent.Hands event){
        if(event.getEntity() instanceof Player p&&restricted(p)&&(carried(event.getItemSwappedToMainHand())||filledContainer(event.getItemSwappedToOffHand())))event.setCanceled(true);
    }
}

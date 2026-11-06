package dev.civilization;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;

/**
 * What a player crafts from beyond the pockets: the Hand, the worn pack and the open menu's nearby stores. The recipe
 * book's craftable marks, JEI's missing items and the server's placement all count the same goods.
 */
public final class CraftingSources {
    /** More than any grid takes, so a large stock never overflows the count. */
    private static final int MOST=9*64;
    private CraftingSources(){}
    /** Adds these goods to the pockets' count. */
    public static void account(Player p,StackedContents contents){
        var hand=p.getOffhandItem();if(BlockCarry.carried(hand))contents.accountSimpleStack(hand);
        var pack=Backpack.worn(p);
        if(pack!=null)for(var stack:pack.getItems())contents.accountSimpleStack(stack);
        var near=NearbyStores.of(p.containerMenu);
        if(near!=null)for(int i=0;i<near.size();i++){var e=near.entry(i);contents.accountSimpleStack(e.item().copyWithCount((int)Math.min(MOST,e.count())));}
    }
    /** Takes up to n of the item for the grid, from the Hand, then the pack, then the nearest stores; returns how many. */
    public static int take(Player p,ItemStack item,int n){
        int taken=0;var hand=p.getOffhandItem();
        if(ItemStack.isSameItemSameComponents(hand,item)){taken=Math.min(n,hand.getCount());hand.shrink(taken);}
        var pack=Backpack.worn(p);if(pack!=null&&taken<n)taken+=pack.take(item,n-taken);
        var near=NearbyStores.of(p.containerMenu);if(near!=null&&taken<n)taken+=(int)near.remove(near.indexOf(item),n-taken);
        if(taken>0)p.getInventory().setChanged();
        return taken;
    }
}

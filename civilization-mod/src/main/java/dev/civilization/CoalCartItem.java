package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class CoalCartItem extends Item {
    public CoalCartItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){
        var l=c.getLevel();var pos=c.getClickedPos();if(!l.getBlockState(pos).is(BlockTags.RAILS)||c.getPlayer()==null)return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(!l.mayInteract(c.getPlayer(),pos)||!CivicAccess.allowed(l,pos,c.getPlayer()))return InteractionResult.FAIL;
        var rail=(net.minecraft.world.level.block.BaseRailBlock)l.getBlockState(pos).getBlock();
        boolean rising=l.getBlockState(pos).getValue(rail.getShapeProperty()).isAscending();
        var e=new CoalMinecart(CoalMiningContent.CART.get(),l);e.setPos(pos.getX()+.5,pos.getY()+.15+(rising?.5:0),pos.getZ()+.5);
        if(!l.noCollision(e)||!l.addFreshEntity(e))return InteractionResult.FAIL;
        if(!c.getPlayer().isCreative())c.getItemInHand().shrink(1);return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("Place on rails; right-click with a loaded Coal Shovel."));lines.add(Component.literal("Empty hand: push · park beside the Preparation Screen."));}
}

package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CoalShovelItem extends Item {
    public static final int CAPACITY=16; // Four prepared Coal per shovel load.
    public CoalShovelItem(){super(new Properties().durability(512));}
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();var l=c.getLevel();if(p==null||c.getHand()!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        var s=l.getBlockState(c.getClickedPos());if(!s.is(CoalMiningContent.PILE.get()))return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(!l.mayInteract(p,c.getClickedPos())||!CivicAccess.allowed(l,c.getClickedPos(),p))return InteractionResult.FAIL;
        var tool=c.getItemInHand();int old=CoalMiningContent.load(tool),n=Math.min(CAPACITY-old,s.getValue(RawCoalPileBlock.UNITS));if(n<=0)return InteractionResult.CONSUME;
        int rest=s.getValue(RawCoalPileBlock.UNITS)-n;
        boolean changed=rest==0?l.removeBlock(c.getClickedPos(),false):l.setBlock(c.getClickedPos(),s.setValue(RawCoalPileBlock.UNITS,rest),3);
        if(!changed)return InteractionResult.FAIL;CoalMiningContent.load(tool,old+n);
        if(!p.isCreative())CalorieFoodData.of(p).spendLabor(p,1,false,"coal_shoveling");
        l.playSound(null,c.getClickedPos(),net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,.7f,.65f);return InteractionResult.CONSUME;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack s,Player p,LivingEntity e,InteractionHand h){return InteractionResult.PASS;}
    @Override public boolean canAttackBlock(BlockState b,Level l,BlockPos pos,Player p){return CoalMiningContent.load(p.getMainHandItem())==0;}
    @Override public boolean shouldCauseReequipAnimation(ItemStack a,ItemStack b,boolean slot){return slot||!a.is(b.getItem());}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("Right-click loose coal to scoop; right-click a Coal Minecart to load."));lines.add(Component.literal("Load: "+CoalMiningContent.load(s)+" / "+CAPACITY+" raw units · 4 units prepare into 1 Coal"));}
}

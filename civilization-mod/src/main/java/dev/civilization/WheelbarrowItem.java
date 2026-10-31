package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class WheelbarrowItem extends Item {
    public WheelbarrowItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();if(p==null||c.getClickedFace()!=Direction.UP)return InteractionResult.PASS;
        var l=c.getLevel();if(l.isClientSide)return InteractionResult.SUCCESS;
        var hit=c.getClickLocation();var e=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),l);
        e.setYRot(p.getYRot());e.setPos(hit.x,hit.y+.01,hit.z);
        if(!e.accessible(p,e.getBoundingBox())||!l.noCollision(e)||!l.addFreshEntity(e))return InteractionResult.FAIL;
        if(!p.isCreative())c.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> lines,TooltipFlag f){
        lines.add(Component.literal("Place on clear ground; right-click to take the handles."));
        lines.add(Component.literal("W/S: push/back up · A/D: turn · Sneak: release"));
        lines.add(Component.literal("Sneak + empty-hand right-click: recover when parked. Empty prototype."));
    }
}

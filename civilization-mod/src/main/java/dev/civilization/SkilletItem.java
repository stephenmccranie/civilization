package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** One movable vessel. It consumes on placement even in Creative: its batch is never copied. */
public final class SkilletItem extends Item {
    public SkilletItem() { super(new Properties().stacksTo(1)); }
    public static SkilletContents contents(ItemStack stack) { return SkilletContents.load(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()); }
    public static void contents(ItemStack stack, SkilletContents s) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(s.save())); }
    public static ItemStack stack(SkilletContents s) { var stack = PrototypeStoveContent.SKILLET.toStack(); contents(stack, s); return stack; }
    public static SkilletContents contents(ItemStack stack, Level l) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var s=SkilletContents.load(tag);
        if(l!=null&&tag.contains("warmthTick"))s.cool(Math.max(0,l.getGameTime()-tag.getLong("warmthTick")));
        return s;
    }
    public static void contents(ItemStack stack, SkilletContents s, Level l) { var tag=s.save();tag.putLong("warmthTick",l.getGameTime());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); }
    public static ItemStack stack(SkilletContents s, Level l) { var stack=PrototypeStoveContent.SKILLET.toStack();contents(stack,s,l);return stack; }
    /** Cooling changes components, not the vessel being held. */
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }
    @Override public InteractionResult useOn(UseOnContext c) {
        var l = c.getLevel(); var p = c.getPlayer(); var pos = c.getClickedPos();
        if (p == null || !CivicAccess.allowed(l, pos, p)) return InteractionResult.FAIL;
        if (l.getBlockEntity(pos) instanceof PrototypeStoveEntity stove) {
            if (stove.hasSkillet()) return InteractionResult.FAIL;
            if (!l.isClientSide) {
                if (!stove.valid(p) || !stove.putSkillet(c.getItemInHand())) return InteractionResult.FAIL;
                // Creative's use-on path restores the old stack count; replace its custody too.
                p.setItemInHand(c.getHand(),ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        var target = pos.above();
        if (c.getClickedFace() != Direction.UP || !l.getBlockState(pos).isFaceSturdy(l, pos, Direction.UP)
                || !l.getBlockState(target).isAir() || !p.mayUseItemAt(target, Direction.UP, c.getItemInHand())
                || !CivicAccess.allowed(l, target, p)) return InteractionResult.FAIL;
        if (!l.isClientSide) {
            var state = PrototypeStoveContent.RESTING_SKILLET.get().defaultBlockState().setValue(CivicBlock.FACING, p.getDirection().getOpposite());
            if (!l.setBlock(target, state, 3)) return InteractionResult.FAIL;
            if (!(l.getBlockEntity(target) instanceof RestingSkilletEntity pan)) { l.removeBlock(target, false); return InteractionResult.FAIL; }
            pan.put(contents(c.getItemInHand(),l)); c.getItemInHand().shrink(1);p.setItemInHand(c.getHand(),ItemStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack, Level l, Entity owner, int slot, boolean selected) {
        if (l.isClientSide || l.getGameTime() % 10 != 0) return;
        if(contents(stack).warmth()==0)return;
        var s = contents(stack,l); contents(stack,s,l);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext c, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(contents(stack).observation()));
        lines.add(Component.literal("Place on an empty stove or solid counter."));
        lines.add(Component.literal("Lift: crouch + use, both hands empty."));
    }
}

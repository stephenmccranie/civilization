package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

public final class CutItem extends BlockItem {
    public CutItem(Block block, Properties properties) { super(block, properties); }
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) { return CutPlacement.place(context); }
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
        if(!level.isClientSide && CuttingContent.units(stack)==2 && SlabIntegration.slab(CuttingContent.material(stack).getBlock())!=null
                && entity instanceof net.minecraft.world.entity.player.Player player && player.getInventory().getItem(slot)==stack)
            player.getInventory().setItem(slot,CuttingContent.stack(CuttingContent.material(stack),2,stack.getCount()));
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable(CuttingContent.units(stack) == 3 ? "item.civilization.eighth" : CuttingContent.units(stack) == 1 ? "item.civilization.quarter" : "item.civilization.half", CuttingContent.material(stack).getBlock().getName());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.civilization.cut_placement"));
    }
}

package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CoalPickItem extends Item {
    public static final int CONTACT=12,DURATION=32;
    public CoalPickItem(){super(new Properties().durability(768));}
    public static CompoundTag state(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void save(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    @Override public boolean canAttackBlock(BlockState b,Level l,BlockPos pos,Player p){return false;}
    @Override public boolean onLeftClickEntity(ItemStack s,Player p,Entity target){return true;}
    @Override public boolean shouldCauseReequipAnimation(ItemStack a,ItemStack b,boolean slot){return slot||!a.is(b.getItem());}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("Click to strike huge coal veins; aim through the swing."));lines.add(Component.literal("Creative prototype · loose coal needs the Loading Shovel."));}
}

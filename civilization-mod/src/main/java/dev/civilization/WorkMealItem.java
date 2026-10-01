package dev.civilization;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class WorkMealItem extends Item {
    public WorkMealItem(Properties p){super(p);}
    public static double quality(ItemStack s){double q=s.getOrDefault(PrototypeStoveContent.QUALITY.get(),0.0);return Double.isFinite(q)?Math.clamp(q,0,1):0;}
    public static double calories(ItemStack s){double kcal=s.getOrDefault(PrototypeStoveContent.CALORIES.get(),0.0);return Double.isFinite(kcal)?Math.max(0,kcal):0;}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){double q=quality(s);var result=super.finishUsingItem(s,l,e);if(e instanceof Player p&&CalorieFoodData.active(p)){CalorieFoodData.of(p).workMeal(q);p.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT,"Work meal: %.1f%% lower work calorie costs for 30 minutes",100*StoveCooking.discount(q))),true);}return result;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        lines.add(Component.literal("Cooking quality: "+Math.round(quality(s)*100)+"%"));
        lines.add(Component.literal(String.format(java.util.Locale.ROOT,"Active work uses %.1f%% fewer calories",100*StoveCooking.discount(quality(s)))));
        lines.add(Component.literal("30 minutes online · replaces your previous meal benefit"));
    }
}

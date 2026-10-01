package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

/** Focused native inventory skin/interaction check in the disposable hidden client. */
final class InventoryVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){
            mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.hideGui=false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{
                var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();
                var l=server.overworld();l.setBlockAndUpdate(new BlockPos(0,100,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                p.teleportTo(l,.5,101,.5,java.util.Set.of(),0,0);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.OAK_LOG,3));
                p.getData(Accessories.INVENTORY).setStackInSlot(0, FrontierContent.GEIGER_COUNTER.toStack());
                p.getInventory().setItem(20, FrontierContent.RAW_URANIUM.toStack());
                p.getInventory().setItem(21, FrontierContent.URANIUM_ORE.toStack());
                int[] grades={45,45,60,75,90,100,115};
                for(int i=0;i<grades.length;i++){var item=new ItemStack(Items.IRON_SWORD);EquipmentGrade.apply(item,grades[i],1001L+i*991L);p.getInventory().setItem(11+i,item);}
                var armor=new ItemStack(Items.IRON_CHESTPLATE);EquipmentGrade.apply(armor,55,9017L);armor.setDamageValue(armor.getMaxDamage()/3);p.getInventory().setItem(18,armor);
                var exceptional=new ItemStack(Items.GOLDEN_CHESTPLATE);EquipmentGrade.apply(exceptional,105,5031L);EquipmentGrade.updateTrim(exceptional,l.registryAccess());p.getInventory().setItem(19,exceptional);
                var usedAverage=new ItemStack(Items.IRON_PICKAXE);EquipmentGrade.apply(usedAverage,75,5033L);usedAverage.setDamageValue(usedAverage.getMaxDamage()/3);p.getInventory().setItem(22,usedAverage);
                var usedExceptional=new ItemStack(Items.IRON_PICKAXE);EquipmentGrade.apply(usedExceptional,115,5034L);usedExceptional.setDamageValue((usedExceptional.getMaxDamage()-usedExceptional.getItem().components().getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE,1))/2);p.getInventory().setItem(23,usedExceptional);
                var helmet=new ItemStack(Items.GOLDEN_HELMET);EquipmentGrade.apply(helmet,110,5032L);EquipmentGrade.updateTrim(helmet,l.registryAccess());p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new ItemStack(Items.TORCH,16));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200));
                p.inventoryMenu.broadcastChanges();
            });
        }
        if(ticks==130){mc.setScreen(new InventoryScreen(mc.player));var s=(InventoryScreen)mc.screen;if(s.getRecipeBookComponent().isVisible())toggle(mc);}
        if(ticks==150){
            if(EquipmentGrade.value(mc.player.getInventory().getItem(11))!=45||EquipmentGrade.value(mc.player.getInventory().getItem(17))!=115||EquipmentGrade.wearSeed(mc.player.getInventory().getItem(12))!=1992L
                    ||!mc.player.getInventory().getItem(19).has(net.minecraft.core.component.DataComponents.TRIM)
                    ||!mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).has(net.minecraft.core.component.DataComponents.TRIM)
                    ||!mc.player.inventoryMenu.getSlot(46).getItem().is(FrontierContent.GEIGER_COUNTER.get()))
                throw new IllegalStateException("Inventory fixture did not synchronize: grades="
                        +EquipmentGrade.value(mc.player.getInventory().getItem(11))+","+EquipmentGrade.value(mc.player.getInventory().getItem(17))
                        +" seed="+EquipmentGrade.wearSeed(mc.player.getInventory().getItem(12))
                        +" trim="+mc.player.getInventory().getItem(19).has(net.minecraft.core.component.DataComponents.TRIM)
                        +" helmet="+mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).has(net.minecraft.core.component.DataComponents.TRIM)
                        +" accessory="+mc.player.inventoryMenu.getSlot(46).getItem());
            shot(mc,"grades");shot(mc,"closed");click(mc,9);
        }
        if(ticks==158)click(mc,1);
        if(ticks==170){
            if(!mc.player.inventoryMenu.getSlot(0).getItem().is(Items.OAK_PLANKS))throw new IllegalStateException("Native inventory crafting did not update");
            shot(mc,"crafting");click(mc,0);
        }
        if(ticks==178)click(mc,10);
        if(ticks==190){
            if(!mc.player.inventoryMenu.getSlot(10).getItem().is(Items.OAK_PLANKS)||mc.player.inventoryMenu.getSlot(1).getItem().getCount()!=2||!mc.player.inventoryMenu.getCarried().isEmpty())throw new IllegalStateException("Native crafting custody changed");
            toggle(mc);
        }
        if(ticks==205){if(!((InventoryScreen)mc.screen).getRecipeBookComponent().isVisible())throw new IllegalStateException("Recipe book toggle failed");shot(mc,"recipes");}
        if(ticks==210)server.execute(()->server.getPlayerList().getPlayers().getFirst().setGameMode(net.minecraft.world.level.GameType.CREATIVE));
        if(ticks==235){
            if(!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative))
                throw new IllegalStateException("Creative inventory did not open");
            try {
                var select=creative.getClass().getDeclaredMethod("selectTab",net.minecraft.world.item.CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(creative,net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("inventory")));
            } catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot select creative inventory tab",ex);}
            for(int i=46;i<49;i++)if(creative.getMenu().getSlot(i).x!=-2000||creative.getMenu().getSlot(i).y!=-2000)
                throw new IllegalStateException("Accessory wrapper overlaps creative inventory: "+i);
        }
        if(ticks==236)shot(mc,"creative-inventory");
        if(ticks>240){com.mojang.logging.LogUtils.getLogger().info("PLAYER INVENTORY VISUAL VERIFIED");mc.stop();}
    }
    private static void toggle(Minecraft mc){var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.ImageButton).map(c->(net.minecraft.client.gui.components.ImageButton)c).findFirst().orElseThrow();mc.screen.mouseClicked(b.getX()+5,b.getY()+5,0);mc.screen.mouseReleased(b.getX()+5,b.getY()+5,0);}
    private static void click(Minecraft mc,int index){var screen=(InventoryScreen)mc.screen;var slot=mc.player.inventoryMenu.getSlot(index);double x=screen.getGuiLeft()+slot.x+8,y=screen.getGuiTop()+slot.y+8;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"inventory-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

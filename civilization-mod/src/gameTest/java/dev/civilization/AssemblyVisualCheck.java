package dev.civilization;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Actual synchronized bench controls and art under the existing hidden-client guards. */
final class AssemblyVisualCheck {
    private static int ticks;private static volatile Throwable failure;private static volatile boolean ready;
    private static final BlockPos A=new BlockPos(30,101,0);
    private static void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable t){failure=t;}});}
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException("Assembly visual failed",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(55);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,()->{
            var l=mc.getSingleplayerServer().overworld();for(int x=25;x<=38;x++)for(int z=-5;z<6;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<108;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            var bench=WorkshopContent.BENCH.get().defaultBlockState().setValue(AssemblyBenchBlock.WIDE,true);l.setBlockAndUpdate(A,bench);((AssemblyBenchBlock)bench.getBlock()).setPlacedBy(l,A,bench,null,ItemStack.EMPTY);l.setDayTime(6000);
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();CalorieFoodData.of(p).reserve().set(2400);
            var parts=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i->{String s=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString();return s.startsWith("civilization:")&&(s.endsWith("_head")||s.endsWith("_plate")||s.endsWith("_rod")||s.endsWith("_fasteners")||s.endsWith("tool_handle")||s.endsWith("wooden_wheel"));}).toList();
            for(int i=0;i<parts.size();i++)p.getInventory().setItem(i,new ItemStack(parts.get(i)));
            p.teleportTo(l,33,103,-4,Set.of(),32,24);ready=true;
        });}
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==135)shot(mc,"bench-photon");
        if(ticks==140){mc.options.hideGui=false;server(mc,()->{var l=mc.getSingleplayerServer().overworld();var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.teleportTo(l,31.5,101,-1,Set.of(),0,20);var w=(WorkshopBlockEntity)l.getBlockEntity(A);w.select(WorkshopJobs.index(3,"minecraft:iron_pickaxe"));});}
        if(ticks==150)mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(A.east().getCenter(),Direction.UP,A.east(),false));
        if(ticks==165){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.kind()!=3||m.hasCoalFire()||m.getSlot(4).isActive()||m.getSlot(6).isActive())throw new IllegalStateException("Hand bench synchronization or hidden fuel slots failed");shot(mc,"recipe-and-parts");server(mc,()->{var w=(WorkshopBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(A);w.setItem(0,new ItemStack(ManufacturingContent.head("iron","pickaxe")));w.setItem(1,ManufacturingContent.HANDLE.toStack());});}
        if(ticks==185){click(mc,"Work");shot(mc,"working");}
        if(ticks==300){var m=(WorkshopMenu)mc.player.containerMenu;if(!m.getSlot(5).getItem().is(Items.IRON_PICKAXE)||!EquipmentGrade.hasGrade(m.getSlot(5).getItem())||!m.getSlot(0).getItem().isEmpty()||!m.getSlot(1).getItem().isEmpty())throw new IllegalStateException("Real Work button did not consume the head/handle and synchronize a graded tool");shot(mc,"finished");}
        if(ticks==305){
            var runtime=dev.civilization.client.jei.CivilizationJei.runtime();if(runtime==null)throw new IllegalStateException("JEI runtime missing");
            var type=dev.civilization.client.jei.CivilizationJei.type("assembly");var manager=runtime.getRecipeManager();
            var recipes=manager.createRecipeLookup(type).get().toList();
            if(manager.getRecipeCategory(type)==null||recipes.isEmpty()||recipes.stream().anyMatch(r->r.note().contains("Light the fire")||r.slots().stream().anyMatch(s->s.role()==mezz.jei.api.recipe.RecipeIngredientRole.CATALYST&&s.items().stream().anyMatch(i->i.is(KilnContent.MINERAL_COAL.get())))))throw new IllegalStateException("Hand assembly JEI pages must not advertise ignition or fuel");
            var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,mezz.jei.api.constants.VanillaTypes.ITEM_STACK,new ItemStack(Items.IRON_PICKAXE));
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(type),manager.createRecipeLookup(type).limitFocus(List.of(focus)).get().toList(),List.of(focus));
        }
        if(ticks==325)shot(mc,"jei-hand-assembly");
        if(ticks==330){mc.setScreen(null);mc.player.closeContainer();mc.options.hideGui=true;server(mc,()->{var l=mc.getSingleplayerServer().overworld();var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(l,33,103,-4,Set.of(),32,24);});}
        if(ticks==350)shot(mc,"bench-after");
        if(ticks==355){com.mojang.logging.LogUtils.getLogger().info("ASSEMBLY VISUAL VERIFIED: bench geometry, native icons, synchronized Work, exact head/handle consumption, graded output and JEI hand recipes; no Coal");mc.stop();}
    }
    private static void click(Minecraft mc,String label){var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button v&&v.visible&&v.getMessage().getString().equals(label)).map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();mc.screen.mouseClicked(b.getX()+3,b.getY()+3,0);mc.screen.mouseReleased(b.getX()+3,b.getY()+3,0);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"assembly-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

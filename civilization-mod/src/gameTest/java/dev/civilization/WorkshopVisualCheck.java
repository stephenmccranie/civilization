package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

final class WorkshopVisualCheck {
    private static int ticks;private static volatile boolean ready;private static volatile Throwable failure;
    private static final java.util.Set<String> dragTops=new java.util.LinkedHashSet<>();private static int dragSelection;
    private static BlockPos at(int kind){return new BlockPos(30+kind*7,101,0);}
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Workshop visual failed",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(55);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{try{
                var l=server.overworld();for(int x=24;x<=50;x++)for(int z=-5;z<7;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<110;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(int kind=0;kind<3;kind++){
                    var block=switch(kind){case 0->WorkshopContent.TANNERY.get();case 1->WorkshopContent.TEXTILE.get();default->WorkshopContent.SMITHY.get();};l.setBlockAndUpdate(at(kind),block.defaultBlockState());
                    MachineFixture.build(l,at(kind));
                    if(MachineFixture.status(l,at(kind))!=MachineStructure.COMPLETE)throw new IllegalStateException("Incomplete workshop fixture");
                    var w=(WorkshopBlockEntity)l.getBlockEntity(at(kind));if(WorkshopBlockEntity.fired(kind)){CoalFireFixture.light(w);w.setItem(4,KilnContent.MINERAL_COAL.toStack(8));}
                    if(kind==0){w.setItem(0,WorkshopContent.HIDE.toStack(8));w.setItem(1,WorkshopContent.TANBARK.toStack(8));}
                    if(kind==1)w.setItem(0,new ItemStack(Items.WHITE_WOOL,8));
                    if(kind==2){w.select(WorkshopJobs.index(2,"civilization:iron_pickaxe_head"));w.setItem(0,new ItemStack(Items.IRON_INGOT,8));}
                }
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,44,109,-14,java.util.Set.of(),24,24);l.setDayTime(6000);ready=true;
            }catch(Throwable t){failure=t;}});
        }
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==104)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),30.5,103,-5,java.util.Set.of(),0,12));
        if(ticks==113)shot(mc,"tannery-front");
        if(ticks==114)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),37.5,103,-5,java.util.Set.of(),0,12));
        if(ticks==123)shot(mc,"textile-front");
        if(ticks==125)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),48.5,103.5,-5,java.util.Set.of(),30,15);});
        if(ticks==143)shot(mc,"smithy-working");
        if(ticks==145)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),44,109,-14,java.util.Set.of(),24,24);});
        if(ticks==150)shot(mc,"structures");
        for(int k=0;k<3;k++){final int kind=k;
            if(ticks==155+k*40){mc.options.hideGui=false;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),at(kind).getX()+.5,101,-2,java.util.Set.of(),0,0);p.openMenu((WorkshopBlockEntity)server.overworld().getBlockEntity(at(kind)));});}
            if(ticks==185+k*40){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.kind()!=kind)throw new IllegalStateException("Menu kind did not synchronize: "+kind);shot(mc,"menu-"+kind);}
        }
        if(ticks==187)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            if(!((WorkshopBlockEntity)server.overworld().getBlockEntity(at(0))).pour(p,net.minecraft.world.InteractionHand.MAIN_HAND))failure=new IllegalStateException("The Tannery pit refused its water");});
        if(ticks==193){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.pit()!=Tanning.PIT)throw new IllegalStateException("The steeping pit did not synchronize");shot(mc,"menu-0-steeping");}
        if(ticks==270)server.execute(()->{var l=server.overworld();l.setBlockAndUpdate(at(2),WorkshopContent.BENCH.get().defaultBlockState());var w=(WorkshopBlockEntity)l.getBlockEntity(at(2));w.select(WorkshopJobs.index(3,"minecraft:iron_pickaxe"));var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.openMenu(w);});
        if(ticks==290){click(mc,"Tools");click(mc,"Iron");}
        if(ticks==300){
            // Six rows fit, so read the whole filtered list a page at a time, then return to its top.
            var seen=new java.util.LinkedHashSet<String>();
            for(int key:new int[]{org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN,org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP}){for(var row:recipeRows(mc))if(row.visible)seen.add(row.getMessage().getString());mc.screen.keyPressed(key,0,0);}
            if(seen.size()!=8)throw new IllegalStateException("Tools / Iron must have eight results, found "+seen);
            shot(mc,"recipe");
        }

        if(ticks==302){
            // A real click on a recipe ghost may never turn it into inventory.
            double left=(mc.getWindow().getGuiScaledWidth()-176)/2.0,top=(mc.getWindow().getGuiScaledHeight()-214)/2.0;
            mc.screen.mouseClicked(left+24,top+50,0);mc.screen.mouseReleased(left+24,top+50,0);
            mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,-1);
        }
        if(ticks==312){
            click(mc,"All");click(mc,"All materials");
            if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.selection()!=-1||!m.getCarried().isEmpty()||!m.getSlot(0).getItem().isEmpty())throw new IllegalStateException("Automatic selection or ghost custody failed");
            var field=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();field.setFocused(true);field.setValue("Pickaxe");
            mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,0);
            if(!(mc.screen instanceof dev.civilization.client.WorkshopScreen))throw new IllegalStateException("Recipe search closed the menu");
        }
        if(ticks==315){var button=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button b&&b.visible&&b.getMessage().getString().equals("Diamond Pickaxe")).map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();mc.screen.mouseClicked(button.getX()+4,button.getY()+4,0);mc.screen.mouseReleased(button.getX()+4,button.getY()+4,0);}
        if(ticks==330){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.selection()!=WorkshopJobs.index(3,"minecraft:diamond_pickaxe"))throw new IllegalStateException("Filtered recipe selection did not synchronize");shot(mc,"search");}
        if(ticks==332){click(mc,"Armor");click(mc,"Netherite");var field=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();field.setValue("");}
        if(ticks==337){click(mc,"Netherite Helmet (upgrade)");shot(mc,"upgrade");}
        if(ticks==338){
            click(mc,"All materials");click(mc,"Machines");
            int bottom=(mc.getWindow().getGuiScaledHeight()+214)/2;
            for(var c:ManufacturingJobs.Category.values()){var tab=mc.screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.visible&&b.getMessage().getString().equals(c.label)).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow(()->new IllegalStateException("Missing tab: "+c.label));
                if(tab.getBottom()>bottom)throw new IllegalStateException("Tab "+c.label+" runs below the cabinet");}
            if(recipeRows(mc).stream().noneMatch(r->r.visible&&r.getMessage().getString().equals("Hot-Bulb Engine")))throw new IllegalStateException("Machines tab lacks the Hot-Bulb Engine");
        }
        if(ticks==339)shot(mc,"tabs");
        if(ticks==340){
            click(mc,"All");click(mc,"All materials");mc.screen.mouseScrolled(100,150,0,-1);
            shot(mc,"scroll");
        }
        // Track clicks jump to either end, where the wheel has nowhere further to go.
        if(ticks==342||ticks==345){
            var rows=recipeRows(mc);int x=rows.getFirst().getRight()+4,y=ticks==342?rows.getLast().getBottom()-1:rows.getFirst().getY();String before=topRecipe(mc);
            mc.screen.mouseClicked(x,y,0);mc.screen.mouseReleased(x,y,0);String after=topRecipe(mc);mc.screen.mouseScrolled(x,y,0,ticks==342?-1:1);
            if(after.equals(before)||!topRecipe(mc).equals(after))throw new IllegalStateException("A track click must jump to the end of the list: "+before+" -> "+after);
        }
        if(ticks==344)shot(mc,"scroll-track");
        // A thumb drag held across ticks: the pointer moves six pixels a tick and the list follows every step.
        if(ticks>=346&&ticks<=357){
            var rows=recipeRows(mc);int x=rows.getFirst().getRight()+4,y=rows.getFirst().getY()+2+6*(ticks-346);
            if(ticks==346){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||!mc.screen.mouseClicked(x,y,0))throw new IllegalStateException("The scrollbar thumb ignored a press");dragSelection=m.selection();}
            else if(ticks<357)mc.screen.mouseDragged(x,y,0,0,6);
            else if(!mc.screen.mouseReleased(x,y,0)||dragTops.size()!=11||!(mc.player.containerMenu instanceof WorkshopMenu m)||m.selection()!=dragSelection)throw new IllegalStateException("A thumb drag must move the list every step without choosing a recipe: "+dragTops);
            dragTops.add(topRecipe(mc));
        }
        if(ticks==352)shot(mc,"scroll-drag");
        if(ticks==363){click(mc,"Recipes");shot(mc,"collapsed");}
        if(ticks==366){click(mc,"Recipes");mc.options.guiScale().set(4);mc.resizeDisplay();}
        if(ticks==370)shot(mc,"scale-4");
        if(ticks==375){mc.player.closeContainer();server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();server.overworld().setBlockAndUpdate(at(2),WorkshopContent.SMITHY.get().defaultBlockState());MachineFixture.build(server.overworld(),at(2));p.teleportTo(server.overworld(),49,104,6,java.util.Set.of(),140,20);});}
        if(ticks==390)mc.options.hideGui=true;
        if(ticks==395)shot(mc,"smithy-rear");
        if(ticks==396)mc.options.hideGui=false;
        if(ticks==397)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),44.5,102,-4,java.util.Set.of(),0,25);ModeledMachine.removeSection(ModeledMachine.of(server.overworld().getBlockEntity(at(2))),3,false);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(WorkshopContent.SMITHY.get()));});
        if(ticks==409)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),48.5,103.5,-5,java.util.Set.of(),30,15);});
        if(ticks==415)shot(mc,"smithy-guide");
        // The Tannery still steeps the batch poured at tick 187: its water and hides show from the front left.
        if(ticks==416){mc.options.hideGui=true;server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),34.5,104,-3.5,java.util.Set.of(),37,32));}
        if(ticks==423){shot(mc,"tannery-steeping");mc.options.hideGui=false;}
        if(ticks==425){mc.options.guiScale().set(3);mc.resizeDisplay();server.execute(()->{
            var l=server.overworld();var w=(WorkshopBlockEntity)l.getBlockEntity(at(1));w.clearContent();w.fire.extinguish();w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,37.5,101,-2,java.util.Set.of(),0,0);p.openMenu(w);
        });}
        if(ticks==445)shot(mc,"ignition-rest");
        if(ticks==446)click(mc,"Strike to light");
        if(ticks==447)shot(mc,"ignition-strike");
        // Exercise the real button first; a paced seeded menu strike makes the final art capture repeatable.
        if(ticks==470)server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var w=(WorkshopBlockEntity)server.overworld().getBlockEntity(at(1));
            if(!w.fire.lit()){
                MachineFixture.spark(server.overworld(),true);
                p.containerMenu.clickMenuButton(p,CoalFire.BUTTON);
            }
        });
        if(ticks==485){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||(m.fireState()&1)==0)throw new IllegalStateException("Seeded menu ignition failed to synchronize");shot(mc,"ignition-lit");}
        // The same batch three seconds on: its hides have drifted, and the client knows every hide in the pit; then the bark heap close up.
        if(ticks==487){mc.player.closeContainer();mc.options.hideGui=true;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),34.5,104,-3.5,java.util.Set.of(),37,32);});}
        if(ticks==497){if(ModeledMachine.of(mc.level.getBlockEntity(at(0))).afloat()!=Tanning.PIT)throw new IllegalStateException("The client does not see the steeping hides");shot(mc,"tannery-drift");}
        if(ticks==498)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),34.2,103.2,-1.2,java.util.Set.of(),47,24));
        if(ticks==508)shot(mc,"tannery-heap");
        // The steeping pit again without a shader pack, its water as vanilla draws it.
        if(ticks==509){WheelbarrowVisualCheck.shaders(false);server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),34.5,104,-3.5,java.util.Set.of(),37,32));}
        if(ticks==550){shot(mc,"tannery-plain");com.mojang.logging.LogUtils.getLogger().info("WORKSHOP VISUAL VERIFIED, manual ignition synchronized");mc.stop();}
    }
    private static void click(Minecraft mc,String name){
        var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button button&&button.visible&&button.getMessage().getString().equals(name)).map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow(()->new IllegalStateException("Missing control: "+name));
        mc.screen.mouseClicked(b.getX()+4,b.getY()+4,0);mc.screen.mouseReleased(b.getX()+4,b.getY()+4,0);
    }
    /** The recipe drawer's rows, top to bottom: the wide buttons left of the cabinet. Its scrollbar runs four pixels right of them. */
    private static java.util.List<net.minecraft.client.gui.components.Button> recipeRows(Minecraft mc){int left=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.screen).getGuiLeft();return mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button b&&b.getWidth()>80&&b.getX()<left).map(c->(net.minecraft.client.gui.components.Button)c).toList();}
    private static String topRecipe(Minecraft mc){return recipeRows(mc).getFirst().getMessage().getString();}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"workshop-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

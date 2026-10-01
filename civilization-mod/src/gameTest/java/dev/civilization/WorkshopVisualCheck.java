package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

final class WorkshopVisualCheck {
    private static int ticks;private static volatile boolean ready;private static volatile Throwable failure;
    private static BlockPos at(int kind){return new BlockPos(30+kind*7,101,0);}
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Workshop visual failed",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(55);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{try{
                var l=server.overworld();for(int x=24;x<=50;x++)for(int z=-5;z<7;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<110;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(int kind=0;kind<3;kind++){
                    var block=switch(kind){case 0->WorkshopContent.TANNERY.get();case 1->WorkshopContent.TEXTILE.get();default->WorkshopContent.SMITHY.get();};l.setBlockAndUpdate(at(kind),block.defaultBlockState());
                    for(var part:WorkshopStructure.parts(kind))MachineStructure.placePart(l,at(kind),Direction.NORTH,part);
                    if(MachineStructure.check(l,at(kind),Direction.NORTH).status()!=1)throw new IllegalStateException("Incomplete workshop fixture");
                    var w=(WorkshopBlockEntity)l.getBlockEntity(at(kind));CoalFireFixture.light(w);w.setItem(4,KilnContent.MINERAL_COAL.toStack(8));
                    if(kind==0)w.setItem(0,WorkshopContent.HIDE.toStack(8));
                    if(kind==1)w.setItem(0,new ItemStack(Items.WHITE_WOOL,8));
                    if(kind==2){var pick=new ItemStack(Items.IRON_PICKAXE);pick.setDamageValue(220);w.setItem(0,pick);w.setItem(1,new ItemStack(Items.IRON_INGOT,8));w.setItem(2,new ItemStack(Items.LEATHER,8));}
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
        if(ticks==270)server.execute(()->{var l=server.overworld();var w=(WorkshopBlockEntity)l.getBlockEntity(at(2));w.clearContent();w.select(1);var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.openMenu(w);});
        if(ticks==290){click(mc,"Tools");click(mc,"Iron");}
        if(ticks==300){
            long count=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button b&&b.visible&&b.getWidth()>80).count();
            if(count!=6)throw new IllegalStateException("Tools / Iron must have six results, found "+count);
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
        if(ticks==330){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.selection()!=19)throw new IllegalStateException("Filtered recipe selection did not synchronize");shot(mc,"search");}
        if(ticks==332){click(mc,"Armor");click(mc,"Netherite");var field=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();field.setValue("");}
        if(ticks==337){click(mc,"Netherite Helmet (upgrade)");shot(mc,"upgrade");}
        if(ticks==340){
            click(mc,"All");click(mc,"All materials");mc.screen.mouseScrolled(100,150,0,-1);
            shot(mc,"scroll");
        }
        if(ticks==343){click(mc,"Recipes");shot(mc,"collapsed");}
        if(ticks==346){click(mc,"Recipes");mc.options.guiScale().set(4);mc.resizeDisplay();}
        if(ticks==350)shot(mc,"scale-4");
        if(ticks==355){mc.player.closeContainer();server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(server.overworld(),49,104,6,java.util.Set.of(),140,20);});}
        if(ticks==370)mc.options.hideGui=true;
        if(ticks==375)shot(mc,"smithy-rear");
        if(ticks==376)mc.options.hideGui=false;
        if(ticks==377)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),44.5,102,-4,java.util.Set.of(),0,25);server.overworld().removeBlock(at(2).east(WorkshopStructure.SMITHY_ANVIL_X),false);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(WorkshopContent.SMITHY.get()));});
        if(ticks==389)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),48.5,103.5,-5,java.util.Set.of(),30,15);});
        if(ticks==395)shot(mc,"smithy-guide");
        if(ticks==405){mc.options.guiScale().set(3);mc.resizeDisplay();server.execute(()->{
            var l=server.overworld();var w=(WorkshopBlockEntity)l.getBlockEntity(at(0));w.clearContent();w.fire.extinguish();w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,30.5,101,-2,java.util.Set.of(),0,0);p.openMenu(w);
        });}
        if(ticks==425)shot(mc,"ignition-rest");
        if(ticks==426)click(mc,"Strike to light");
        if(ticks==427)shot(mc,"ignition-strike");
        // Exercise the real button first; a paced seeded menu strike makes the final art capture repeatable.
        if(ticks==450)server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var w=(WorkshopBlockEntity)server.overworld().getBlockEntity(at(0));
            if(!w.fire.lit()){
                for(long seed=0;;seed++){var r=net.minecraft.util.RandomSource.create(seed);r.nextFloat();if(r.nextInt(3)==0){server.overworld().random.setSeed(seed);break;}}
                p.containerMenu.clickMenuButton(p,CoalFire.BUTTON);
            }
        });
        if(ticks==465){if(!(mc.player.containerMenu instanceof WorkshopMenu m)||(m.fireState()&1)==0)throw new IllegalStateException("Seeded menu ignition failed to synchronize");shot(mc,"ignition-lit");com.mojang.logging.LogUtils.getLogger().info("WORKSHOP VISUAL VERIFIED, manual ignition synchronized");mc.stop();}
    }
    private static void click(Minecraft mc,String name){
        var b=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button button&&button.visible&&button.getMessage().getString().equals(name)).map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow(()->new IllegalStateException("Missing control: "+name));
        mc.screen.mouseClicked(b.getX()+4,b.getY()+4,0);mc.screen.mouseReleased(b.getX()+4,b.getY()+4,0);
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"workshop-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}

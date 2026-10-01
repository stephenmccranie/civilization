package dev.civilization.client;
import java.util.*;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class KilnScreen extends MachineScreen<KilnMenu> {
    public KilnScreen(KilnMenu m,Inventory i,Component title){super(m,i,title);imageHeight=214;inventoryLabelY=120;}
    @Override protected List<RecipeEntry> recipes(){
        var list=new ArrayList<RecipeEntry>();for(int i=0;i<menu.recipes.size();i++){
            var recipe=menu.recipes.get(i).value();var output=recipe.getResultItem(minecraft.level.registryAccess());
            var input=recipe.getIngredients().getFirst().getItems()[0];
            list.add(new RecipeEntry(i,output.getHoverName().copy().append(" — ").append(input.getHoverName()),output,"Components",net.minecraft.world.item.Items.AIR));
        }return list;
    }
    @Override protected int chosen(){return menu.selection();}
    @Override protected void choose(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private net.minecraft.world.item.crafting.AbstractCookingRecipe recipe(){
        int selected=menu.selection();
        if(selected>=0&&selected<menu.recipes.size())return menu.recipes.get(selected).value();
        var actual=menu.getSlot(0).hasItem()?menu.getSlot(0).getItem():menu.getSlot(4).getItem();
        if(!actual.isEmpty())return menu.recipes.stream().filter(r->r.value().getIngredients().getFirst().test(actual)).map(r->r.value()).findFirst().orElse(null);
        return null;
    }
    @Override protected void drawMachine(GuiGraphics g,float dt,int mx,int my){
        var recipe=recipe();
        g.drawString(font,"Materials",leftPos+17,topPos+29,MachineUi.INK,false);g.drawString(font,"Output",leftPos+130,topPos+29,MachineUi.INK,false);
        if(recipe!=null){
            for(int i:MachineInventory.KILN_INPUT)if(!menu.getSlot(i).hasItem())MachineUi.ghost(g,recipe.getIngredients().getFirst().getItems()[0],leftPos+menu.getSlot(i).x,topPos+43);
            for(int i:MachineInventory.KILN_OUTPUT)if(!menu.getSlot(i).hasItem())MachineUi.ghost(g,recipe.getResultItem(minecraft.level.registryAccess()),leftPos+menu.getSlot(i).x,topPos+43);
        }
        MachineUi.progress(g,leftPos+73,topPos+49,43,menu.getBurnProgress());
        g.drawString(font,"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
        for(int index:new int[]{1,3})if(menu.getSlot(index).getItem().isEmpty())MachineUi.ghost(g,KilnContent.MINERAL_COAL.toStack(),leftPos+menu.getSlot(index).x,topPos+88);
        MachineUi.fire(g,leftPos+66,topPos+85,44,23,menu.isLit());
        boolean incomplete=menu.requiresStructure()&&menu.structureStatus()!=1;
        var state=!incomplete&&!menu.isLit()&&menu.operatingStatus()!=7?Component.literal("Add Coal and strike to light"):Component.translatable(incomplete?"gui.civilization.structure_"+menu.structureStatus():"gui.civilization.operating_"+menu.operatingStatus());
        g.drawString(font,font.plainSubstrByWidth(state.getString(),158),leftPos+9,topPos+64,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);
        for(int index:new int[]{0,1,2,3,4,5}){var slot=menu.getSlot(index);if(!slot.hasItem()&&isHovering(slot.x,slot.y,16,16,x,y)){
            var r=recipe();ItemStack ghost=index==1||index==3?KilnContent.MINERAL_COAL.toStack():r==null?ItemStack.EMPTY:(index==0||index==4)?r.getIngredients().getFirst().getItems()[0]:r.getResultItem(minecraft.level.registryAccess());
            g.renderTooltip(font,ghost.isEmpty()?Component.literal((index==0||index==4)?"Materials — choose a recipe for a guide":"Output"):ghost.getHoverName(),x,y);
        }}
    }
}

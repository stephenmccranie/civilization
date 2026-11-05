package dev.civilization.client;
import java.util.*;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;

public final class KilnScreen extends MachineScreen<KilnMenu> {
    public KilnScreen(KilnMenu m,Inventory i,Component title){super(m,i,title);imageHeight=214;inventoryLabelY=120;}
    @Override protected List<RecipeEntry> recipes(){
        var registries=minecraft.level.registryAccess();var list=new ArrayList<RecipeEntry>();
        for(int i=0;i<menu.outputs.size();i++){
            var made=KilnBlockEntity.candidates(menu.recipes,menu.outputs.get(i),registries);
            var output=made.getFirst().value().getResultItem(registries);
            // An output whose every recipe keeps its material is a heat.
            boolean heat=made.stream().allMatch(r->output.is(r.value().getIngredients().getFirst().getItems()[0].getItem()));
            var name=heat?Component.literal("Heat ").append(output.getHoverName()):output.getHoverName().copy();
            list.add(new RecipeEntry(i,name,output,"Components",net.minecraft.world.item.Items.AIR,made.stream().map(r->input(r.value())).toList()));
        }return list;
    }
    private static Component input(AbstractCookingRecipe recipe){
        return Component.literal(String.join(" or ",Arrays.stream(recipe.getIngredients().getFirst().getItems()).map(s->s.getHoverName().getString()).distinct().toList()));
    }
    @Override protected int chosen(){return menu.selection();}
    @Override protected void choose(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    /** What the ghosts show: the recipe the loaded material runs, else the selected output's recipes, the first drawn and all named. */
    private List<AbstractCookingRecipe> guide(){
        var actual=menu.getSlot(0).hasItem()?menu.getSlot(0).getItem():menu.getSlot(4).getItem();
        var candidates=KilnBlockEntity.candidates(menu.recipes,menu.selectedOutput(),minecraft.level.registryAccess());
        var running=candidates.stream().filter(r->r.value().getIngredients().getFirst().test(actual)).findFirst();
        if(running.isPresent())return List.of(running.get().value());
        return menu.selectedOutput()==null?List.of():candidates.stream().map(r->(AbstractCookingRecipe)r.value()).toList();
    }
    @Override protected void drawMachine(GuiGraphics g,float dt,int mx,int my){
        var guide=guide();
        g.drawString(font,"Materials",leftPos+17,topPos+29,MachineUi.INK,false);g.drawString(font,"Output",leftPos+130,topPos+29,MachineUi.INK,false);
        if(!guide.isEmpty()){
            var recipe=guide.getFirst();
            for(int i:MachineInventory.KILN_INPUT)if(!menu.getSlot(i).hasItem())MachineUi.ghost(g,recipe.getIngredients().getFirst().getItems()[0],leftPos+menu.getSlot(i).x,topPos+43);
            for(int i:MachineInventory.KILN_OUTPUT)if(!menu.getSlot(i).hasItem())MachineUi.ghost(g,recipe.getResultItem(minecraft.level.registryAccess()),leftPos+menu.getSlot(i).x,topPos+43);
        }
        MachineUi.progress(g,leftPos+73,topPos+49,43,menu.getBurnProgress());
        g.drawString(font,"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
        for(int index:new int[]{1,3})if(menu.getSlot(index).getItem().isEmpty())MachineUi.ghost(g,KilnContent.MINERAL_COAL.toStack(),leftPos+menu.getSlot(index).x,topPos+88);
        MachineUi.fire(g,leftPos+66,topPos+85,44,23,menu.isLit());
        boolean incomplete=menu.requiresStructure()&&menu.structureStatus()!=1;
        var state=!incomplete&&!menu.isLit()&&!menu.hot()&&menu.operatingStatus()!=7?Component.literal("Add Coal and strike to light"):Component.translatable(incomplete?"gui.civilization.structure_"+menu.structureStatus():"gui.civilization.operating_"+menu.operatingStatus());
        g.drawString(font,font.plainSubstrByWidth(state.getString(),158),leftPos+9,topPos+64,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);
        for(int index:new int[]{0,1,2,3,4,5}){var slot=menu.getSlot(index);if(!slot.hasItem()&&isHovering(slot.x,slot.y,16,16,x,y)){
            boolean input=index==0||index==4;var guide=guide();
            List<Component> lines=index==1||index==3?List.of(KilnContent.MINERAL_COAL.toStack().getHoverName())
                :guide.isEmpty()?List.of(Component.literal(input?"Materials — choose an output for a guide":"Output"))
                :input?guide.stream().map(KilnScreen::input).toList():List.of(guide.getFirst().getResultItem(minecraft.level.registryAccess()).getHoverName());
            g.renderComponentTooltip(font,lines,x,y);
        }}
    }
}

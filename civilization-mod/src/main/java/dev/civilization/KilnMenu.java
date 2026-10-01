package dev.civilization;
import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Native furnace processing with two coal slots and a shared cabinet layout. */
public final class KilnMenu extends AbstractContainerMenu implements CoalFireMenu {
    private final Container inventory;
    private final ContainerData data,states;
    public final List<RecipeHolder<? extends AbstractCookingRecipe>> recipes;
    public KilnMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(6),new SimpleContainerData(4));}
    public KilnMenu(int id,Inventory inv,Container c,ContainerData d){this(KilnContent.MENU.get(),KilnContent.RECIPE_TYPE.get(),id,inv,c,d);}
    public static KilnMenu retort(int id,Inventory inv){return new KilnMenu(KilnContent.RETORT_MENU.get(),KilnContent.RETORT_RECIPE_TYPE.get(),id,inv,new SimpleContainer(6),new SimpleContainerData(4));}
    public static KilnMenu foundry(int id,Inventory inv){return new KilnMenu(KilnContent.FOUNDRY_MENU.get(),KilnContent.FOUNDRY_RECIPE_TYPE.get(),id,inv,new SimpleContainer(6),new SimpleContainerData(4));}
    public static KilnMenu cooking(int id,Inventory inv){return new KilnMenu(CookingContent.MENU.get(),CookingContent.RECIPE_TYPE.get(),id,inv,new SimpleContainer(6),new SimpleContainerData(4));}
    public KilnMenu(MenuType<KilnMenu> type,RecipeType<? extends AbstractCookingRecipe> recipeType,int id,Inventory inv,Container c,ContainerData d){
        super(type,id);inventory=c;data=d;
        recipes=new ArrayList<>(inv.player.level().getRecipeManager().getAllRecipesFor(recipeType));recipes.sort(Comparator.comparing(r->r.id().toString()));
        addSlot(new Slot(c,0,17,43){@Override public boolean mayPlace(ItemStack s){return recipes.stream().anyMatch(r->r.value().getIngredients().getFirst().test(s));}});
        addSlot(new Slot(c,1,17,88){@Override public boolean mayPlace(ItemStack s){return KilnBlockEntity.acceptsFuel(s);}});
        addSlot(new FurnaceResultSlot(inv.player,c,2,125,43));
        addSlot(new Slot(c,3,35,88){@Override public boolean mayPlace(ItemStack s){return KilnBlockEntity.acceptsFuel(s);}});
        addSlot(new Slot(c,4,35,43){@Override public boolean mayPlace(ItemStack s){return recipes.stream().anyMatch(r->r.value().getIngredients().getFirst().test(s));}});
        addSlot(new FurnaceResultSlot(inv.player,c,5,143,43));
        MachineMenus.playerSlots(this::addSlot,inv,132);addDataSlots(d);
        states=c instanceof KilnBlockEntity machine?new ContainerData(){public int get(int i){return i==0?machine.structureStatus():i==1?machine.operatingStatus():i==2?machine.fire.state():i==3?machine.fire.remaining():machine.selection();}public void set(int i,int v){}public int getCount(){return 5;}}:new SimpleContainerData(5);
        addDataSlots(states);
    }
    @Override public boolean stillValid(Player p){return inventory.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){return MachineMenus.quickMove(this,p,index,6,new int[]{1,3,0,4},this::moveItemStackTo);}
    public String tooltipPrefix(){return getType()==KilnContent.FOUNDRY_MENU.get()?"foundry":getType()==CookingContent.MENU.get()?"cooking":getType()==KilnContent.RETORT_MENU.get()?"retort":"kiln";}
    public boolean requiresStructure(){return getType()!=CookingContent.MENU.get();}
    public int structureStatus(){return states.get(0);}public int operatingStatus(){return states.get(1);}
    public int fireState(){return states.get(2);}
    public int coalRemaining(){return states.get(3);}
    public int selection(){return states.get(4);}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!stillValid(p)||!(inventory instanceof KilnBlockEntity m))return false;
        if(id==CoalFire.BUTTON){boolean ok=m.fire.strike(p,m.checkStructure().status()==MachineStructure.COMPLETE);broadcastChanges();return ok;}
        if(id < -1 || id >= recipes.size())return false;
        m.selectRecipe(id);broadcastChanges();return true;
    }
    public boolean isLit(){return (fireState()&1)!=0;}
    public float getLitProgress(){return data.get(0)/(float)Math.max(1,data.get(1));}
    public float getBurnProgress(){return data.get(2)/(float)Math.max(1,data.get(3));}
    public int heatTicks(){return data.get(0);}
}

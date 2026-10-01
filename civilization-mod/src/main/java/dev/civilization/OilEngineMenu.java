package dev.civilization;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class OilEngineMenu extends AbstractContainerMenu {
 public final ContainerData data;private final OilEngineEntity engine;
 public OilEngineMenu(int id,Inventory inv){this(id,inv,null,new SimpleContainerData(7));}
 public OilEngineMenu(int id,Inventory inv,OilEngineEntity engine,ContainerData data){super(OilEngineContent.MENU.get(),id);this.engine=engine;this.data=data;MachineMenus.playerSlots(this::addSlot,inv,132);addDataSlots(data);}
 @Override public boolean stillValid(Player p){return engine==null||engine.valid(p);}
 @Override public boolean clickMenuButton(Player p,int id){if(engine==null||!engine.valid(p)||id<0||id>1)return false;engine.control(id);return true;}
 @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
}

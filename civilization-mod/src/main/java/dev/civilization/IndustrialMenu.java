package dev.civilization;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class IndustrialMenu extends AbstractContainerMenu implements CoalFireMenu {
    public final ContainerData data;
    private final Container inventory;
    public IndustrialMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(8),new SimpleContainerData(14));}
    public IndustrialMenu(int id,Inventory inv,Container machine,ContainerData data){super(IndustrialContent.MENU.get(),id);this.data=data;inventory=machine;
        for(int bank=0;bank<2;bank++)for(int n=0;n<4;n++){
            final boolean input=bank==0;final int offset=n;int slot=(input?new int[]{0,2,3,4}:MachineInventory.INDUSTRIAL_OUTPUT)[n];
            addSlot(new Slot(machine,slot,(input?17:106)+(n%2)*18,(input?88:34)+(n/2)*18){
                @Override public boolean mayPlace(ItemStack s){return input&&isActive()&&s.is(KilnContent.MINERAL_COAL.get());}
                @Override public boolean isActive(){return input?offset<2&&(kind()==IndustrialBlock.Kind.PUMP||kind()==IndustrialBlock.Kind.REFINERY):(kind()==IndustrialBlock.Kind.DRILL||kind()==IndustrialBlock.Kind.COLUMN);}
            });
        }
        MachineMenus.playerSlots(this::addSlot,inv,132);addDataSlots(data);
    }
    public int fireState(){return data.get(11);}
    public int coalRemaining(){return data.get(12);}
    public boolean hasCoalFire(){return kind()==IndustrialBlock.Kind.PUMP||kind()==IndustrialBlock.Kind.REFINERY;}
    @Override public boolean clickMenuButton(Player p,int id){if(id==ModeledDerrick.BUILD_BUTTON){boolean ok=stillValid(p)&&inventory instanceof IndustrialBlockEntity m&&ModeledDerrick.build(m,p);broadcastChanges();return ok;}if(id!=CoalFire.BUTTON||!stillValid(p)||!(inventory instanceof IndustrialBlockEntity m)||!m.coalPowered())return false;boolean ok=m.fire.strike(p,IndustrialStructure.bind(m));broadcastChanges();return ok;}
    public IndustrialBlock.Kind kind(){return IndustrialBlock.Kind.values()[Math.clamp(data.get(0),0,IndustrialBlock.Kind.values().length-1)];}
    @Override public boolean stillValid(Player p){return inventory.stillValid(p);}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)){p.closeContainer();return;}super.clicked(slot,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){return MachineMenus.quickMove(this,p,index,8,new int[]{0,1},this::moveItemStackTo);}
}

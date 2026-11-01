package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Native player slots and paged cargo identities. Large quantities never enter a native slot/cursor. */
public final class CargoMenu extends AbstractContainerMenu {
    public static final int ROWS=27, DATA=120;
    public static final int OFFHAND_SLOT=63;
    private final CargoHolder holder,target;
    private final SimpleContainer icons=new SimpleContainer(ROWS);
    public final ContainerData data;
    private int page;
    private int renderedPage=-1;
    private long renderedRevision=-1;
    public CargoMenu(int id,Inventory inv){this(id,inv,null);}
    public CargoMenu(int id,Inventory inv,CargoHolder holder){
        super(CargoContent.MENU.get(),id);this.holder=holder;target=holder==null?null:nearby(holder,inv.player);
        data=holder==null?new SimpleContainerData(DATA):new ContainerData(){
            public int getCount(){return DATA;}public void set(int i,int value){}
            public int get(int i){
                if(i<108){int row=i/4;return part(holder.cargo().count(page*ROWS+row),i%4);}
                if(i<112)return part(Double.doubleToLongBits(holder.cargo().mass()),i-108);
                if(i<116)return part(Double.doubleToLongBits(holder.cargo().capacity()),i-112);
                return switch(i){case 116->page;case 117->pages();case 118->target==null?0:target instanceof StockpileEntity?1:2;default->0;};
            }
        };
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(icons,row*9+col,8+col*18,54+row*18){
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public boolean mayPickup(Player p){return false;}
        });
        MachineMenus.playerSlots(this::addSlot,inv,169);if(BlockCarry.restricted(inv.player))addSlot(new Slot(inv,BlockCarry.OFFHAND,-25,227));addDataSlots(data);refresh();
    }
    private static int part(long value,int index){return (int)(value>>>(index*16))&65535;}
    private long value(int base){long n=0;for(int i=0;i<4;i++)n|=(long)(data.get(base+i)&65535)<<(16*i);return n;}
    public long count(int row){return value(row*4);}
    public double mass(){return Double.longBitsToDouble(value(108));}
    public double capacity(){return Double.longBitsToDouble(value(112));}
    public int page(){return data.get(116);}
    public int pageCount(){return data.get(117);}
    private int pages(){return Math.max(1,(holder.cargo().size()+ROWS-1)/ROWS);}
    private void refresh(){
        if(holder==null)return;page=Math.min(page,pages()-1);
        if(renderedPage==page&&renderedRevision==holder.cargo().revision())return;
        renderedPage=page;renderedRevision=holder.cargo().revision();
        for(int i=0;i<ROWS;i++){int at=page*ROWS+i;icons.setItem(i,at<holder.cargo().size()?holder.cargo().entry(at).item():ItemStack.EMPTY);}
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return holder==null||holder.cargoValid(p);}
    private boolean valid(Player p){if(holder==null||!holder.cargoValid(p)){if(!p.level().isClientSide)p.closeContainer();return false;}return true;}
    @Override public boolean canDragTo(Slot slot){return slot.container!=icons&&super.canDragTo(slot);}
    @Override public void clicked(int index,int button,ClickType type,Player p){
        if(p.level().isClientSide)return; // Stock actions are server-authoritative; native packets synchronize the cursor.
        if(!valid(p))return;
        if(index>=0&&index<ROWS){
            int at=page*ROWS+index;var cargo=holder.cargo();
            if(type==ClickType.QUICK_MOVE){withdraw(p,at,true,0);}
            else if(type==ClickType.PICKUP&&(button==0||button==1)){
                var carried=getCarried();
                if(carried.isEmpty())withdraw(p,at,false,button);
                else if(at>=cargo.size()||ItemStack.isSameItemSameComponents(carried,cargo.entry(at).item()))carried.shrink((int)cargo.insert(carried,button==1?1:carried.getCount()));
            }
            refresh();broadcastChanges();return;
        }
        super.clicked(index,button,type,p);
    }
    private void withdraw(Player p,int at,boolean inventory,int button){
        var cargo=holder.cargo();if(at<0||at>=cargo.size())return;
        var identity=cargo.entry(at).item();
        for(int attempt=0;attempt<(inventory?38:1);attempt++){
            if(at>=cargo.size()||!ItemStack.isSameItemSameComponents(identity,cargo.entry(at).item()))break;
            var e=cargo.entry(at);int amount=(int)Math.min(e.count(),e.item().getMaxStackSize());if(button==1)amount=(amount+1)/2;
            var stack=e.item().copyWithCount(amount);
            if(inventory){p.getInventory().add(stack);int moved=amount-stack.getCount();cargo.remove(at,moved);if(moved<amount)break;}
            else if(getCarried().isEmpty()){setCarried(stack);cargo.remove(at,amount);}
        }
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!valid(p)||index<ROWS||index>=slots.size())return ItemStack.EMPTY;
        var s=slots.get(index);var stack=s.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var before=stack.copy();
        int moved=(int)holder.cargo().insert(stack,stack.getCount());if(moved==0)return ItemStack.EMPTY;
        stack.shrink(moved);s.setChanged();refresh();return before;
    }
    @Override public boolean clickMenuButton(Player p,int id){
        if(!valid(p))return false;
        if(id==2||id==3){page=Math.clamp(page+(id==2?-1:1),0,pages()-1);refresh();return true;}
        if(id<0||id>1||target==null||!target.cargoValid(p)||holder.cargoPosition().distanceToSqr(target.cargoPosition())>9)return false;
        if(id==0)target.cargo().transferTo(holder.cargo());else holder.cargo().transferTo(target.cargo());
        refresh();broadcastChanges();return true;
    }
    private static CargoHolder nearby(CargoHolder source,Player p){
        if(source instanceof WheelbarrowEntity){
            var center=BlockPos.containing(source.cargoPosition());CargoHolder best=null;double distance=9;
            for(var at:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,1,3))){
                if(!source.cargoLevel().hasChunkAt(at))continue;
                if(source.cargoLevel().getBlockEntity(at) instanceof StockpileEntity s&&s.cargoValid(p)){
                    double d=s.cargoPosition().distanceToSqr(source.cargoPosition());if(d<distance){distance=d;best=s;}
                }
            }return best;
        }
        return source.cargoLevel().getEntitiesOfClass(WheelbarrowEntity.class,new AABB(source.cargoPosition(),source.cargoPosition()).inflate(3),e->e.cargoValid(p)&&e.cargoPosition().distanceToSqr(source.cargoPosition())<=9)
                .stream().min(java.util.Comparator.comparingDouble(e->e.cargoPosition().distanceToSqr(source.cargoPosition()))).orElse(null);
    }
}

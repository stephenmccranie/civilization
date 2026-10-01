package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Shared physical storage with two non-extractable offer templates. */
public final class CivicMenu extends AbstractContainerMenu {
    private final ServerPlayer player;
    private final CivicData data;
    private final CivicData.Shop shop;
    private final SimpleContainer storage, ghosts = new SimpleContainer(2);
    private final ContainerData values = new SimpleContainerData(5);
    public static void open(ServerPlayer p, CivicData.Address at, boolean land) {
        if (land) { LandMenu.open(p, at); return; }
        if (!CivicData.get(p.server).shops.containsKey(at)) return;
        p.openMenu(new SimpleMenuProvider((id, inv, ignored) -> new CivicMenu(id, p, at, false), Component.literal("Trade Counter")));
    }
    public CivicMenu(int id, Inventory inv) { this(id, inv, null, null); }
    public CivicMenu(int id, ServerPlayer p, CivicData.Address at, boolean ignored) { this(id, p.getInventory(), p, CivicData.get(p.server).shops.get(at)); }
    private CivicMenu(int id, Inventory inv, ServerPlayer p, CivicData.Shop shop) {
        super(CivicContent.SHOP_MENU.get(), id); this.player = p; this.shop = shop; data = p == null ? null : CivicData.get(p.server);
        storage = new SimpleContainer(27) {
            @Override public ItemStack getItem(int i) { return shop == null ? super.getItem(i) : shop.inventory.get(i); }
            @Override public void setItem(int i, ItemStack item) { if (shop == null) super.setItem(i,item); else { shop.inventory.set(i,item); setChanged(); } }
            @Override public ItemStack removeItem(int i, int n) { if (shop == null) return super.removeItem(i,n); var item = net.minecraft.world.ContainerHelper.removeItem(shop.inventory,i,n); setChanged(); return item; }
            @Override public void setChanged() { if (data != null) data.setDirty(); }
        };
        for (int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(storage,row*9+col,8+18*col,18+18*row) {
            @Override public boolean mayPlace(ItemStack s) { return manager(); }
            @Override public boolean mayPickup(Player p) { return manager(); }
        });
        addSlot(new Slot(ghosts,0,-54,32) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public boolean mayPickup(Player p) { return false; } });
        addSlot(new Slot(ghosts,1,214,32) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public boolean mayPickup(Player p) { return false; } });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inv,9+row*9+col,8+18*col,85+18*row));
        for(int col=0;col<9;col++) addSlot(new Slot(inv,col,8+18*col,143));
        addDataSlots(values);
    }
    public boolean manager() { return player == null ? values.get(0) != 0 : data.canManage(shop,player.getUUID()); }
    public int amount() { return values.get(1); }
    public int price() { return values.get(2); }
    public int revision() { return values.get(3); }
    public int batches() { return values.get(4); }
    @Override public boolean stillValid(Player p) { return player == null || player == p && data.shops.get(shop.at) == shop && p.level().dimension().location().toString().equals(shop.at.dimension()) && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(shop.at.pos())) <= 64 && p.level().getBlockState(shop.at.pos()).is(CivicContent.SHOP.get()); }
    @Override public void clicked(int slot, int mouse, ClickType type, Player p) {
        if (!stillValid(p)) { p.closeContainer(); return; }
        if (slot == 27 || slot == 28) {
            if (player != null && manager() && type == ClickType.PICKUP) {
                var item = getCarried().copyWithCount(1);
                if(slot == 27) { shop.template = item; shop.amount = 1; } else { shop.payment = item; shop.price = 1; }
                shop.revision = (shop.revision + 1) & 32767; data.setDirty();
            }
            if(player != null) { broadcastChanges(); broadcastFullState(); } return;
        }
        super.clicked(slot,mouse,type,p);
    }
    @Override public ItemStack quickMoveStack(Player p,int index) {
        if(index < 0 || index >= slots.size() || index == 27 || index == 28 || !manager()) return ItemStack.EMPTY;
        var slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY; var item=slot.getItem(); var copy=item.copy();
        if(index < 27) { if(!moveItemStackTo(item,29,65,true)) return ItemStack.EMPTY; }
        else if(!moveItemStackTo(item,0,27,false)) return ItemStack.EMPTY;
        if(item.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(p,item); return copy;
    }
    public void action(int button,int expectedRevision) {
        if(player == null || !stillValid(player)) return;
        if(expectedRevision != shop.revision) { player.displayClientMessage(Component.literal("Offer changed. Review it again."),false); broadcastChanges(); return; }
        if(button == 10) player.displayClientMessage(Component.literal(CivicService.trade(data,shop,player)),false);
        broadcastChanges();
    }
    public void quantities(int amount,int price,int expectedRevision) {
        if(player==null || !stillValid(player) || !manager()) return;
        if(expectedRevision!=shop.revision) { player.displayClientMessage(Component.literal("Offer changed. Review it again."),false); broadcastChanges(); return; }
        if(amount<1 || amount>9999 || price<1 || price>9999) { player.displayClientMessage(Component.literal("Enter quantities from 1 to 9999."),false); return; }
        if(shop.amount!=amount || shop.price!=price) {
            shop.amount=amount; shop.price=price; shop.revision=(shop.revision+1)&32767; data.setDirty();
        }
        broadcastChanges();
    }
    @Override public void broadcastChanges() {
        if(player != null) {
            ghosts.setItem(0,shop.template.copy()); ghosts.setItem(1,shop.payment.copy());
            values.set(0,manager()?1:0); values.set(1,shop.amount); values.set(2,shop.price); values.set(3,shop.revision);
            values.set(4,shop.payment.isEmpty()?0:shop.count(shop.payment)/shop.price);
        }
        super.broadcastChanges();
    }
}

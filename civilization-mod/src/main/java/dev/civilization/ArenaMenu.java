package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Four read-only escrow views; deposits only debit the selected held stack. */
public final class ArenaMenu extends AbstractContainerMenu {
    private final ServerPlayer player;
    private final ArenaData data;
    private final ArenaData.Pit pit;
    private final SimpleContainer views=new SimpleContainer(4);
    public final ContainerData values=new SimpleContainerData(18);
    private int selected;
    public static void open(ServerPlayer p,BlockPos at){var d=ArenaData.get(p.server);var pit=d.pits.get(ArenaService.address(p.serverLevel(),at));if(pit!=null)p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new ArenaMenu(id,inv,p,pit),Component.literal("Gladiator Pit")));}
    public ArenaMenu(int id,Inventory inv){this(id,inv,null,null);}
    public ArenaMenu(int id,ServerPlayer player,ArenaData.Pit pit){this(id,player.getInventory(),player,pit);}
    private ArenaMenu(int id,Inventory inv,ServerPlayer p,ArenaData.Pit pit){
        super(ArenaContent.MENU.get(),id);this.player=p;this.pit=pit;this.data=p==null?null:ArenaData.get(p.server);
        for(int i=0;i<4;i++)addSlot(new Slot(views,i,new int[]{42,226,42,226}[i],i<2?44:122){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,64+18*col,194+18*row));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,64+18*col,252));addDataSlots(values);
    }
    @Override public boolean stillValid(Player p){return player==null||player==p&&data.pits.get(pit.at)==pit&&p.level().dimension().location().toString().equals(pit.at.dimension())&&p.distanceToSqr(pit.at.pos().getCenter())<=64&&p.level().getBlockState(pit.at.pos()).is(ArenaContent.PIT.get());}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)){p.closeContainer();return;}if(slot>=0&&slot<4)return;super.clicked(slot,button,type,p);}
    public void action(int revision,int action,int offer){
        if(player==null||!stillValid(player)||revision!=pit.revision)return;
        if(action==10||action==11){selected=Math.clamp(selected+(action==10?-1:1),0,Math.max(0,pit.bets.size()-1));}
        else {if(action==8||action==9){if(pit.bets.isEmpty()||pit.bets.get(Math.min(selected,pit.bets.size()-1)).id!=offer)return;}ArenaService.action(data,pit,player,action,offer);}
        broadcastChanges();broadcastFullState();
    }
    @Override public void broadcastChanges(){
        if(player!=null){
            values.set(0,pit.phase);values.set(1,pit.revision);values.set(2,pit.side(player.getUUID()));
            for(int i=0;i<2;i++){var f=pit.fighters[i]==null?null:player.server.getPlayerList().getPlayer(pit.fighters[i]);values.set(3+i,f==null?0:f.getId());values.set(5+i,pit.accepted[i]?1:0);values.set(7+i,pit.ready[i]?1:0);views.setItem(i,pit.stakes[i].copy());}
            selected=Math.min(selected,Math.max(0,pit.bets.size()-1));var b=pit.bets.isEmpty()?null:pit.bets.get(selected);
            values.set(9,pit.bets.size());values.set(10,selected+1);values.set(11,b==null?0:b.id);values.set(12,b==null?0:b.side);values.set(13,b!=null&&b.opponent!=null?1:0);
            values.set(14,(int)Math.clamp((pit.deadline-player.serverLevel().getGameTime()+19)/20,0,300));values.set(15,pit.winner);values.set(16,pit.owner.equals(player.getUUID())||player.hasPermissions(2)?1:0);
            var owed=data.credits.get(player.getUUID());values.set(17,owed==null?0:owed.size());views.setItem(2,b==null?ItemStack.EMPTY:b.stake.copy());views.setItem(3,owed==null||owed.isEmpty()?ItemStack.EMPTY:owed.getFirst().copy());
        }super.broadcastChanges();
    }
}

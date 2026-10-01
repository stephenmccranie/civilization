package dev.civilization;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
public final class SurveyMenu extends AbstractContainerMenu {
    private final ServerPlayer player;
    private int centerX,centerZ,zoom=4;
    private long lastSent=Long.MIN_VALUE;
    private final String dimension;
    public SurveyPayload snapshot;
    private final net.minecraft.core.BlockPos anchor;
    public SurveyMenu(int id,Inventory inv){this(id,inv,null,net.minecraft.core.BlockPos.ZERO);}
    public SurveyMenu(int id,Inventory inv,ServerPlayer p,net.minecraft.core.BlockPos at){super(CivicContent.SURVEY_MENU.get(),id);player=p;anchor=at.immutable();dimension=p==null?"":p.level().dimension().location().toString();centerX=at.getX();centerZ=at.getZ();zoom=1;}
    public static void open(ServerPlayer p,net.minecraft.core.BlockPos at){if(SurveyTable.complete(p.level(),at)&&CivicAccess.allowed(p.level(),at,p)&&at.distToCenterSqr(p.position())<=64)p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new SurveyMenu(id,inv,p,at),Component.literal("Survey Table")));}
    @Override public boolean stillValid(Player p){return player==null||p==player&&p.level().dimension().location().toString().equals(dimension)&&anchor.distToCenterSqr(p.position())<=64&&SurveyTable.complete(p.level(),anchor)&&CivicAccess.allowed(p.level(),anchor,p);}
    @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player p,int id){return false;}
    public static SurveyPayload snapshot(ServerPlayer p,int menuId,int centerX,int centerZ,int zoom){
        var d=CivicData.get(p.server);String dim=p.level().dimension().location().toString();int reach=128*zoom;long now=System.currentTimeMillis();var markers=new ArrayList<SurveyPayload.Marker>();boolean more=false;
        for(var c:d.claims.values()) {
            if(!c.at.dimension().equals(dim)||Math.abs((long)c.at.pos().getX()-centerX)>reach+c.radius||Math.abs((long)c.at.pos().getZ()-centerZ)>reach+c.radius)continue;
            d.settle(c,now);if(c.energy<=0)continue;
            if(markers.size()>=256){more=true;continue;}
            markers.add(new SurveyPayload.Marker(true,c.at.pos().getX(),c.at.pos().getY(),c.at.pos().getZ(),c.radius,c.height,"Claim: "+d.ownerName(c.owner,p.server),"Size: "+(c.radius*2)+" × "+(c.radius*2)+"\nHeight: Y "+(c.at.pos().getY()-c.height/2)+" to "+(c.at.pos().getY()+c.height/2-1)+(c.buyer==null?"":"\nHandover pending")));
        }
        for(var s:d.shops.values()) {
            if(!s.at.dimension().equals(dim)||Math.abs((long)s.at.pos().getX()-centerX)>=reach||Math.abs((long)s.at.pos().getZ()-centerZ)>=reach)continue;
            if(markers.size()>=256){more=true;continue;}
            String details=s.template.isEmpty()||s.payment.isEmpty()?"No offer configured":"You give: "+s.amount+" "+s.template.getHoverName().getString()+"\nYou get: "+s.price+" "+s.payment.getHoverName().getString()+"\nPayments stocked: "+s.count(s.payment)/s.price+" trades\nVisit the counter to trade.";
            markers.add(new SurveyPayload.Marker(false,s.at.pos().getX(),s.at.pos().getY(),s.at.pos().getZ(),0,0,"Counter: "+d.ownerName(s.owner,p.server),details.substring(0,Math.min(512,details.length()))));
        }
        return new SurveyPayload(menuId,centerX,centerZ,zoom,p.getBlockX(),p.getBlockZ(),new int[4096],List.copyOf(markers),more);
    }
    @Override public void broadcastChanges(){
        super.broadcastChanges();if(player==null||player.containerMenu!=this||!stillValid(player))return;
        long now=player.serverLevel().getGameTime();if(lastSent!=Long.MIN_VALUE&&now-lastSent<40)return;lastSent=now;
        var data=snapshot(player,containerId,centerX,centerZ,zoom);
        if(player.level().getBlockEntity(anchor) instanceof SurveyBlockEntity table)data=new SurveyPayload(containerId,centerX,centerZ,1,player.getBlockX(),player.getBlockZ(),table.terrain.clone(),data.markers(),data.truncated());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,data);
    }
}

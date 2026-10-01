package dev.civilization;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
public final class ProspectingRod extends Item {
    public ProspectingRod(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getLevel() instanceof ServerLevel l && c.getPlayer()!=null){
            if(c.getPlayer().getCooldowns().isOnCooldown(this))return InteractionResult.CONSUME;
            c.getPlayer().getCooldowns().addCooldown(this,20);
            var p=c.getClickedPos();var s=Deposits.nearby(l,p);
            String text="No large deposit within 192 blocks.";
            if(s!=null){String name=s.kind()==Deposits.Kind.COAL?"Coal seam":"Oil reservoir";
                text=name+" at X "+s.x()+", Z "+s.z()+" | underground Y "+s.bottom()+" to "+s.top()+" | "+(s.contains(p)?"Build a "+(s.kind()==Deposits.Kind.COAL?"Coal Drill":"Oil Derrick")+" here":"Move over the deposit footprint near surface Y "+s.y());}
            c.getPlayer().displayClientMessage(Component.literal(text),false);
        }return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
}

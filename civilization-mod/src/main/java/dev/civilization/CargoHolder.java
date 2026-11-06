package dev.civilization;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface CargoHolder {
    /** How far a player reaches a store to open it, and a parked wheelbarrow the pallet it loads from or unloads to. */
    int REACH=6;
    CargoInventory cargo();
    Level cargoLevel();
    Vec3 cargoPosition();
    /** Whether the player may use this store from up to `reach` blocks away. */
    boolean cargoValid(Player player,double reach);
    default boolean cargoValid(Player player){return cargoValid(player,REACH);}
    Component cargoName();
    /**
     * The distinct stores within REACH of a point, nearest first: each Pallet group once, by its nearest member, and
     * each wheelbarrow. Only block entities of loaded chunks and loaded entities are read.
     */
    static List<CargoHolder> around(Level level,Vec3 center){
        var pallets=new ArrayList<CargoHolder>();
        int x=(int)Math.floor(center.x),z=(int)Math.floor(center.z);
        for(int cx=(x-REACH)>>4;cx<=(x+REACH)>>4;cx++)for(int cz=(z-REACH)>>4;cz<=(z+REACH)>>4;cz++){
            var chunk=Chunks.chunk(level,cx,cz);if(chunk==null)continue;
            for(var entity:chunk.getBlockEntities().values())if(entity instanceof StockpileEntity s&&s.cargoPosition().distanceToSqr(center)<=REACH*REACH)pallets.add(s);
        }
        var found=new ArrayList<CargoHolder>();
        found.addAll(pallets);found.addAll(level.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(center,center).inflate(REACH),w->w.cargoPosition().distanceToSqr(center)<=REACH*REACH));
        found.sort(Comparator.comparingDouble(s->s.cargoPosition().distanceToSqr(center)));
        var groups=Collections.newSetFromMap(new IdentityHashMap<CargoInventory,Boolean>());
        found.removeIf(s->!groups.add(s.cargo()));
        return found;
    }
    /** A right-click puts the first held goods the store accepts onto it, main hand first. False when nothing fits, and the click opens the store. */
    default boolean putHeld(Player p){
        for(var hand:net.minecraft.world.InteractionHand.values()){
            var held=p.getItemInHand(hand);var name=held.getHoverName();
            int n=(int)cargo().insert(held,held.getCount());
            if(n>0){held.shrink(n);p.displayClientMessage(Component.literal("Stacked "+n+" ").append(name).append(" · "+CargoInventory.blocks(cargo().load(),cargo().capacity())),true);return true;}
        }
        return false;
    }
}

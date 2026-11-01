package dev.civilization.mixin;

import dev.civilization.WheelbarrowEntity;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityGetter.class)
public interface WheelbarrowCollisionMixin {
    @Inject(method="getEntityCollisions",at=@At("HEAD"),cancellable=true)
    private void civilization$carrierShapes(Entity moving,AABB box,CallbackInfoReturnable<List<VoxelShape>> result){
        if(box.getSize()<1e-7)return;
        Predicate<Entity> predicate=moving==null?EntitySelector.CAN_BE_COLLIDED_WITH:EntitySelector.NO_SPECTATORS.and(moving::canCollideWith);
        var entities=((EntityGetter)this).getEntities(moving,box.inflate(1e-7),predicate);
        // Preserve native entity collision behavior; only the carrier replaces
        // its broad-phase body bounds with its actual solid surfaces.
        var shapes=new ArrayList<VoxelShape>(entities.size());
        for(var entity:entities){
            if(entity instanceof WheelbarrowEntity e){
                var shape=e.playerCollisionShape();
                // A pre-existing overlap must be escapable. Complex voxel grids
                // otherwise expose internal planes and can trap the player.
                if(moving instanceof net.minecraft.world.entity.player.Player&&Shapes.joinIsNotEmpty(shape,Shapes.create(moving.getBoundingBox().deflate(1e-5)),BooleanOp.AND))continue;
                if(Shapes.joinIsNotEmpty(shape,Shapes.create(box),BooleanOp.AND))shapes.add(shape);
            }
            else shapes.add(Shapes.create(entity.getBoundingBox()));
        }
        result.setReturnValue(shapes);
    }
}

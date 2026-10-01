package dev.civilization.lab;

import dev.civilization.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.*;

/** The only laboratory class coupled to Sable's assembly/control API. No survival rules here. */
public final class SableAdapter {
    public static final String TAG="civilization_lab";
    public record Input(double throttle,double steering,boolean brake){}
    private static final Map<UUID,Input> controls=new HashMap<>();
    public static void clear(){controls.clear();}
    public static List<ServerSubLevel> platforms(ServerLevel level){var c=SubLevelContainer.getContainer(level);return c==null?List.of():c.getAllSubLevels().stream().filter(s->s.getUserDataTag().getBoolean(TAG)).toList();}
    public static ServerSubLevel create(ServerLevel level,BlockPos origin){
        var blocks=new ArrayList<BlockPos>();
        for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++)blocks.add(origin.offset(x,0,z));
        blocks.add(origin.above());blocks.add(origin.offset(2,1,0));
        for(var pos:blocks)if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir())throw new IllegalStateException("Platform needs empty, loaded space");
        for(var pos:blocks)level.setBlockAndUpdate(pos,Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.above(),Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)level.getBlockEntity(origin.above());chest.setItem(0,KilnContent.MINERAL_COAL.toStack(23));chest.setChanged();
        var cutPos=origin.offset(2,1,0);level.setBlockAndUpdate(cutPos,CuttingContent.PIECE.get().defaultBlockState());
        var cells=new net.minecraft.world.level.block.state.BlockState[8];cells[0]=Blocks.COPPER_BLOCK.defaultBlockState();cells[1]=Blocks.BRICKS.defaultBlockState();
        ((CutBlockEntity)level.getBlockEntity(cutPos)).cells(cells);
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,origin,blocks,BoundingBox3i.from(blocks));
        ship.setName("Civilization cargo laboratory");
        var data=new CompoundTag();data.putBoolean(TAG,true);data.putDouble("hoverY",ship.logicalPose().position().y());data.putLong("chest",ship.getPlot().getCenterBlock().above().asLong());data.putLong("cut",ship.getPlot().getCenterBlock().offset(2,1,0).asLong());ship.setUserDataTag(data);
        control(ship,0,0,true);return ship;
    }
    public static void control(ServerSubLevel ship,double throttle,double steering,boolean brake){controls.put(ship.getUniqueId(),new Input(Math.clamp(throttle,-1,1),Math.clamp(steering,-1,1),brake));}
    public static void step(ServerSubLevel ship,double dt){
        var body=RigidBodyHandle.of(ship);if(body==null||!body.isValid())return;
        double mass=ship.getMassTracker().getMass();if(!Double.isFinite(mass)||mass<=0)return;
        var input=controls.getOrDefault(ship.getUniqueId(),new Input(0,0,true));
        var velocity=body.getLinearVelocity(new Vector3d());var angular=body.getAngularVelocity(new Vector3d());
        var pose=ship.logicalPose();
        // Diagnostic hover support isolates transport APIs; this is not a fuel or airship lift model.
        var acceleration=new Vector3d(-velocity.x*(input.brake()?6:.35),Math.clamp(11+4*(ship.getUserDataTag().getDouble("hoverY")-pose.position().y())-3*velocity.y,-25,25),-velocity.z*(input.brake()?6:.35));
        var localImpulse=pose.transformNormalInverse(acceleration,new Vector3d()).mul(mass*dt);
        if(!input.brake())localImpulse.x+=input.throttle()*mass*3*dt;
        body.applyLinearImpulse(localImpulse);
        var angularAcceleration=new Vector3d(-angular.x*4,(input.brake()?0:input.steering()*.35)-angular.y,-angular.z*4).mul(4);
        var localAngular=pose.transformNormalInverse(angularAcceleration,new Vector3d());
        ship.getMassTracker().getInertiaTensor().transform(localAngular).mul(dt);body.applyAngularImpulse(localAngular);
    }
    public static Vec3 deck(ServerSubLevel ship){return ship.logicalPose().transformPosition(Vec3.atLowerCornerOf(ship.getPlot().getCenterBlock()).add(-1,1.05,0));}
    public static double speed(ServerSubLevel ship){var h=RigidBodyHandle.of(ship);return h==null?0:h.getLinearVelocity(new Vector3d()).length();}
    public static void assertCargo(ServerSubLevel ship){
        var level=ship.getLevel();var data=ship.getUserDataTag();
        if(!(level.getBlockEntity(BlockPos.of(data.getLong("chest"))) instanceof ChestBlockEntity chest)||!chest.getItem(0).is(KilnContent.MINERAL_COAL.get())||chest.getItem(0).getCount()!=23)throw new IllegalStateException("Cargo was lost or changed");
        if(!(level.getBlockEntity(BlockPos.of(data.getLong("cut"))) instanceof CutBlockEntity cut)||cut.cells()[0]==null||!cut.cells()[0].is(Blocks.COPPER_BLOCK)||cut.cells()[1]==null||!cut.cells()[1].is(Blocks.BRICKS)||CutCells.mask(cut.cells())!=3)throw new IllegalStateException("Mixed cut cells were not preserved");
    }
}

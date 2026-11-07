package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Disposable fixtures build native-model machines the way players do, section by section. */
final class MachineFixture {
    static void build(ServerLevel l,BlockPos at){
        var modeled=ModeledMachine.of(l.getBlockEntity(at));
        var model=modeled.model();var front=modeled.front();
        for(var c:model.cells){var p=ModeledMachine.position(at,front,c);l.getChunkAt(p);l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());}
        for(var port:model.ports)l.setBlockAndUpdate(MachineStructure.position(at,front,port.part()),Blocks.AIR.defaultBlockState());
        var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"machine-fixture"));p.setGameMode(GameType.CREATIVE);p.setPos(at.getX()+.5,at.getY()+1,at.getZ()+.5);
        if(!ModeledMachine.assemble(modeled,p))throw new IllegalStateException(model.id+" fixture could not assemble");
    }
    /** Chooses the level's next rolls so the next spark struck in it catches, or fails; the fire still pays and burns as it would. */
    static void spark(ServerLevel l,boolean catches){
        for(long seed=0;;seed++){var r=net.minecraft.util.RandomSource.create(seed);r.nextFloat();if((r.nextInt(3)==0)==catches){l.random.setSeed(seed);return;}}
    }
    static int status(ServerLevel l,BlockPos at){return ModeledMachine.status(ModeledMachine.of(l.getBlockEntity(at)));}
}

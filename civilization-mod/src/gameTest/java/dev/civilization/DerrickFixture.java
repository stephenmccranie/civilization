package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Disposable scene fixtures use the same validated construction as the player. */
final class DerrickFixture {
    static void assemble(IndustrialBlockEntity m){
        var l=(ServerLevel)m.getLevel();var at=m.getBlockPos();
        for(var c:MachineModel.OIL_DERRICK.cells){var p=ModeledMachine.position(at,m.front(),c);l.getChunkAt(p);l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());}
        l.setBlockAndUpdate(MachineStructure.position(at,m.front(),MachineModel.OIL_DERRICK.ports.getFirst().part()),Blocks.AIR.defaultBlockState());
        var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"derrick-fixture"));p.setGameMode(GameType.CREATIVE);p.setPos(at.getX()+.5,at.getY()+1,at.getZ()-3);
        if(!ModeledMachine.assemble(m,p))throw new IllegalStateException("Derrick fixture could not assemble");
    }
}

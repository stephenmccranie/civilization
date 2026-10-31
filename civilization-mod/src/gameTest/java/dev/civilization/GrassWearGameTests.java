package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class GrassWearGameTests {
    private static int total(net.minecraft.world.level.chunk.LevelChunk chunk){
        if(!chunk.hasData(GrassWearChunk.TYPE))return 0;int n=0;for(long saved:chunk.getData(GrassWearChunk.TYPE).wear.save())n+=saved&127;return n;
    }
    @GameTest(template="industrial") public static void onlyRealGroundFootstepsWearGrass(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(3,3,3));
        for(int x=-1;x<=6;x++){l.setBlockAndUpdate(a.offset(x,-1,0),Blocks.GRASS_BLOCK.defaultBlockState());for(int y=0;y<3;y++)l.setBlockAndUpdate(a.offset(x,y,0),Blocks.AIR.defaultBlockState());}
        var p=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"grass-walker"));
        p.setPos(a.getX()+.1,a.getY(),a.getZ()+.5);p.setOnGround(true);GrassWearSystem.step(p);
        var chunk=l.getChunkAt(a);int before=total(chunk);
        for(int i=0;i<5;i++){p.setPos(p.getX()+.16,p.getY(),p.getZ());p.setOnGround(true);GrassWearSystem.step(p);}
        h.assertTrue(total(chunk)==before+1,"Accepted ground distance creates one actual footfall");
        for(int i=0;i<30;i++)GrassWearSystem.step(p);h.assertTrue(total(chunk)==before+1,"Standing still does not wear grass");
        p.getAbilities().flying=true;p.setPos(p.getX()+1,p.getY(),p.getZ());GrassWearSystem.step(p);
        p.getAbilities().flying=false;p.setOnGround(false);p.setPos(p.getX()+1,p.getY(),p.getZ());GrassWearSystem.step(p);
        p.setOnGround(true);p.setPos(p.getX()+200,p.getY(),p.getZ());GrassWearSystem.step(p);
        h.assertTrue(total(chunk)==before+1,"Flight, airborne travel and large teleports do not trample");h.succeed();
    }
    @GameTest(template="industrial") public static void wornGrassPersistsAndSyncsOnlyVisibleStages(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(4,2,4));l.setBlockAndUpdate(at,Blocks.GRASS_BLOCK.defaultBlockState());
        var chunk=l.getChunkAt(at);long key=GrassWear.key(at.getX(),at.getY(),at.getZ());
        for(int i=0;i<7;i++)GrassWearSystem.record(l,at);
        var snap=GrassWearSystem.snapshot(chunk);h.assertTrue(java.util.Arrays.stream(snap.cells()).noneMatch(c->c>>4==key),"Sub-threshold footfalls need no mesh update");
        GrassWearSystem.record(l,at);snap=GrassWearSystem.snapshot(chunk);
        h.assertTrue(java.util.Arrays.stream(snap.cells()).anyMatch(c->c>>4==key&&(c&15)==1),"Crossing eight steps sends a visual stage");
        var original=chunk.getData(GrassWearChunk.TYPE);var restored=new GrassWearChunk();restored.deserializeNBT(l.registryAccess(),original.serializeNBT(l.registryAccess()));
        h.assertTrue(restored.wear.steps(key)==8,"Actual attachment NBT keeps exact footfall counts");
        for(int i=0;i<100;i++)l.setDayTime(l.getDayTime()+24000);
        h.assertTrue(original.wear.steps(key)==8,"Elapsed days do not restore unused grass");
        l.setBlockAndUpdate(at,Blocks.DIRT.defaultBlockState());h.assertTrue(!GrassWearSystem.record(l,at),"Ordinary dirt is not grass wear");h.succeed();
    }
    @GameTest(template="industrial") public static void grassSnapshotsRoundTripAndRejectOversizedPackets(GameTestHelper h){
        var l=h.getLevel();long cell=(GrassWear.key(-1,4096,-1)<<4)|8;
        var original=new GrassWearPayload(l.dimension().location(),-1,-1,true,new long[]{cell});
        var bytes=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),l.registryAccess());
        try{GrassWearPayload.CODEC.encode(bytes,original);var copy=GrassWearPayload.CODEC.decode(bytes);h.assertTrue(copy.dimension().equals(original.dimension())&&copy.chunkX()==-1&&copy.cells()[0]==cell,"Native snapshot codec preserves tall/negative positions");}
        finally{bytes.release();}
        var bad=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),l.registryAccess());
        try{
            bad.writeResourceLocation(l.dimension().location());bad.writeInt(0);bad.writeInt(0);bad.writeBoolean(true);bad.writeVarInt(GrassWear.MAX_CELLS+1);
            boolean rejected=false;try{GrassWearPayload.CODEC.decode(bad);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Oversized snapshots cannot allocate unbounded client state");
        }finally{bad.release();}
        h.succeed();
    }
}

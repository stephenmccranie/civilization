package dev.civilization;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class SurveyGameTests {
    @GameTest(template="empty") public static void tableAutomaticallySurveysAndPersists(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(1,2,1));var level=h.getLevel();
        level.setBlockAndUpdate(at,CivicContent.TABLE.get().defaultBlockState());
        for(var pos:SurveyTable.positions(level,at))if(!pos.equals(at))level.setBlockAndUpdate(pos,CivicContent.TABLE_PART.get().defaultBlockState());
        var table=(SurveyBlockEntity)level.getBlockEntity(at);table.sample(4096);
        int known=0;for(int color:table.terrain)if(color!=0)known++;
        h.assertTrue(known>0,"Nearby loaded surface is surveyed without player exploration");
        var copy=new SurveyBlockEntity(at,table.getBlockState());copy.loadWithComponents(table.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        h.assertTrue(java.util.Arrays.equals(table.terrain,copy.terrain),"Table terrain survives save/load");
        h.succeed();
    }
    @GameTest(template="empty") public static void publicMarkersDoNotLoadChunksOrRevealTerrain(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"map-test"));var d=CivicData.get(p.server);var at=new CivicData.Address("minecraft:overworld",new BlockPos(4000000,80,4000000));
        var c=new CivicData.Claim();c.at=at;c.owner=new CivicData.Owner(p.getUUID(),false);c.coalUnit=3600000;c.energy=3600000;c.lastUpdate=System.currentTimeMillis();d.claims.put(at,c);d.rebuildIndex();
        var shop=new CivicData.Shop();shop.at=at;shop.owner=c.owner;shop.template=Items.WHEAT.getDefaultInstance();shop.amount=65;shop.price=3;shop.inventory.set(0,KilnContent.MINERAL_COAL.toStack(9));d.shops.put(at,shop);
        try{
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(250000,250000)==null,"Fixture region starts unloaded");
            var snapshot=SurveyMenu.snapshot(p,1,4000000,4000000,1);
            h.assertTrue(snapshot.markers().size()==2&&snapshot.markers().stream().anyMatch(m->!m.claim()&&m.details().contains("65")&&m.details().contains("3 trades")),"Remote public claim and actual counter batch/stock are visible");
            for(int color:snapshot.terrain())h.assertTrue(color==0,"Public marker does not reveal surrounding terrain");
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(250000,250000)==null,"Browsing never loads remote chunks");
            c.energy=0;h.assertTrue(SurveyMenu.snapshot(p,1,4000000,4000000,1).markers().size()==1,"Expired claim disappears while counter remains");
        }finally{d.claims.remove(at);d.shops.remove(at);d.rebuildIndex();}h.succeed();
    }
    @GameTest(template="empty") public static void tableRequiresCompleteNearbyStructure(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(1,2,1));
        var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"table-reader"));p.setPos(at.getX()+.5,at.getY()+1,at.getZ()+.5);
        level.setBlockAndUpdate(at,CivicContent.TABLE.get().defaultBlockState());
        var menu=new SurveyMenu(15,p.getInventory(),p,at);
        h.assertTrue(!menu.stillValid(p),"Controller alone does not grant map access");
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
            for(var pos:SurveyTable.positions(level,at))if(!pos.equals(at))level.removeBlock(pos,false);
            level.setBlockAndUpdate(at,CivicContent.TABLE.get().defaultBlockState().setValue(CivicBlock.FACING,direction));
            for(var pos:SurveyTable.positions(level,at))if(!pos.equals(at))level.setBlockAndUpdate(pos,CivicContent.TABLE_PART.get().defaultBlockState());
            h.assertTrue(menu.stillValid(p),"Complete table works in every facing with empty hands");
        }
        h.assertTrue(!menu.clickMenuButton(p,0),"Fixed table map cannot browse remotely");
        p.setPos(at.getX()+20,at.getY(),at.getZ());h.assertTrue(!menu.stillValid(p),"Leaving table closes access");
        p.setPos(at.getX()+.5,at.getY()+1,at.getZ()+.5);
        level.removeBlock(SurveyTable.positions(level,at).get(1),false);h.assertTrue(!menu.stillValid(p),"Breaking a section invalidates open map immediately");
        h.succeed();
    }
}

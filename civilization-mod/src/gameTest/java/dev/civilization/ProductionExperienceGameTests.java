package dev.civilization;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ProductionExperienceGameTests {
    @GameTest(template="industrial") public static void thermalOutputAwardsSavedRecipeXpOnce(GameTestHelper h){
        var k=KilnGameTests.kiln(h);k.setItem(0,new ItemStack(Items.CLAY,5));k.setItem(1,KilnContent.MINERAL_COAL.toStack(8));KilnGameTests.ticks(h,k,1100);
        var copy=new KilnBlockEntity(k.getBlockPos(),k.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(k.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.getLevel().setBlockEntity(copy);
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"xp_test"));player.setPos(k.getBlockPos().getCenter());
        copy.awardUsedRecipesAndPopExperience(player);
        var area=new net.minecraft.world.phys.AABB(k.getBlockPos()).inflate(2);
        var orbs=h.getLevel().getEntitiesOfClass(ExperienceOrb.class,area);h.assertTrue(!orbs.isEmpty(),"Saved completed custom recipes release XP");orbs.forEach(ExperienceOrb::discard);
        copy.awardUsedRecipesAndPopExperience(player);h.assertTrue(h.getLevel().getEntitiesOfClass(ExperienceOrb.class,area).isEmpty(),"Claiming twice cannot duplicate XP");h.succeed();
    }
    @GameTest(template="industrial") public static void workshopOutputBanksCollectSavedXpOnce(GameTestHelper h){
        var w=WorkshopGameTests.build(h,0,Direction.NORTH);w.setItem(0,WorkshopContent.HIDE.toStack(2));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));WorkshopGameTests.run(w,1200);
        h.assertTrue(w.pendingExperience==2,"Each completed production batch earns one XP");
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(w.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.pendingExperience==2,"XP survives save/load");
        copy.setItem(7,copy.removeItem(5,2));h.assertTrue(copy.pendingExperience==2,"Automation removal retains XP");
        var slot=new WorkshopResultSlot(copy,7,0,0);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        slot.onTake(player,slot.remove(1));h.assertTrue(copy.pendingExperience==0,"Second output bank claims accumulated XP");
        slot.onTake(player,slot.remove(1));h.assertTrue(copy.pendingExperience==0,"Remaining output does not mint XP");h.succeed();
    }
}

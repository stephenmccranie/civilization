package dev.civilization;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class WorldTimeGameTests {
    @GameTest(template="industrial") public static void nativeClockUsesSixtyMinutesAndHonorsSavedOverrides(GameTestHelper h) throws Exception {
        var level=h.getLevel();float speed=level.getDayTimePerTick(),fraction=level.getDayTimeFraction();long gameTime=level.getGameTime(),dayTime=level.getDayTime();
        boolean daylight=level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        try {
            level.setDayTimePerTick(-1f);var defaults=new WorldTime.Defaults();defaults.apply(level);
            h.assertTrue(Math.abs(level.getDayTimePerTick()-1f/3)<.00001,"Native clock set to one-third speed");
            var advance=net.minecraft.world.level.Level.class.getDeclaredMethod("advanceDaytime");advance.setAccessible(true);
            level.setDayTimeFraction(0);long advanced=0;for(int i=0;i<72000;i++)advanced+=(long)advance.invoke(level);
            h.assertTrue(advanced==24000,"72,000 simulation ticks advance exactly one sun/moon cycle");
            h.assertTrue(level.getGameTime()==gameTime && level.getDayTime()==dayTime,"Setting the clock rate never rewinds world or simulation time");
            h.assertTrue(level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)==daylight,"Frozen daylight gamerule is preserved");
            var saved=defaults.save(new CompoundTag(),level.registryAccess());level.setDayTimePerTick(-1f);WorldTime.Defaults.load(saved,level.registryAccess()).apply(level);
            h.assertTrue(level.getDayTimePerTick()==-1f,"Admin reset to vanilla is preserved on reload");
            level.setDayTimePerTick(.5f);new WorldTime.Defaults().apply(level);h.assertTrue(level.getDayTimePerTick()==.5f,"Existing custom clock speed is respected");
        } finally {level.setDayTimePerTick(speed);level.setDayTimeFraction(fraction);}
        h.succeed();
    }
}

package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.PacketDistributor;

/** Replaces FoodData itself: vanilla has no second food, saturation, or exhaustion store. */
public final class CalorieFoodData extends FoodData {
    private final CalorieReserve reserve;
    private int healingTicks, starvingTicks, syncTicks;
    private double lastSent = -1, lastCapacity = -1, lastSprintMinimum = -1;
    private boolean depleted, lastDepleted;
    private double lastRecovery = -1;
    public long broken, placed;
    public double walkDistance, sprintDistance, laborSpent, travelSpent, otherSpent, eaten;

    public CalorieFoodData() {
        double capacity = CalorieConfig.SPEC.isLoaded() ? CalorieConfig.CAPACITY.get() : 2400;
        reserve = new CalorieReserve(capacity, capacity);
    }

    public CalorieReserve reserve() { return reserve; }
    public boolean isDepleted() { return depleted || reserve.calories() <= 0; }
    public double recoveryThreshold() { return Math.min(reserve.capacity(), CalorieConfig.RECOVERY.get()); }
    public void updateRecovery(Player player) {
        boolean next = RecoveryRules.depleted(depleted, reserve.calories(), recoveryThreshold());
        if (next != depleted) {
            depleted = next;
            EnergyLog.marker(player, next ? "depleted_enter" : "depleted_recovered");
            if (player instanceof ServerPlayer) player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    next ? "message.civilization.depleted" : "message.civilization.recovered", (int) recoveryThreshold()), false);
        }
    }
    public static CalorieFoodData of(Player player) { return (CalorieFoodData) player.getFoodData(); }
    public static boolean active(Player player) {
        return !player.level().isClientSide && player.isAlive() && !player.isCreative()
                && !player.isSpectator() && !player.getAbilities().invulnerable;
    }

    public void spendLabor(Player player, double kcal, boolean mining, String block) {
        double before = reserve.calories();
        laborSpent += reserve.spend(kcal);
        if (mining) broken++; else placed++;
        EnergyLog.record(player, mining ? "break_block" : "place_block", block, -kcal,
                before, reserve.calories(), 1, false);
        updateRecovery(player);
    }

    public void move(Player player, double dx, double dy, double dz) {
        if (!active(player) || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying) return;
        double distance = MovementCost.distance(dx, dy, dz, player.isInWater(), player.onClimbable());
        boolean sprint = player.isSprinting();
        if (distance <= 0) return;
        if (sprint) sprintDistance += distance; else walkDistance += distance;
        double cost = MovementCost.calories(distance, sprint, CalorieConfig.WALK.get(), CalorieConfig.SPRINT_MULTIPLIER.get());
        double before = reserve.calories();
        travelSpent += reserve.spend(cost);
        EnergyLog.record(player, sprint ? "sprint" : "walk", player.isInWater() ? "swimming" : player.onClimbable() ? "climbing" : "ground_or_air",
                -cost, before, reserve.calories(), distance, true);
        updateRecovery(player);
    }

    public void spendOther(Player player, double kcal, String action) {
        if (active(player) && kcal > 0) {
            double before = reserve.calories();
            otherSpent += reserve.spend(kcal);
            EnergyLog.record(player, action, "", -kcal, before, reserve.calories(), 1, action.equals("hunger_effect"));
            updateRecovery(player);
        }
    }

    public void consume(Player player, double kcal, String food) {
        if (active(player)) {
            double before = reserve.calories();
            eaten += reserve.eat(kcal);
            EnergyLog.record(player, "eat", food, kcal, before, reserve.calories(), 1, false);
            updateRecovery(player);
        }
    }

    public void resetCounters() {
        broken = placed = 0;
        walkDistance = sprintDistance = laborSpent = travelSpent = otherSpent = eaten = 0;
    }

    public void resizeForConfig(Player player) {
        double beforeResize = reserve.calories();
        reserve.resize(CalorieConfig.CAPACITY.get());
        if (beforeResize != reserve.calories()) EnergyLog.record(player, "capacity_clamp", "config", reserve.calories() - beforeResize,
                beforeResize, reserve.calories(), 1, false);
        updateRecovery(player);
    }

    @Override public void tick(Player player) {
        resizeForConfig(player);
        if (active(player)) {
            if (isDepleted() || reserve.calories() < CalorieConfig.SPRINT_MINIMUM.get()) player.setSprinting(false);
            var hunger = player.getEffect(MobEffects.HUNGER);
            if (hunger != null) spendOther(player, CalorieConfig.HUNGER.get() * (hunger.getAmplifier() + 1.0) / 20.0, "hunger_effect");
            double healCost = CalorieConfig.HEAL.get();
            if (!isDepleted() && player.isHurt() && reserve.canAfford(healCost)
                    && player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
                if (++healingTicks >= 80) {
                    float before = player.getHealth();
                    player.heal(1);
                    spendOther(player, Math.max(0, player.getHealth() - before) * healCost, "natural_heal");
                    healingTicks = 0;
                }
            } else healingTicks = 0;
            if (reserve.calories() <= 0 && player.level().getDifficulty() != Difficulty.PEACEFUL) {
                if (++starvingTicks >= 80) {
                    float damage = RecoveryRules.starvationDamage(player.getHealth(), CalorieConfig.STARVATION_FLOOR.get().floatValue());
                    if (damage > 0) {
                        player.hurt(player.damageSources().starve(), damage);
                        EnergyLog.marker(player, "starvation_damage_attempt");
                    }
                    starvingTicks = 0;
                }
            } else starvingTicks = 0;
        } else { healingTicks = starvingTicks = 0; }
        if (player instanceof ServerPlayer serverPlayer && ++syncTicks >= 5) {
            syncTicks = 0;
            sync(serverPlayer, false);
        }
    }

    public void sync(ServerPlayer player, boolean force) {
        updateRecovery(player);
        double kcal = Math.floor(reserve.calories() * 10) / 10;
        double minimum = CalorieConfig.SPRINT_MINIMUM.get();
        if (force || kcal != lastSent || reserve.capacity() != lastCapacity || minimum != lastSprintMinimum
                || depleted != lastDepleted || recoveryThreshold() != lastRecovery) {
            PacketDistributor.sendToPlayer(player, new CaloriePayload(kcal, reserve.capacity(), minimum, depleted, recoveryThreshold()));
            lastSent = kcal;
            lastCapacity = reserve.capacity();
            lastSprintMinimum = minimum;
            lastDepleted = depleted;
            lastRecovery = recoveryThreshold();
        }
    }

    public double clientSprintMinimum = 100;
    public double clientRecoveryThreshold = 200;
    public void receive(double kcal, double capacity, double minimum, boolean depleted, double recovery) {
        reserve.resize(capacity);
        reserve.set(kcal);
        clientSprintMinimum = minimum;
        this.depleted = depleted;
        clientRecoveryThreshold = recovery;
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        reserve.resize(CalorieConfig.CAPACITY.get());
        // Old hunger and saturation are discarded once on migration, rather than retaining hidden stores.
        reserve.set(tag.contains("civilizationKcal", 99) ? tag.getDouble("civilizationKcal") : reserve.capacity());
        depleted = RecoveryRules.depleted(tag.getBoolean("civilizationDepleted"), reserve.calories(), recoveryThreshold());
        var stats = tag.getCompound("civilizationCalorieStats");
        broken = stats.getLong("broken"); placed = stats.getLong("placed");
        walkDistance = stats.getDouble("walk"); sprintDistance = stats.getDouble("sprint");
        laborSpent = stats.getDouble("labor"); travelSpent = stats.getDouble("travel");
        otherSpent = stats.getDouble("other"); eaten = stats.getDouble("eaten");
        lastSent = -1;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("civilizationKcal", reserve.calories());
        tag.putBoolean("civilizationDepleted", isDepleted());
        var stats = new CompoundTag();
        stats.putLong("broken", broken); stats.putLong("placed", placed);
        stats.putDouble("walk", walkDistance); stats.putDouble("sprint", sprintDistance);
        stats.putDouble("labor", laborSpent); stats.putDouble("travel", travelSpent);
        stats.putDouble("other", otherSpent); stats.putDouble("eaten", eaten);
        tag.put("civilizationCalorieStats", stats);
        // Compatibility projection only. These values are never read as energy by this mod.
        tag.putInt("foodLevel", getFoodLevel());
        tag.putFloat("foodSaturationLevel", 0);
        tag.putFloat("foodExhaustionLevel", 0);
        tag.putInt("foodTickTimer", 0);
    }

    @Override public int getFoodLevel() { return (int) Math.ceil(20 * reserve.calories() / reserve.capacity()); }
    @Override public int getLastFoodLevel() { return getFoodLevel(); }
    @Override public boolean needsFood() { return reserve.needsFood(); }
    @Override public float getSaturationLevel() { return 0; }
    @Override public float getExhaustionLevel() { return 0; }
    @Override public void setFoodLevel(int value) {} // Vanilla health packets and Peaceful refill cannot mint calories.
    @Override public void setSaturation(float value) {}
    @Override public void setExhaustion(float value) {}
    @Override public void addExhaustion(float value) {}
    @Override public void eat(int nutrition, float saturation) {} // Also disables the saturation status effect.
    @Override public void eat(FoodProperties food) {} // Real foods are handled with their item identity.
}

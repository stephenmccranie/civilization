package dev.civilization;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Baseline on ordinary ground at 20 TPS; vanilla friction and acceleration still apply. */
public final class PlayerMovement {
    public static final double WALK = 3.2;
    public static final double SPRINT = 4.6;
    // Forward input is damped by 0.98; ordinary ground retains 0.6 * 0.91 velocity.
    public static final float WALK_ATTRIBUTE = (float) (WALK * (1 - 0.6 * 0.91) / (20 * 0.98));
    public static final double SPRINT_BONUS = SPRINT / WALK - 1;
    // Match airborne forward acceleration to each gait, rather than vanilla's faster gait.
    public static final double AIR_CONTROL_RATIO = (1 - .91) / (1 - .6 * .91);
    // Calibrated for a 20% flat-ground sprint-jump bonus at the current sprint speed.
    public static final double JUMP_BOOST_SCALE = .686;
    // Creative and spectator flight at twice vanilla's 0.05; the server sends it to the client with the other abilities.
    public static final float FLYING_SPEED = .1f;

    public static void apply(Player player) {
        player.getAbilities().setWalkingSpeed(WALK_ATTRIBUTE);
        player.getAbilities().setFlyingSpeed(FLYING_SPEED);
        player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WALK_ATTRIBUTE);
    }
}

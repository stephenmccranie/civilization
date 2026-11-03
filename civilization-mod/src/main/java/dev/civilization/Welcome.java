package dev.civilization;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** The bare minimum to get started, sent to every joining player as plain server chat so any client shows it. */
@EventBusSubscriber(modid = "civilization")
public final class Welcome {
    static final String[] STEPS = {
        "Building blocks ride in your offhand, one stack of up to 32 at a time.",
        "Furnaces and campfires don't work here; your first machine is the Kiln.",
        "Craft a Stone Saw (3 cobblestone, 2 sticks). Put it in a crafting grid with a block to cut slabs, then smaller pieces.",
        "Craft a Kiln Controller from 9 cobblestone and place it. Look at it to see the blocks it needs, then right-click it holding them.",
        "Open the Kiln, add Coal and click the striker until it lights. Keep it out of the rain.",
        "The Kiln fires clay blocks into bricks, plus glass and stone. Bricks build the Foundry, which smelts ores.",
        "Until you can cook, eat raw food, or forage: crouch and right-click the ground with empty hands for 10 seconds.",
        "Press R over any item to see how it's made.",
    };

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.sendSystemMessage(Component.literal("Welcome to Civilization! To get started:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 0; i < STEPS.length; i++)
            player.sendSystemMessage(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GOLD).append(Component.literal(STEPS[i]).withStyle(ChatFormatting.WHITE)));
    }
}

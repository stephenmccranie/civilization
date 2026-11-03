package dev.civilization;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The bare minimum to get started, sent to every joining player as plain server chat so any client shows it.
 * It stays within the ten lines closed chat shows, each short enough not to wrap at the pack's GUI scale.
 */
@EventBusSubscriber(modid = "civilization")
public final class Welcome {
    static final List<Component> MESSAGE = List.of(
            Component.empty().append(rule()).append(Component.literal(" Welcome to Civilization ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(rule()),
            item("• ", "Blocks go in your *offhand*. No furnaces or campfires here."),
            item("1", "*Stone Saw*: 3 cobblestone + 2 sticks. It cuts blocks."),
            item("2", "*Kiln*: craft the controller from 9 cobblestone, place it."),
            item("3", "Right-click it holding the blocks and pieces it shows."),
            item("4", "Add *Coal* and click the striker until lit. Keep it dry."),
            item("5", "Fire clay into *bricks* to build the *Foundry* for ore."),
            item("• ", "Hungry? Raw food works. Press *R* on items for recipes."),
            Component.literal("Feedback or bugs? Just type it in chat. We read it all.").withStyle(ChatFormatting.GREEN));

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MESSAGE.forEach(player::sendSystemMessage);
    }

    private static Component rule() { return Component.literal(" ".repeat(12)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH); }
    /** One marked line; words between asterisks are highlighted. */
    private static Component item(String marker, String text) {
        var line = Component.literal(" " + marker + "  ").withStyle(ChatFormatting.GOLD);
        var parts = text.split("\\*");
        for (int i = 0; i < parts.length; i++) line.append(Component.literal(parts[i]).withStyle(i % 2 == 1 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
        return line;
    }
}

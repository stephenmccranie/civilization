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
 * It stays within the twenty lines the pack's closed chat shows, each short enough not to wrap at the pack's chat width.
 */
@EventBusSubscriber(modid = "civilization")
public final class Welcome {
    static final List<Component> MESSAGE = List.of(
            Component.empty().append(rule()).append(Component.literal(" Welcome to Civilization ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(rule()),
            item("• ", "Blocks and buckets ride in your *offhand*, "+BlockCarry.LIMIT+" at a time."),
            item("• ", "Your food bar counts *calories*; work and travel spend them."),
            item("• ", "Furnaces don't work. Start with a *Kiln*."),
            heading("First steps"),
            item("1", "*Stone Saw*: 3 cobblestone + 2 sticks; cuts blocks to size."),
            item("2", "*Kiln*: craft the controller from 9 cobblestone; place it."),
            item("3", "Look at it to see its parts; right-click it holding them."),
            item("4", "Add *Coal*, then click the striker; keep rain off it."),
            item("5", "Fire *clay blocks* into bricks, sand into glass."),
            item("6", "*Foundry*: 8 brick blocks around cobblestone; smelts ore."),
            heading("Good to know"),
            item("• ", "Roast food on a *campfire*; a *Cooking Stove* cooks it better."),
            item("• ", "Starving? Crouch empty-handed and right-click the ground."),
            item("• ", "Iron tools come from the *Smithy* and *Assembly Bench*."),
            item("• ", "Claim land with a *Land Controller*; it burns Coal hourly."),
            item("• ", "Press *R* on any item for its recipe, *U* for its uses."),
            Component.literal("Feedback, bugs or ideas? Just type them in chat. We read it all.").withStyle(ChatFormatting.GREEN));

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MESSAGE.forEach(player::sendSystemMessage);
    }

    private static Component rule() { return Component.literal(" ".repeat(16)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH); }
    private static Component heading(String text) { return Component.literal(text).withStyle(ChatFormatting.GOLD); }
    /** One marked line; words between asterisks are highlighted. */
    private static Component item(String marker, String text) {
        var line = Component.literal(" " + marker + "  ").withStyle(ChatFormatting.GOLD);
        var parts = text.split("\\*");
        for (int i = 0; i < parts.length; i++) line.append(Component.literal(parts[i]).withStyle(i % 2 == 1 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
        return line;
    }
}

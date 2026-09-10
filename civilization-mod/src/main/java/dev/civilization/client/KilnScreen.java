package dev.civilization.client;

import dev.civilization.KilnMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Native furnace layout without a recipe book advertising unrelated furnace recipes. */
public final class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    public KilnScreen(KilnMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.isLit()) {
            int h = (int)Math.ceil(menu.getLitProgress() * 13) + 1;
            graphics.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"),
                    14, 14, 0, 14 - h, leftPos + 56, topPos + 50 - h, 14, h);
        }
        int w = (int)Math.ceil(menu.getBurnProgress() * 24);
        if (w > 0) graphics.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/burn_progress"),
                24, 16, 0, 0, leftPos + 79, topPos + 34, w, 16);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float delta) {
        super.render(graphics, x, y, delta);
        boolean incomplete = menu.requiresStructure() && menu.structureStatus() != 1;
        graphics.drawString(font, Component.translatable(incomplete ? "gui.civilization.structure_" + menu.structureStatus()
                        : "gui.civilization.operating_" + menu.operatingStatus()), leftPos, topPos - 12,
                !incomplete && (menu.operatingStatus() == 1 || menu.operatingStatus() == 6) ? 0xFF99DD88 : 0xFFFFBB77, true);
        renderTooltip(graphics, x, y);
        if (menu.getSlot(0).getItem().isEmpty() && isHovering(56, 17, 16, 16, x, y))
            graphics.renderTooltip(font, Component.translatable("tooltip.civilization." + menu.tooltipPrefix() + "_input"), x, y);
        if (menu.getSlot(1).getItem().isEmpty() && isHovering(56, 53, 16, 16, x, y))
            graphics.renderTooltip(font, Component.translatable("tooltip.civilization." + menu.tooltipPrefix() + "_fuel"), x, y);
    }
}

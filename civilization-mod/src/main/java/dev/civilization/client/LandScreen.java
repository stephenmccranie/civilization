package dev.civilization.client;

import dev.civilization.ClaimTier;
import dev.civilization.LandMenu;
import dev.civilization.LandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A native single-chest layout, with labeled controls outside the inventory instead of item icons. */
public final class LandScreen extends AbstractContainerScreen<LandMenu> {
    private final CivicWidget[] tiers = new CivicWidget[3], names = new CivicWidget[5];
    private CivicWidget access, buy, collect, add, remove, previous, next;
    private EditBox username;
    private boolean editing;
    private int page;
    public LandScreen(LandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 168; inventoryLabelY = 74;
    }
    private CivicWidget button(String text, int x, int y, int width, Runnable action) {
        return addRenderableWidget(new CivicWidget(text, x, y, width, 20, action));
    }
    private void action(int id) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void init() {
        super.init();
        int right = leftPos + imageWidth + 6;
        for (int i = 0; i < tiers.length; i++) {
            final int tier = i;
            tiers[i] = addRenderableWidget(new CivicWidget(new String[]{"I", "II", "III"}[i], leftPos - 25, topPos + 8 + i * 30, 22, 22, () -> action(tier)));
        }
        access = addRenderableWidget(new CivicWidget("Whitelist", leftPos + imageWidth + 3, topPos + 8, 22, 22, () -> { editing = !editing; page = 0; updateButtons(); }));
        buy = addRenderableWidget(new CivicWidget("Buy land", leftPos + imageWidth + 3, topPos + 38, 22, 22, () -> action(10)));
        buy.glyph = "buy";
        collect = addRenderableWidget(new CivicWidget("Collect coal", leftPos + imageWidth + 3, topPos + 68, 22, 22, () -> action(11)));
        collect.glyph = "collect";
        collect.setTooltip(Tooltip.create(Component.literal("Collect sale payment, refund, or unfinished purchase deposit.")));
        username = addRenderableWidget(new EditBox(font, right, topPos + 35, 94, 16, Component.literal("Username")));
        username.setMaxLength(16); username.setHint(Component.literal("Username"));
        add = button("Add", right, topPos + 55, 44, () -> access(false));
        remove = button("Remove", right + 48, topPos + 55, 46, () -> access(true));
        for (int i = 0; i < names.length; i++) {
            final int row = i;
            names[i] = addRenderableWidget(new CivicWidget("", right, topPos + 79 + i * 13, 94, 12, () -> {
                int index = page * names.length + row;
                if (index < menu.state.whitelist().size()) username.setValue(menu.state.whitelist().get(index));
            }));
        }
        previous = button("<", right, topPos + 145, 44, () -> { page = Math.max(0, page - 1); updateButtons(); });
        next = button(">", right + 48, topPos + 145, 46, () -> { page++; updateButtons(); });
        updateButtons();
    }
    private void access(boolean remove) {
        if (!username.getValue().isBlank()) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new LandPayload.Access(menu.containerId, username.getValue(), remove));
    }
    private void updateButtons() {
        var s = menu.state;
        for (int i = 0; i < tiers.length; i++) {
            var t = ClaimTier.values()[i]; tiers[i].selected = s.tier() == i;
            tiers[i].active = (s.flags() & 8) != 0 && !tiers[i].selected;
            tiers[i].setTooltip(Tooltip.create(Component.literal("Tier " + (i+1) + ": " + t.width + "×" + t.width + ", 64 tall\nUpkeep: " + t.upkeep + " coal/hour\nSwitch: " + (t.upkeep * dev.civilization.CivicConfig.TIER_SWITCH_HOURS) + " coal from reserve\n24-hour cooldown\n" + s.tierStatus())));
        }
        access.active = (s.flags() & 4) != 0;
        if (!access.active) editing = false;
        access.setMessage(Component.literal(editing ? "Done" : "Whitelist"));
        access.glyph = editing ? "" : "access";
        access.setX(leftPos + imageWidth + (editing ? 6 : 3)); access.setWidth(editing ? 94 : 22);
        access.setTooltip(Tooltip.create(Component.literal("Whitelist · Owner: " + s.owner())));
        buy.visible = collect.visible = !editing;
        buy.setTooltip(Tooltip.create(Component.literal(s.buyLabel() + "\nBuy this land; owner has one week to move."))); buy.active = (s.flags() & 16) != 0;
        username.visible = add.visible = remove.visible = editing;
        page = Math.min(page, Math.max(0, (s.whitelist().size() - 1) / names.length));
        for (int i = 0; i < names.length; i++) {
            int index = page * names.length + i; names[i].visible = editing && index < s.whitelist().size();
            if (names[i].visible) names[i].setMessage(Component.literal(font.plainSubstrByWidth(s.whitelist().get(index), 80)));
        }
        previous.visible = next.visible = editing && s.whitelist().size() > names.length;
        previous.active = page > 0; next.active = (page + 1) * names.length < s.whitelist().size();
    }
    @Override protected void containerTick() { updateButtons(); }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (editing && username.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) { access(false); return true; }
            if (username.keyPressed(key, scan, mods) || username.canConsumeInput()) return true;
        }
        return super.keyPressed(key, scan, mods);
    }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        int footer = menu.state.notice().isEmpty() ? 0 : font.split(Component.literal(menu.state.notice()), imageWidth-12).size()*9+12;
        return x < left-28 || x >= left+imageWidth+(editing?106:28) || y < top-48 || y >= top+imageHeight+footer;
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);}
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        CivicWidget.panel(g, leftPos, topPos - 48, imageWidth, 54);
        for(int i=0;i<3;i++) CivicWidget.panel(g,leftPos-28,topPos+5+i*30,31,28);
        if(editing) CivicWidget.panel(g,leftPos+imageWidth-3,topPos+2,109,168);
        else for(int i=0;i<3;i++) CivicWidget.panel(g,leftPos+imageWidth-3,topPos+5+i*30,31,28);
        MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);
        MachineUi.slots(g,menu,leftPos,topPos);
        MachineUi.title(g,title.getString(),leftPos,topPos,imageWidth);
        if (!menu.state.notice().isEmpty()) {
            int height = font.split(Component.literal(menu.state.notice()), imageWidth-12).size()*9+12;
            CivicWidget.panel(g,leftPos,topPos+imageHeight-3,imageWidth,height);
        }
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        g.drawString(font, menu.state.status(), leftPos + 7, topPos - 39, 0xFF303030, false);
        g.drawString(font, menu.state.details(), leftPos + 7, topPos - 28, 0xFF404040, false);
        g.drawString(font, menu.state.tierStatus(), leftPos + 7, topPos - 17, 0xFF555555, false);
        if (!menu.state.notice().isEmpty()) g.drawWordWrap(font, Component.literal(menu.state.notice()), leftPos + 6, topPos + imageHeight + 3, imageWidth-12, 0xFF303030);
        renderTooltip(g, x, y);
        if (isHovering(8, 18, 162, 54, x, y) && (hoveredSlot == null || !hoveredSlot.hasItem()))
            g.renderTooltip(font, Component.literal((menu.state.flags() & 1) != 0 ? "Coal — drag or shift-click to supply" : "Stored coal — withdrawals restricted"), x, y);
    }
}

package io.hamlook.aetheria.features.capes.ui;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.repo.RepoLinks;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.UrlOpener;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A minimal cape gallery with a focused player preview page. */
public class CapeSelectorGUI extends AetheriaBaseScreen {

    private static final int GALLERY = 0;
    private static final int PREVIEW = 1;
    private static final int MAX_W = 640;
    private static final int MAX_H = 420;
    private static final int CARD_H = 164;
    private static final int GAP = 8;
    private static final int SUGGEST_H = 84;

    private final List<CapeDisplay> capes = new ArrayList<>();
    private final ThemeContext.HitList hits = new ThemeContext.HitList();
    private final LerpUtils.LerpingFloat opening = new LerpUtils.LerpingFloat(0f, 180);

    private int page = GALLERY;
    private int boxX, boxY, boxW, boxH, contentX, contentY, contentW, contentH;
    private int columns, cardW, maxScroll;
    private float scrollY, scrollTarget;
    private int pressedX, pressedY;
    private boolean modelDrag, dragged;
    private CapeDisplay pressedCape;
    private CapeDisplay selectedCape;

    @Override
    protected void onInitGui() {
        layout();
        capes.clear();
        CapeManager.capes.values().stream()
                .filter(cape -> cape != null && cape.id != null)
                .sorted(Comparator.comparing(cape -> cape.name == null ? cape.id : cape.name,
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(cape -> capes.add(new CapeDisplay(cape)));
    }

    private void layout() {
        boxW = Math.min(MAX_W, Math.max(0, width - 28));
        boxH = Math.min(MAX_H, Math.max(0, height - 28));
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        contentX = boxX + 16;
        contentY = boxY + 58;
        contentW = boxW - 32;
        contentH = Math.max(0, boxH - 100);
        columns = contentW >= 540 ? 3 : contentW >= 360 ? 2 : 1;
        cardW = Math.max(1, (contentW - (columns - 1) * GAP) / columns);
        maxScroll = getMaxScroll();
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
    }

    private int getMaxScroll() {
        int rows = (capes.size() + Math.max(1, columns) - 1) / Math.max(1, columns);
        int totalHeight = rows == 0 ? 0 : rows * (CARD_H + GAP) + SUGGEST_H;
        return Math.max(0, totalHeight - Math.max(0, contentH));
    }

    @Override
    protected void guiClosed() {
        KeyboardCompat.enableRepeatEvents(false);
    }

    @Override
    protected void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        //#if MC < 1.21
        drawDefaultBackground();
        //#else
        //$$ renderBackground(DrawContextUtils.getDrawContext(), mouseX, mouseY, partialTicks);
        //#endif

        layout();
        hits.clear(mouseX, mouseY);
        opening.setTarget(1f);
        opening.tick();
        float fade = LerpUtils.sigmoidZeroOne(opening.getValue());
        ThemeContext.panel(boxX, boxY, boxW, boxH, fade);
        ThemeContext.rect(boxX, boxY, boxW, 2, ThemeContext.withAlpha(ThemeContext.ACCENT, fade));

        if (page == PREVIEW) drawPreview(partialTicks);
        else drawGallery(mouseX, mouseY, partialTicks);
    }

    private void drawHeader(FontRenderer font, String title, String subtitle) {
        ThemeContext.text(font, subtitle, boxX + ThemeContext.SIDE_PAD, boxY + 25, ThemeContext.MUTED);

        String player = localPlayerName();
        Cape equipped = player.isEmpty() ? null : CapeManager.getCapeForPlayer(player);
        String status = equipped == null ? "No cape equipped" : "Wearing: " + safeName(equipped);
        status = ThemeContext.fit(font, status, Math.max(80, boxW / 3));
        String fittedTitle = ThemeContext.fit(font, title,
                boxW - ThemeContext.SIDE_PAD * 2 - ThemeContext.width(font, status) - 20);
        ThemeContext.text(font, fittedTitle, boxX + ThemeContext.SIDE_PAD, boxY + 10, ThemeContext.TEXT);
        ThemeContext.textRight(font, status, boxX + boxW - ThemeContext.SIDE_PAD, boxY + 14,
                equipped == null ? ThemeContext.MUTED : ThemeContext.ACCENT);
        ThemeContext.divider(boxX + ThemeContext.SIDE_PAD, boxY + 42, boxW - ThemeContext.SIDE_PAD * 2);
    }

    private void drawGallery(int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        drawHeader(font, "Capes", capes.size() + " in your collection · click to preview");

        ThemeContext.panel(contentX, contentY, contentW, contentH);
        if (capes.isEmpty()) {
            ThemeContext.textCenter(font, "No capes are available yet.",
                    contentX + contentW / 2, contentY + contentH / 2 - 4, ThemeContext.MUTED);
            drawSuggestion(font, contentY + contentH - SUGGEST_H - 10);
        } else {
            drawGrid(font, mouseX, mouseY, partialTicks);
        }
        drawScrollbar();
        drawFooter(font, "Auto-spin previews · click to inspect");
    }

    private void drawGrid(FontRenderer font, int mouseX, int mouseY, float partialTicks) {
        maxScroll = getMaxScroll();
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
        scrollY += (scrollTarget - scrollY) * 0.28f;
        if (Math.abs(scrollTarget - scrollY) < 0.3f) scrollY = scrollTarget;

        int scale = GuiScreenUtils.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GlStateManagerCompat.scissor(contentX * scale,
                GuiScreenUtils.getDisplayHeight() - (contentY + contentH) * scale,
                contentW * scale, contentH * scale);
        hits.clip(contentX, contentY, contentW, contentH);
        try {
            String player = localPlayerName();
            String equippedId = currentCapeId(player);
            for (int i = 0; i < capes.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = contentX + col * (cardW + GAP);
                int y = contentY + row * (CARD_H + GAP) - (int) scrollY;
                if (y + CARD_H <= contentY || y >= contentY + contentH) continue;
                boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= contentY && mouseY < contentY + contentH
                        && mouseY >= y && mouseY < y + CARD_H;
                CapeDisplay display = capes.get(i);
                boolean equipped = display.capeID.equals(equippedId);
                Cape cape = CapeManager.getCape(display.capeID);
                Runnable cardAction = null;
                if (!player.isEmpty() && cape != null && cape.isLoaded()) {
                    cardAction = equipped ? () -> CapeManager.removeCape(player)
                            : () -> CapeManager.equipCape(player, cape);
                }
                display.draw(x, y, cardW, CARD_H, hover, equipped, player, partialTicks, font,
                        hits, cardAction);
            }
            int rows = (capes.size() + columns - 1) / columns;
            int suggestionY = contentY + rows * (CARD_H + GAP) - (int) scrollY;
            if (suggestionY < contentY + contentH && suggestionY + SUGGEST_H > contentY) {
                drawSuggestion(font, suggestionY);
            }
        } finally {
            hits.unclip();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawSuggestion(FontRenderer font, int y) {
        int x = contentX + 8;
        int w = Math.max(1, contentW - 16);
        ThemeContext.panel(x, y, w, SUGGEST_H);
        ThemeContext.textCenter(font, "Couldn't find what you were looking for?",
                x + w / 2, y + 10, ThemeContext.TEXT);
        ThemeContext.textCenter(font, "Suggest a Cape in our Discord",
                x + w / 2, y + 25, ThemeContext.ACCENT);
        int buttonW = ThemeContext.buttonWidth(font, "Open Discord");
        ThemeContext.button(hits, font, "Open Discord", x + (w - buttonW) / 2,
                y + 48, buttonW, ThemeContext.FIELD_H, true, false,
                () -> UrlOpener.open(RepoLinks.discord()), null);
    }

    private void drawPreview(float partialTicks) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        CapeDisplay display = selectedCape;
        Cape cape = display == null ? null : CapeManager.getCape(display.capeID);
        String name = cape == null ? "Cape preview" : safeName(cape);
        drawHeader(font, name, "Preview before equipping");

        int areaY = boxY + 68;
        int areaH = Math.max(100, boxH - 108);
        int previewW = (boxW - 48) * 58 / 100;
        int detailsX = boxX + 32 + previewW;
        int detailsW = boxW - (detailsX - boxX) - 24;
        ThemeContext.rect(boxX + 16, areaY, previewW, areaH, 0xFF262626);
        ThemeContext.border(boxX + 16, areaY, previewW, areaH, ThemeContext.LINE);
        ThemeContext.panel(detailsX, areaY, detailsW, areaH);

        if (cape == null) {
            ThemeContext.textCenter(font, "This cape is no longer available.", boxX + boxW / 2,
                    areaY + areaH / 2, ThemeContext.MUTED);
        } else {
            String player = localPlayerName();
            int modelBaseY = areaY + areaH - 28;
            if (!player.isEmpty() && cape.isLoaded()) {
                int modelScale = Math.max(32, Math.min(118, areaH * 2 / 5));
                display.drawModel(player, cape, boxX + 16, areaY, previewW, areaH,
                        modelBaseY, modelScale, partialTicks, true);
            } else {
                ThemeContext.textCenter(font, "Join a world to preview your model.",
                        boxX + 16 + previewW / 2, areaY + areaH / 2, ThemeContext.MUTED);
            }

            drawDetails(font, cape, display, detailsX, areaY, detailsW, areaH, player);
            String rotationTip = display.isManuallyRotated() ? "Manual angle" : "Auto spin · drag to turn";
            ThemeContext.textCenter(font, rotationTip, boxX + 16 + previewW / 2,
                    areaY + areaH - 16, ThemeContext.MUTED);
        }
        drawFooter(font, "Preview only · drag to rotate", true);
    }

    private void drawDetails(FontRenderer font, Cape cape, CapeDisplay display,
                             int x, int y, int w, int h, String player) {
        int pad = 16;
        String equippedId = currentCapeId(player);
        boolean equipped = cape.id.equals(equippedId);
        ThemeContext.text(font, "Cape", x + pad, y + 16, ThemeContext.ACCENT);
        ThemeContext.text(font, ThemeContext.fit(font, safeName(cape), w - pad * 2),
                x + pad, y + 38, ThemeContext.TEXT);
        ThemeContext.text(font, "ID  " + ThemeContext.fit(font, cape.id, w - pad * 2 - 20),
                x + pad, y + 57, ThemeContext.MUTED);
        ThemeContext.divider(x + pad, y + 76, w - pad * 2);
        ThemeContext.text(font, equipped ? "Currently worn" : cape.isLoaded() ? "Ready to wear" : "Loading texture",
                x + pad, y + 92, equipped ? ThemeContext.ACCENT : ThemeContext.MUTED);

        GlStateManagerCompat.resetColor();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        if (!player.isEmpty() && cape.isLoaded()) {
            if (equipped) {
                int removeW = w - pad * 2;
                ThemeContext.button(hits, font, "Remove cape", x + pad, y + h - 38,
                        removeW, ThemeContext.FIELD_H, false, true,
                        () -> CapeManager.removeCape(player), null);
            } else {
                ThemeContext.button(hits, font, "Equip cape", x + pad, y + h - 38,
                        w - pad * 2, ThemeContext.FIELD_H, true, false,
                        () -> CapeManager.equipCape(player, cape), null);
            }
        } else {
            ThemeContext.buttonDisabled(font, player.isEmpty() ? "Join a world to equip" : "Waiting for texture",
                    x + pad, y + h - 38, w - pad * 2, ThemeContext.FIELD_H);
        }

        if (display.isManuallyRotated()) {
            ThemeContext.button(hits, font, "Resume spin", x + pad, y + h - 60,
                    w - pad * 2, ThemeContext.BTN_H, false, false,
                    display::resumeAutoRotation, null);
        }
    }

    private void drawScrollbar() {
        if (maxScroll <= 0 || contentH <= 0) return;
        int x = contentX + contentW - 5;
        ThemeContext.rect(x, contentY + 5, 2, contentH - 10, ThemeContext.LINE);
        int thumbH = Math.max(24, (contentH - 10) * contentH / (contentH + maxScroll));
        int thumbY = contentY + 5 + Math.round((contentH - 10 - thumbH) * (scrollY / maxScroll));
        ThemeContext.rect(x, thumbY, 2, thumbH, ThemeContext.ACCENT);
    }

    private void drawFooter(FontRenderer font, String left) {
        drawFooter(font, left, false);
    }

    private void drawFooter(FontRenderer font, String left, boolean showBack) {
        int closeW = ThemeContext.buttonWidth(font, "Close");
        int buttonY = boxY + boxH - ThemeContext.FOOT_H + 2;
        int buttonX = boxX + boxW - ThemeContext.SIDE_PAD - closeW;
        ThemeContext.divider(boxX + ThemeContext.SIDE_PAD, buttonY - 6, boxW - ThemeContext.SIDE_PAD * 2);
        ThemeContext.text(font, left, boxX + ThemeContext.SIDE_PAD, buttonY + 4, ThemeContext.MUTED);
        if (showBack) {
            int backW = ThemeContext.buttonWidth(font, "Back to capes");
            ThemeContext.button(hits, font, "Back to capes", buttonX - 6 - backW, buttonY,
                    backW, ThemeContext.BTN_H, false, false, () -> page = GALLERY, null);
        }
        ThemeContext.button(hits, font, "Close", buttonX, buttonY,
                closeW, ThemeContext.BTN_H, false, false, () -> MinecraftCompat.displayGuiScreen(null), null);
    }

    private String localPlayerName() {
        return MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getLocalWorld() == null ? ""
                : MinecraftCompat.getLocalPlayer().getGameProfile().getName();
    }

    private String currentCapeId(String player) {
        Cape cape = player.isEmpty() ? null : CapeManager.getCapeForPlayer(player);
        return cape == null ? null : cape.id;
    }

    private String safeName(Cape cape) {
        return cape.name == null || cape.name.trim().isEmpty() ? cape.id : cape.name;
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        if (hits.click(mouseX, mouseY)) return;

        if (page == PREVIEW && selectedCape != null && selectedCape.containsModel(mouseX, mouseY)) {
            modelDrag = true;
            dragged = false;
            pressedX = mouseX;
            pressedY = mouseY;
            selectedCape.beginRotationDrag(mouseX);
            return;
        }

        if (page != GALLERY) return;
        CapeDisplay display = capeAt(mouseX, mouseY);
        if (display == null) return;
        pressedCape = display;
        pressedX = mouseX;
        pressedY = mouseY;
        dragged = false;
        modelDrag = false;
    }

    @Override
    protected void onMouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (button != 0 || page != PREVIEW || !modelDrag) return;
        if (Math.abs(mouseX - pressedX) + Math.abs(mouseY - pressedY) > 4) dragged = true;
        if (dragged && modelDrag) {
            CapeDisplay active = page == PREVIEW ? selectedCape : pressedCape;
            if (active != null) active.dragRotation(mouseX);
        }
    }

    @Override
    protected void onMouseReleased(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        CapeDisplay released = page == PREVIEW ? selectedCape : pressedCape;
        if (modelDrag && released != null) released.endRotationDrag();
        modelDrag = false;
        if (page == GALLERY && pressedCape != null && !dragged
                && pressedCape.contains(mouseX, mouseY)) {
            selectedCape = pressedCape;
            page = PREVIEW;
        }
        pressedCape = null;
    }

    private CapeDisplay capeAt(int mouseX, int mouseY) {
        if (mouseX < contentX || mouseX >= contentX + contentW
                || mouseY < contentY || mouseY >= contentY + contentH) return null;
        for (CapeDisplay cape : capes) {
            if (cape.contains(mouseX, mouseY)) return cape;
        }
        return null;
    }

    private void handleWheel(double amount, int mouseX, int mouseY) {
        if (page != GALLERY || mouseX < contentX || mouseX > contentX + contentW
                || mouseY < contentY || mouseY > contentY + contentH) return;
        scrollTarget = Math.max(0f, Math.min((float) maxScroll,
                scrollTarget - (float) Math.signum(amount) * 44f));
    }

    //#if MC < 1.16
    @Override
    protected void onHandleMouseInput() {
        int wheel = MouseCompat.getEventDWheel();
        if (wheel != 0) handleWheel(wheel, GuiScreenUtils.getMouseX(), GuiScreenUtils.getMouseY());
    }
    //#else
    //$$ @Override
    //$$ protected void onMouseScrolled(int mouseX, int mouseY, double verticalAmount) {
    //$$     handleWheel(verticalAmount, mouseX, mouseY);
    //$$ }
    //#endif

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        if (!KeybindHelper.isEscapeKey(keyCode)) return;
        if (page == PREVIEW) {
            page = GALLERY;
            return;
        }
        MinecraftCompat.displayGuiScreen(null);
    }
}

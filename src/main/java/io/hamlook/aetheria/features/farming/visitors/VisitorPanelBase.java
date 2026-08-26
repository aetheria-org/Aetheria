package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.debug.GLDebugProbe;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import lombok.Getter;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public abstract class VisitorPanelBase {

    private static final int PAD = 5;
    private static final int TEXT_ROW_H = 12;
    private static final int SEP_H = 5;
    private static final int ICON_SIZE = 14;
    private static final int ICON_GAP = 3;
    private static final int ITEM_ROW_H = ICON_SIZE + 3;

    private final List<Clickable> clickTargets = new ArrayList<>();
    private int mouseX = -1;
    private int mouseY = -1;

    /** Last drawn on-screen rect (scaled), for the position editor size suppliers */
    @Getter
    private int lastX = 0;
    @Getter
    private int lastY = 0;
    @Getter
    private int lastWidth = 120;
    @Getter
    private int lastHeight = 80;
    @Getter
    private long lastDrawnAtMs = 0L;

    /** Why the panel last refused to render (diagnostics via /asmdebug visitors) */
    @Getter
    private String blockReason = "";

    private static boolean tipShownThisLaunch = false;

    protected abstract boolean panelEnabled();

    protected abstract List<VisitorLine> lines();

    protected boolean blocked(String reason) {
        this.blockReason = reason;
        return false;
    }

    protected void clearBlocked() {
        this.blockReason = "";
    }

    @SubscribeEvent
    public void onContainerRender(GuiContainerRenderBeforeTooltipEvent event) {
        render(event.gui, event.mouseX, event.mouseY, true);
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiEditSign)) return;
        render(event.gui, event.mouseX, event.mouseY, false);
    }

    private void render(GuiScreen gui, int mx, int my, boolean undoContainerTranslate) {
        boolean visible = panelEnabled();
        if (!visible) return;
        List<VisitorLine> ls = lines();
        if (ls.isEmpty()) {
            blockReason = "no lines to draw";
            return;
        }
        blockReason = "";
        mouseX = mx;
        mouseY = my;
        drawPanel(ls, gui, undoContainerTranslate, true);
        maybeShowTip(ls);
    }

    public void renderPreview() {
        List<VisitorLine> ls = VisitorShoppingList.buildMainLines(true);
        if (ls.isEmpty()) return;
        mouseX = -1;
        mouseY = -1;
        drawPanel(ls, Minecraft.getMinecraft().currentScreen, false, false);
    }

    // GL diagnostics via the shared fail-safe probe (see utils/debug/GLDebugProbe).
    // Gated by Debug -> Enable Debug; zero output by default.
    private boolean glDebugEnabled = false;

    private void glProbe(String point, boolean live) {
        if (!live || !glDebugEnabled) return;
        Aetheria.logger.info("[PANEL-GL] " + point + " " + GLDebugProbe.state());
    }

    private void drawPanel(List<VisitorLine> ls, GuiScreen gui, boolean undoContainerTranslate, boolean live) {
        VisitorsConfig.PanelConfig pc = panelConfig();
        float s = pc == null ? 1f : Math.max(0.1f, pc.scale);

        glDebugEnabled = live && ATHRConfig.feature != null
                && ATHRConfig.feature.debug.enableDebug
                && GLDebugProbe.throttle("VisitorPanel", 1000L);

        Minecraft mc = Minecraft.getMinecraft();
        int w = PAD * 2;
        int h = PAD * 2;
        int[] rowHeights = new int[ls.size()];
        for (int i = 0; i < ls.size(); i++) {
            rowHeights[i] = rowHeight(ls.get(i));
            w = Math.max(w, PAD * 2 + lineWidth(ls.get(i)));
            h += rowHeights[i];
        }
        float scaledW = w * s;
        float scaledH = h * s;

        Position pos = pc != null ? pc.panelPos : new Position(-350, 120, false, false);
        ScaledResolution sr = new ScaledResolution(mc);
        int x = pos.getAbsX(sr, Math.round(scaledW));
        int y = pos.getAbsY(sr, Math.round(scaledH));
        if (pos.isCenterX()) x -= Math.round(scaledW / 2f);
        if (pos.isCenterY()) y -= Math.round(scaledH / 2f);

        lastX = x;
        lastY = y;
        lastWidth = Math.round(scaledW);
        lastHeight = Math.round(scaledH);
        lastDrawnAtMs = System.currentTimeMillis();

        boolean compensate = undoContainerTranslate && gui instanceof GuiContainer;
        glProbe("ENTER", live);
        if (compensate) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-((GuiContainer) gui).guiLeft, -((GuiContainer) gui).guiTop, 50);
        }
        try {
            GlStateManager.pushMatrix();
            // Force an unlit white state: other container features leave the
            // lighting mode enabled, which shades the panel text darker.
            GlStateManager.disableLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(s, s, 1);

            glProbe("PRE_BG", live);
            clickTargets.clear();
            drawPanelBackground(0, 0, w, h);
            // The nine-slice helper disables blending on exit; restore the
            // standard alpha blend or all following text/rects render unblended
            // (darker text, broken hover highlights).
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            glProbe("POST_BG", live);
            GlStateManager.color(1f, 1f, 1f, 1f);
            int dy = PAD;
            boolean iconProbed = false;
            for (int i = 0; i < ls.size(); i++) {
                VisitorLine line = ls.get(i);
                int rowH = rowHeights[i];
                Clickable clickable = null;
                float lmx = (mouseX - x) / s;
                float lmy = (mouseY - y) / s;
                boolean hovered = lmx >= 2 && lmx <= w - 2 && lmy >= dy && lmy <= dy + rowH;
                boolean clickableRow = line.kind == VisitorLine.Kind.ITEM;

                if (line.kind == VisitorLine.Kind.SEPARATOR) {
                    Gui.drawRect(PAD, dy + 1, w - PAD, dy + 2, 0x40FFFFFF);
                } else {
                    // Highlight only where clicking actually does something
                    if (hovered && clickableRow) Gui.drawRect(2, dy, w - 2, dy + rowH, 0x28FFFFFF);
                    int tx = PAD;
                    if (clickableRow) {
                        // renderItemIcon manages its own lighting/GL state; the
                        // outer pair here used to re-pollute blend+lighting
                        // after each icon, darkening the row text.
                        if (line.icon != null) {
                            ItemRenderUtils.renderItemIcon(mc, line.icon, tx, dy + ((ITEM_ROW_H - ICON_SIZE) / 2), ICON_SIZE);
                            GlStateManager.disableLighting();
                            GlStateManager.color(1f, 1f, 1f, 1f);
                            GlStateManager.enableBlend();
                            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                            if (!iconProbed) {
                                glProbe("POST_ICON", live);
                                iconProbed = true;
                            }
                        }
                        tx += ICON_SIZE + ICON_GAP;
                        clickable = new Clickable(
                                Math.round(x + 2 * s), Math.round(y + dy * s),
                                Math.round((w - 4) * s), Math.round(rowH * s),
                                line.itemId, line.amount);
                    }
                    if (line.text != null && !line.text.isEmpty()) {
                        mc.fontRendererObj.drawStringWithShadow(line.text, tx, dy + 2, 0xFFFFFF);
                    }
                }
                if (clickable != null) clickTargets.add(clickable);
                dy += rowH;
            }
            glProbe("POST_ROWS", live);
        } finally {
            GlStateManager.popMatrix();
            if (compensate) GlStateManager.popMatrix();
        }
    }

    /**
     * One-time-per-launch chat tip teaching that panel rows open the Bazaar
     * Permanently dismissible via /visitortip hide ([HIDE] button in the message)
     */
    private static void maybeShowTip(List<VisitorLine> ls) {
        if (tipShownThisLaunch) return;
        if (ls.stream().noneMatch(l -> l.kind == VisitorLine.Kind.ITEM)) return;
        VisitorsConfig cfg = ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.visitors;
        if (cfg == null || cfg.shoppingListTipHidden) return;

        tipShownThisLaunch = true;
        io.hamlook.aetheria.utils.SoundUtils.playSound("note.pling");
        ChatComponentText line1 = new ChatComponentText("§e[ASM] §7Tip: click an item in the shopping list to open it on the Bazaar.");
        ChatComponentText line2 = new ChatComponentText("§7The order amount is filled for you automatically. ");
        ChatComponentText hide = new ChatComponentText("§a[HIDE]");
        hide.getChatStyle()
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Never show this tip again")))
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/visitortip hide"));
        line2.appendSibling(hide);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(line1);
            mc.thePlayer.addChatMessage(line2);
        }
    }

    public static boolean isOverPanel(int mx, int my) {
        return isRectOverPanel(mx, my, 0, 0);
    }

    /** True when the rect intersects a panel that is currently visible */
    public static boolean isRectOverPanel(int x, int y, int w, int h) {
        VisitorPanel panel = VisitorPanel.getInstance();
        if (panel == null) return false;
        if (!panel.panelEnabled()) return false;
        if (System.currentTimeMillis() - panel.getLastDrawnAtMs() > 250L) return false;
        int px = panel.getLastX();
        int py = panel.getLastY();
        return x < px + panel.getLastWidth() && x + w > px
                && y < py + panel.getLastHeight() && y + h > py;
    }

    protected static VisitorsConfig.PanelConfig panelConfig() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.visitors.panel;
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (clickTargets.isEmpty()) return;
        boolean visible = panelEnabled();
        if (!visible) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) return;
        int mx = Mouse.getEventX() * mc.currentScreen.width / mc.displayWidth;
        int my = mc.currentScreen.height - Mouse.getEventY() * mc.currentScreen.height / mc.displayHeight - 1;
        for (Clickable clickable : clickTargets) {
            if (clickable.contains(mx, my)) {
                event.setCanceled(true);
                VisitorShoppingList.onRowClick(clickable.itemId, clickable.amount);
                return;
            }
        }
    }

    private static int rowHeight(VisitorLine line) {
        switch (line.kind) {
            case SEPARATOR:
                return SEP_H;
            case ITEM:
                return ITEM_ROW_H;
            default:
                return TEXT_ROW_H;
        }
    }

    private static int lineWidth(VisitorLine line) {
        String text = line.text == null ? "" : line.text;
        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        if (line.kind == VisitorLine.Kind.ITEM) textWidth += ICON_SIZE + ICON_GAP;
        return textWidth;
    }

    public static void drawPanelBackground(int x, int y, int w, int h) {
        ResourceLocation texture = BetterContainers.isEnabled()
                ? Resources.betterContainerNineSlice(ATHRConfig.feature.qol.betterContainers.style)
                : Resources.betterContainerNineSlice(3);
        NineSliceUtils.draw(texture, x, y, w, h, 6, 18);
    }

    private static final class Clickable {
        final int x;
        final int y;
        final int w;
        final int h;
        final String itemId;
        final int amount;

        Clickable(int x, int y, int w, int h, String itemId, int amount) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.itemId = itemId;
            this.amount = amount;
        }

        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}

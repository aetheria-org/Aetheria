package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public abstract class Overlay {

    protected static final int LINE_HEIGHT = 10;
    protected static final int PADDING = 3;
    protected static final int ICON_SIZE = 8;
    protected static final int ICON_GAP = 2;

    protected static final Minecraft mc = MinecraftCompat.getMinecraft();
    protected static int cachedWidth;
    protected static int cachedHeight;

    protected int lastW;
    protected int lastH;

    protected Overlay(int defaultW, int defaultH) {
        this.lastW = defaultW;
        this.lastH = defaultH;
    }

    protected static final int DEFAULT_SUPERSAMPLE = 4;

    public static void drawRoundedRect(int x, int y, int w, int h, int r, int color) {
        drawRoundedRect(x, y, w, h, r, color, DEFAULT_SUPERSAMPLE);
    }

    public static void drawRoundedRect(int x, int y, int w, int h, int r, int color, int supersample) {
        int ss = Math.max(1, supersample);
        withSupersample(ss, () -> drawRoundedRectRaw(x * ss, y * ss, w * ss, h * ss, r * ss, color));
    }

    private static void drawRoundedRectRaw(int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w - x, h - y) / 2);
        beginMesh();
        try {
            if (r <= 0) {
                emitQuad(x, y, w, h, color);
            } else {
                emitQuad(x + r, y, w - r, h, color);
                emitQuad(x, y + r, x + r, h - r, color);
                emitQuad(w - r, y + r, w, h - r, color);

                for (int i = 0; i < r; i++) {
                    int cut = cornerCut(r, i);
                    emitQuad(x + i, y + cut, x + i + 1, y + r, color);
                    emitQuad(w - i - 1, y + cut, w - i, y + r, color);
                    emitQuad(x + i, h - r, x + i + 1, h - cut, color);
                    emitQuad(w - i - 1, h - r, w - i, h - cut, color);
                }
            }
        } finally {
            endMesh();
        }
    }

    public static void drawRoundedRectBorder(int x, int y, int w, int h, int r, int t, int color) {
        drawRoundedRectBorder(x, y, w, h, r, t, color, DEFAULT_SUPERSAMPLE);
    }

    public static void drawRoundedRectBorder(int x, int y, int w, int h, int r, int t, int color, int supersample) {
        int ss = Math.max(1, supersample);
        withSupersample(ss, () -> drawRoundedRectBorderRaw(x * ss, y * ss, w * ss, h * ss, r * ss, t * ss, color));
    }

    private static void drawRoundedRectBorderRaw(int x, int y, int w, int h, int r, int t, int color) {
        if (t <= 0) return;
        r = Math.min(r, Math.min(w - x, h - y) / 2);
        int width = w - x;
        int height = h - y;

        beginMesh();
        try {
            if (width - 2 * r > 0) {
                emitQuad(x + r, y, w - r, y + t, color);
                emitQuad(x + r, h - t, w - r, h, color);
            }
            if (height - 2 * r > 0) {
                emitQuad(x, y + r, x + t, h - r, color);
                emitQuad(w - t, y + r, w, h - r, color);
            }
            if (r > 0) {
                int ri = Math.max(r - t, 0);
                int innerW = width - 2 * t;
                for (int i = 0; i < r; i++) {
                    int co = cornerCut(r, i);
                    int cl = x + i;
                    int cr = x + width - 1 - i;
                    if (i >= t && innerW > 0) {
                        int ci = cornerCut(ri, i - t);
                        emitQuad(cl, y + co, cl + 1, y + t + ci, color);
                        emitQuad(cl, h - t - ci, cl + 1, h - co, color);
                        emitQuad(cr, y + co, cr + 1, y + t + ci, color);
                        emitQuad(cr, h - t - ci, cr + 1, h - co, color);
                    } else {
                        emitQuad(cl, y + co, cl + 1, h - co, color);
                        emitQuad(cr, y + co, cr + 1, h - co, color);
                    }
                }
            }
        } finally {
            endMesh();
        }
    }

    public static void drawRoundedRectBorderFlow(int x, int y, int w, int h, int r, int t, ChromaStyle style) {
        drawRoundedRectBorderFlow(x, y, w, h, r, t, style, DEFAULT_SUPERSAMPLE);
    }

    public static void drawRoundedRectBorderFlow(int x, int y, int w, int h, int r, int t, ChromaStyle style, int supersample) {
        int base = style.toArgb();
        int mode = style.getMode();
        float size = style.getSize();
        int ss = Math.max(1, supersample);
        withSupersample(ss, () -> drawRoundedRing(x * ss, y * ss, w * ss, h * ss, r * ss, t * ss,
                columnX -> ChromaColour.applyChromaShift(base, columnX / (float) ss, 0, mode, size)));
    }

    public static void drawRoundedRectFlow(int x, int y, int w, int h, int r, ChromaStyle style) {
        drawRoundedRectFlow(x, y, w, h, r, style, DEFAULT_SUPERSAMPLE);
    }

    public static void drawRoundedRectFlow(int x, int y, int w, int h, int r, ChromaStyle style, int supersample) {
        int base = style.toArgb();
        int mode = style.getMode();
        float size = style.getSize();
        int ss = Math.max(1, supersample);
        withSupersample(ss, () -> {
            int sx = x * ss, sy = y * ss, sw = w * ss, sh = h * ss;
            int sr = Math.min(r * ss, Math.min(sw - sx, sh - sy) / 2);
            int width = sw - sx;
            beginMesh();
            try {
                for (int i = 0; i < width; i++) {
                    int top = sy + Math.max(cornerCut(sr, i), cornerCut(sr, width - 1 - i));
                    int bot = sh - Math.max(cornerCut(sr, i), cornerCut(sr, width - 1 - i));
                    if (top < bot) {
                        emitQuad(sx + i, top, sx + i + 1, bot, ChromaColour.applyChromaShift(base, (sx + i) / (float) ss, 0, mode, size));
                    }
                }
            } finally {
                endMesh();
            }
        });
    }

    private static void withSupersample(int supersample, Runnable draw) {
        if (supersample <= 1) {
            draw.run();
            return;
        }
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!blendWasEnabled) {
            GlStateManagerCompat.enableBlend();
            GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        }
        GlStateManagerCompat.pushMatrix();
        float inv = 1f / supersample;
        GlStateManagerCompat.scale(inv, inv, 1f);
        try {
            draw.run();
        } finally {
            GlStateManagerCompat.popMatrix();
            if (!blendWasEnabled) GlStateManagerCompat.disableBlend();
        }
    }

    private static void drawRoundedRing(int x, int y, int w, int h, int r, int t, IntUnaryOperator colorForColumn) {
        if (t <= 0) return;
        r = Math.min(r, Math.min(w - x, h - y) / 2);
        int width = w - x;
        beginMesh();
        try {
            if (r <= 0) {
                for (int i = 0; i < width; i++) {
                    int color = colorForColumn.applyAsInt(x + i);
                    if (i < t || i >= width - t) {
                        emitQuad(x + i, y, x + i + 1, h, color);
                    } else {
                        emitQuad(x + i, y, x + i + 1, y + t, color);
                        emitQuad(x + i, h - t, x + i + 1, h, color);
                    }
                }
                return;
            }
            int ri = Math.max(r - t, 0);
            int innerW = width - 2 * t;
            for (int i = 0; i < r; i++) {
                int co = cornerCut(r, i);
                int cl = x + i;
                int cr = x + width - 1 - i;
                int colorL = colorForColumn.applyAsInt(cl);
                int colorR = colorForColumn.applyAsInt(cr);
                if (i >= t && innerW > 0) {
                    int ci = cornerCut(ri, i - t);
                    emitQuad(cl, y + co, cl + 1, y + t + ci, colorL);
                    emitQuad(cl, h - t - ci, cl + 1, h - co, colorL);
                    emitQuad(cr, y + co, cr + 1, y + t + ci, colorR);
                    emitQuad(cr, h - t - ci, cr + 1, h - co, colorR);
                } else {
                    emitQuad(cl, y + co, cl + 1, h - co, colorL);
                    emitQuad(cr, y + co, cr + 1, h - co, colorR);
                }
            }
            for (int i = r; i < width - r; i++) {
                int color = colorForColumn.applyAsInt(x + i);
                emitQuad(x + i, y, x + i + 1, y + t, color);
                emitQuad(x + i, h - t, x + i + 1, h, color);
            }
        } finally {
            endMesh();
        }
    }

    private static int cornerCut(int r, int d) {
        if (r <= 0 || d >= r) return 0;
        return (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - (double) (r - d - 1) * (r - d - 1))));
    }

    private static VertexBuilder meshBuilder;
    private static boolean blendWasEnabled;
    private static int meshQuads;

    private static void beginMesh() {
        //#if MC < 1.16
        //#else
        //$$ DrawContextUtils.flushIfPresent();
        meshBuilder = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
        meshQuads = 0;
        blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        //#endif
    }

    private static void endMesh() {
        //#if MC < 1.16
        //#else
        if (meshQuads > 0) meshBuilder.draw();
        meshBuilder = null;
        if (!blendWasEnabled) GlStateManagerCompat.disableBlend();
        //#endif
    }

    private static void emitQuad(int x1, int y1, int x2, int y2, int color) {
        //#if MC < 1.16
        DrawContextUtils.fill(x1, y1, x2, y2, color);
        //#else
        if (x2 <= x1 || y2 <= y1) return;
        int a = (color >>> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        meshBuilder.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        meshBuilder.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        meshBuilder.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        meshBuilder.pos(x2, y1, 0).color(r, g, b, a).endVertex();
        meshQuads++;
        //#endif
    }

    public int getOverlayWidth() {
        return lastW;
    }

    public int getOverlayHeight() {
        return lastH;
    }

    public abstract List<String> getLines(boolean preview);

    public abstract Position getPosition();

    public abstract float getScale();

    public abstract int getBgColor();

    public abstract int getCornerRadius();

    protected abstract boolean isEnabled();

    protected boolean extraGuard() {
        return true;
    }

    private final Map<String, ItemStack> lineIcons = new HashMap<>();

    protected final void clearLineIcons() {
        lineIcons.clear();
    }

    protected final void putLineIcon(String line, ItemStack icon) {
        if (icon != null) lineIcons.put(line, icon);
    }

    protected ItemStack getLineIcon(String line) {
        return lineIcons.get(line);
    }

    protected int getIconSize() {
        return ICON_SIZE;
    }

    protected void drawLine(String line, int x, int y) {
        DrawContextUtils.drawTextWithShadow(MinecraftCompat.getFontRenderer(), line, x, y, 0xFFFFFF);
    }

    @HandleEvent
    public final void onRenderOverlay(ASMRenderOverlayEvent event) {
        if (event.type != 0) return;
        cachedWidth = event.scaledWidth;
        cachedHeight = event.scaledHeight;
        if (!isLiveActive()) return;
        render(false);
    }

    public boolean isLiveActive() {
        if (ATHRConfig.feature == null || !isEnabled() || !extraGuard()) return false;
        if (applyOverlayHideGate()) {
            boolean shouldHide = (hideOnChat() && OverlayUtils.isChatOpen()) || (hideOnTab() && OverlayUtils.isTabHeld()) || (hideOnDebug() && OverlayUtils.isDebugActive()) || OverlayUtils.isStorageActive();
            if (shouldHide) return false;
        }
        return true;
    }

    protected boolean applyOverlayHideGate() {
        return true;
    }

    protected boolean hideOnChat() {
        return true;
    }

    protected boolean hideOnTab() {
        return true;
    }

    protected boolean hideOnDebug() {
        return true;
    }

    /**
     * Renders the overlay at its configured {@link Position}.
     * <p>
     * Convention: {@code getPosition().getAbsX/getAbsY} return the box's top-left corner.
     * For centered positions they return the screen center, so half the scaled size is subtracted.
     * Overrides that draw their own box must use the same convention as {@code GuiPositionEditor},
     * or the preview drifts from the drag box.
     *
     * @param preview true when rendered inside the position editor
     */
    public void render(boolean preview) {
        if (preview && isLiveActive()) return;
        if (!preview && !extraGuard()) return;

        List<String> lines = getLines(preview);
        if (lines == null || lines.isEmpty()) return;

        float scale = getScale();

        int w = getBaseWidth();
        for (String line : lines)
            w = Math.max(w, DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), line)
                    + (getLineIcon(line) != null ? getIconSize() + ICON_GAP : 0) + PADDING * 2);
        int h = lines.size() * LINE_HEIGHT + PADDING * 2;
        lastW = w;
        lastH = h;

        Position pos = getPosition();
        int x = pos.getAbsX(cachedWidth, (int) (w * scale));
        int y = pos.getAbsY(cachedHeight, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(x, y, 0);
        GlStateManagerCompat.scale(scale, scale, 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) drawRoundedRect(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), bgColor);

        int dy = 0;
        for (String line : lines) {
            int tx = 0;
            ItemStack icon = getLineIcon(line);
            if (icon != null) {
                ItemRenderUtils.renderItemIcon(mc, icon, 0, dy - 1, getIconSize());
                tx = getIconSize() + ICON_GAP;
            }
            drawLine(line, tx, dy);
            dy += LINE_HEIGHT;
        }

        GlStateManagerCompat.popMatrix();
    }

    protected int getBaseWidth() {
        return 20;
    }
}

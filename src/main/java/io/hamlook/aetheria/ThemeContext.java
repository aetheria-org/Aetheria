package io.hamlook.aetheria;

import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.LerpUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.DoubleConsumer;

public final class ThemeContext {

    public static final int DIM = 0xCC000000;
    public static final int PANEL = 0xFF202020;
    public static final int INSET = 0xFF171717;
    public static final int LINE = 0xFF2D2D2D;
    public static final int EDGE = 0xFF101010;
    public static final int BTN = 0xFF2D2D2D;
    public static final int BTN_BORDER = 0xFF3F3F3F;
    public static final int BTN_HOVER = 0xFF383838;
    public static final int ACCENT = 0xFF02AEAE;
    public static final int ACCENT_HOVER = 0xFF19C2C2;
    public static final int ACCENT_ON = 0xFF031A1A;
    public static final int SUCCESS = 0xFF55FF55;
    public static final int DANGER = 0xFFFF5555;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFF8D8D8D;
    public static final int OVERLAY = 0x66000000;

    public static final int PAD = 8;
    public static final int SIDE_PAD = 12;
    public static final int HEADER_H = 26;
    public static final int ACTIVE_H = 44;
    public static final int TAB_H = 22;
    public static final int TOOLBAR_H = 28;
    public static final int ROW_H = 34;
    public static final int TABLE_H = 22;
    public static final int FOOT_H = 22;
    public static final int BTN_H = 16;
    public static final int SMALL_H = 14;
    public static final int FIELD_H = 24;
    public static final int LINE_H = 10;

    private ThemeContext() {
    }

    public static int width(FontRenderer fr, String text) {
        return DrawContextUtils.getStringWidth(fr, text == null ? "" : text);
    }

    public static void text(FontRenderer fr, String s, int x, int y, int color) {
        DrawContextUtils.drawText(fr, s == null ? "" : s, x, y, color, false);
    }

    public static void textCenter(FontRenderer fr, String s, int cx, int y, int color) {
        DrawContextUtils.drawText(fr, s == null ? "" : s, cx - width(fr, s) / 2, y, color, false);
    }

    public static void textRight(FontRenderer fr, String s, int right, int y, int color) {
        DrawContextUtils.drawText(fr, s == null ? "" : s, right - width(fr, s), y, color, false);
    }

    public static String fit(FontRenderer fr, String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0) return "";
        if (width(fr, text) <= maxWidth) return text;
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width(fr, text.substring(0, mid) + "...") <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return lo == 0 ? "" : text.substring(0, lo) + "...";
    }

    public static void rect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        DrawContextUtils.fill(x, y, x + w, y + h, color);
    }

    public static void border(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        rect(x, y, w, 1, color);
        rect(x, y + h - 1, w, 1, color);
        rect(x, y, 1, h, color);
        rect(x + w - 1, y, 1, h, color);
    }

    /** Draws the shared raised surface used by cards and grouped controls. */
    public static void panel(int x, int y, int w, int h) {
        rect(x, y, w, h, PANEL);
        border(x, y, w, h, LINE);
    }

    public static void panel(int x, int y, int w, int h, float alpha) {
        rect(x, y, w, h, withAlpha(PANEL, alpha));
        border(x, y, w, h, withAlpha(LINE, alpha));
    }

    /** Draws the inset surface used for lists, text fields and preview areas. */
    public static void inset(int x, int y, int w, int h) {
        rect(x, y, w, h, INSET);
        border(x, y, w, h, LINE);
    }

    public static void inset(int x, int y, int w, int h, float alpha) {
        rect(x, y, w, h, withAlpha(INSET, alpha));
        border(x, y, w, h, withAlpha(LINE, alpha));
    }

    public static void divider(int x, int y, int w) {
        rect(x, y, w, 1, LINE);
    }

    public static int argbOf(String chroma) {
        try {
            return ChromaColour.specialToChromaRGB(chroma);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xff) * LerpUtils.clampZeroOne(alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int mixArgb(int from, int to, float t) {
        t = LerpUtils.clampZeroOne(t);
        int r = interpolate(from, to, t, 16);
        int g = interpolate(from, to, t, 8);
        int b = interpolate(from, to, t, 0);
        int a = interpolate(from, to, t, 24);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int interpolate(int from, int to, float t, int shift) {
        int a = (from >>> shift) & 0xff;
        int b = (to >>> shift) & 0xff;
        return Math.round(a + (b - a) * t);
    }

    public static int buttonWidth(FontRenderer fr, String label) {
        return Math.max(28, width(fr, label) + 12);
    }

    public static int button(HitList hits, FontRenderer fr, String label, int x, int y, int w, int h, boolean primary, boolean danger, Runnable action, String tooltip) {
        boolean hover = hits.hover(x, y, w, h);
        buttonVisual(fr, label, x, y, w, h, primary, danger, hover);
        hits.add(x, y, w, h, tooltip, action);
        return w;
    }

    /** Draws a themed button when the owning UI handles its own input. */
    public static void buttonVisual(FontRenderer fr, String label, int x, int y, int w, int h,
                                    boolean primary, boolean danger, boolean hover) {
        if (primary) {
            rect(x, y, w, h, hover ? ACCENT_HOVER : ACCENT);
            border(x, y, w, h, ACCENT_HOVER);
        } else {
            rect(x, y, w, h, hover ? BTN_HOVER : BTN);
            border(x, y, w, h, BTN_BORDER);
        }
        int color = primary ? ACCENT_ON : danger ? DANGER : TEXT;
        textCenter(fr, fit(fr, label, w - 8), x + w / 2, y + (h - 8) / 2, color);
    }

    public static void buttonDisabled(FontRenderer fr, String label, int x, int y, int w, int h) {
        rect(x, y, w, h, INSET);
        border(x, y, w, h, LINE);
        textCenter(fr, label, x + w / 2, y + (h - 8) / 2, MUTED);
    }

    public static void toggle(HitList hits, int x, int y, float frac, Runnable action, String tooltip) {
        int w = 30, h = 16;
        int trackOn = mixArgb(INSET, ACCENT, 0.35f);
        rect(x, y, w, h, mixArgb(INSET, trackOn, frac));
        border(x, y, w, h, LINE);
        int knobX = x + 2 + Math.round(LerpUtils.clampZeroOne(frac) * (w - 2 - 12 - 2));
        rect(knobX, y + 3, 12, 10, mixArgb(MUTED, ACCENT, frac));
        hits.add(x, y, w, h, tooltip, action);
    }

    public static void slider(int x, int y, int w, double value, double min, double max) {
        int trackY = y + 8;
        double frac = max <= min ? 0 : (value - min) / (max - min);
        frac = Math.max(0, Math.min(1, frac));
        rect(x, trackY, w, 4, LINE);
        rect(x, trackY, (int) Math.round(w * frac), 4, ACCENT);
        int knobX = x + (int) Math.round(w * frac) - 2;
        rect(Math.max(x - 2, Math.min(x + w - 2, knobX)), trackY - 3, 4, 10, TEXT);
    }

    public static final class SliderDrag {
        private final int x, w;
        private final double min, max, step;
        private final DoubleConsumer apply;

        public SliderDrag(int x, int w, double min, double max, double step, DoubleConsumer apply) {
            this.x = x;
            this.w = w;
            this.min = min;
            this.max = max;
            this.step = step;
            this.apply = apply;
        }

        public void applyAt(int mouseX) {
            double frac = w <= 0 ? 0 : (mouseX - x) / (double) w;
            frac = Math.max(0, Math.min(1, frac));
            double v = min + frac * (max - min);
            v = Math.round(v / step) * step;
            v = Math.max(min, Math.min(max, v));
            apply.accept(v);
        }
    }

    public static String valueLabel(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    public static final class Hit {
        public final int x, y, w, h;
        public final String tooltip;
        public final Runnable action;

        Hit(int x, int y, int w, int h, String tooltip, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.tooltip = tooltip;
            this.action = action;
        }

        public boolean inside(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public static final class HitList {
        private final List<Hit> hits = new ArrayList<>();
        private final Deque<int[]> clips = new ArrayDeque<>();
        private int cx0, cy0, cx1, cy1;
        private int mouseX = -1, mouseY = -1;

        public void clear(int mouseX, int mouseY) {
            hits.clear();
            clips.clear();
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            cx0 = 0;
            cy0 = 0;
            cx1 = Integer.MAX_VALUE / 2;
            cy1 = Integer.MAX_VALUE / 2;
        }

        public int mouseX() {
            return mouseX;
        }

        public int mouseY() {
            return mouseY;
        }

        public void setMouse(int mouseX, int mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public void clip(int x, int y, int w, int h) {
            clips.push(new int[]{cx0, cy0, cx1, cy1});
            cx0 = Math.max(cx0, x);
            cy0 = Math.max(cy0, y);
            cx1 = Math.min(cx1, x + w);
            cy1 = Math.min(cy1, y + h);
        }

        public void unclip() {
            if (clips.isEmpty()) return;
            int[] c = clips.pop();
            cx0 = c[0];
            cy0 = c[1];
            cx1 = c[2];
            cy1 = c[3];
        }

        public void add(int x, int y, int w, int h, String tooltip, Runnable action) {
            int x0 = Math.max(x, cx0);
            int y0 = Math.max(y, cy0);
            int x1 = Math.min(x + w, cx1);
            int y1 = Math.min(y + h, cy1);
            if (x1 <= x0 || y1 <= y0) return;
            hits.add(new Hit(x0, y0, x1 - x0, y1 - y0, tooltip, action));
        }

        public boolean hover(int x, int y, int w, int h) {
            if (mouseX < 0) return false;
            if (mouseX < cx0 || mouseX >= cx1 || mouseY < cy0 || mouseY >= cy1) return false;
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }

        public Hit topAt(int mx, int my) {
            if (mx < cx0 || mx >= cx1 || my < cy0 || my >= cy1) return null;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.inside(mx, my)) return hit;
            }
            return null;
        }

        public boolean click(int mx, int my) {
            Hit hit = topAt(mx, my);
            if (hit == null) return false;
            if (hit.action != null) hit.action.run();
            return true;
        }
    }
}

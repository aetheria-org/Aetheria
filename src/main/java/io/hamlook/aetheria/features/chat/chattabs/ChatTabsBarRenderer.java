package io.hamlook.aetheria.features.chat.chattabs;

import io.hamlook.aetheria.core.features.chat.ChatTabsConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.hooks.ChatTabsBarHook;
import net.minecraft.client.gui.FontRenderer;

import java.util.EnumMap;
import java.util.Map;

public final class ChatTabsBarRenderer {

    static final int TEXT_Y_OFFSET = 2;
    static final int TEXT_IDLE = 0xA0A0A0;
    static final int TEXT_HOVER = 0xFFFFA0;
    static final int TEXT_ACTIVE = 0xFFFFFF;

    private static final int HELD_RAISE = 3;
    private static final int HELD_BG = 0xCC141414;
    private static final int HELD_OUTLINE = 0x99FFFFFF;
    private static final int HELD_SHADOW_NEAR = 0x40000000;
    private static final int HELD_SHADOW_FAR = 0x30000000;
    private static final int GHOST_OUTLINE = 0x99FFFFFF;
    private static final int PREVIEW_UNDERLINE_ALPHA = 0x80000000;
    private static final int SLIDE_TIME_MS = 120;

    private static final Map<ChatTabType, LerpUtils.LerpingFloat> slide = new EnumMap<>(ChatTabType.class);

    private ChatTabsBarRenderer() {
    }

    public static void draw(int mouseX, int mouseY, ChatTabsBar.Layout baseLayout) {
        if (!ChatTabsBar.isVisible() && !ChatTabsBarHook.dragActive()) return;
        ChatTabsConfig cfg = ChatTabsBar.config();
        if (cfg == null) return;

        int screenWidth = GuiScreenUtils.getScaledWindowWidth();
        int screenHeight = GuiScreenUtils.getScaledWindowHeight();
        ChatTabType held = ChatTabsBarHook.draggedTab();
        ChatTabsBar.Layout layout = held != null
                ? ChatTabsBar.dragLayout(screenWidth, screenHeight, held, mouseX)
                : baseLayout;
        FontRenderer fr = MinecraftCompat.getFontRenderer();

        int bgColor = ChromaColour.specialToChromaRGB(cfg.barBgColor);
        int hoverBgColor = ChromaColour.specialToChromaRGB(cfg.barHoverBgColor);
        int activeColor = ChromaColour.specialToChromaRGB(cfg.barActiveColor);
        int badgeArgb = cfg.unreadBadge == null ? 0 : ChromaColour.specialToChromaRGB(cfg.unreadBadge.badgeColor);
        ChatTabType activeTab = ChatTabs.activeTab();
        ChatTabType previewTab = ChatTabs.previewTab();
        int y = layout.y;

        for (ChatTabsBar.Entry entry : layout.entries) {
            int drawX = animatedX(entry);
            boolean isActive = entry.tab == activeTab;
            boolean isPreview = !isActive && previewTab == entry.tab;
            boolean hovered = held == null && inBox(entry, drawX, y, mouseX, mouseY);

            DrawContextUtils.fill(drawX, y, drawX + entry.w, y + ChatTabsBar.BOX_HEIGHT,
                    hovered || isPreview ? hoverBgColor : bgColor);
            if (isActive || isPreview) {
                int color = isActive ? activeColor : (activeColor & 0x00FFFFFF) | PREVIEW_UNDERLINE_ALPHA;
                DrawContextUtils.fill(drawX, y + ChatTabsBar.UNDERLINE_ROW, drawX + entry.w,
                        y + ChatTabsBar.BOX_HEIGHT, color);
            }

            if (isPreview) {
                drawPreviewText(fr, entry.tab.label, drawX + ChatTabsBar.PAD, y + TEXT_Y_OFFSET,
                        cfg.hoverPreview, cfg.barTextShadow);
            } else {
                int textColor = isActive ? TEXT_ACTIVE : hovered ? TEXT_HOVER : TEXT_IDLE;
                DrawContextUtils.drawText(fr, entry.tab.label, drawX + ChatTabsBar.PAD, y + TEXT_Y_OFFSET,
                        textColor, cfg.barTextShadow);
            }
            drawBadge(fr, entry.tab, drawX, y, badgeArgb, cfg.barTextShadow);
        }

        if (layout.ghostX >= 0) {
            drawDashedOutline(layout.ghostX, y, layout.ghostWidth);
        }

        if (held != null) {
            drawHeld(fr, held, mouseX, screenWidth, y, activeTab, activeColor, cfg.barTextShadow, badgeArgb);
        }

        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHeld(FontRenderer fr, ChatTabType held, int mouseX, int screenWidth,
                                 int y, ChatTabType activeTab, int activeColor, boolean textShadow,
                                 int badgeArgb) {
        int heldWidth = ChatTabsBar.tabWidth(held, fr);
        int heldX = ChatTabsBar.heldTabX(heldWidth, mouseX, screenWidth);
        int hy = y - HELD_RAISE;
        int bottom = hy + ChatTabsBar.BOX_HEIGHT;

        DrawContextUtils.fill(heldX, bottom, heldX + heldWidth, bottom + 1, HELD_SHADOW_NEAR);
        DrawContextUtils.fill(heldX, bottom + 1, heldX + heldWidth, bottom + 2, HELD_SHADOW_FAR);
        DrawContextUtils.fill(heldX, hy, heldX + heldWidth, bottom, HELD_BG);
        if (held == activeTab) {
            DrawContextUtils.fill(heldX, hy + ChatTabsBar.UNDERLINE_ROW, heldX + heldWidth, bottom, activeColor);
        }
        DrawContextUtils.fill(heldX, hy, heldX + heldWidth, hy + 1, HELD_OUTLINE);
        DrawContextUtils.fill(heldX, bottom - 1, heldX + heldWidth, bottom, HELD_OUTLINE);
        DrawContextUtils.fill(heldX, hy + 1, heldX + 1, bottom - 1, HELD_OUTLINE);
        DrawContextUtils.fill(heldX + heldWidth - 1, hy + 1, heldX + heldWidth, bottom - 1, HELD_OUTLINE);

        DrawContextUtils.drawText(fr, held.label, heldX + ChatTabsBar.PAD, hy + TEXT_Y_OFFSET,
                TEXT_ACTIVE, textShadow);
        drawBadge(fr, held, heldX, hy, badgeArgb, textShadow);
    }

    private static void drawBadge(FontRenderer fr, ChatTabType tab, int boxX, int boxY,
                                  int badgeArgb, boolean textShadow) {
        if (tab == ChatTabType.ALL) return;
        String text = ChatTabs.badgeText(tab);
        if (text == null) return;
        int badgeX = boxX + ChatTabsBar.PAD + DrawContextUtils.getStringWidth(fr, tab.label) + 3;
        if (ChatTabs.dotBadges()) {
            DrawContextUtils.fill(badgeX, boxY + 4, badgeX + 4, boxY + 8, badgeArgb);
            return;
        }
        int badgeW = Math.max(DrawContextUtils.getStringWidth(fr, text) + 4, 10);
        DrawContextUtils.fill(badgeX, boxY + 1, badgeX + badgeW, boxY + 10, badgeArgb);
        DrawContextUtils.drawText(fr, text, badgeX + 2, boxY + 2, 0xFFFFFF, textShadow);
    }

    private static void drawDashedOutline(int x, int y, int w) {
        int right = x + w;
        int bottom = y + ChatTabsBar.BOX_HEIGHT;
        int dash = 2;
        int gap = 2;
        for (int col = x; col < right; col += dash + gap) {
            int end = Math.min(col + dash, right);
            DrawContextUtils.fill(col, y, end, y + 1, GHOST_OUTLINE);
            DrawContextUtils.fill(col, bottom - 1, end, bottom, GHOST_OUTLINE);
        }
        for (int row = y; row < bottom; row += dash + gap) {
            int end = Math.min(row + dash, bottom);
            DrawContextUtils.fill(x, row, x + 1, end, GHOST_OUTLINE);
            DrawContextUtils.fill(right - 1, row, right, end, GHOST_OUTLINE);
        }
    }

    private static void drawPreviewText(FontRenderer fr, String text, int x, int y,
                                        ChatTabsConfig.HoverPreview cfg, boolean shadow) {
        boolean animated = ChromaColour.getSpeed(cfg.textColor) != 0;
        ChromaStyle style = animated ? ChromaStyle.of(cfg.textColor, cfg.chromaMode, cfg.chromaSize) : null;
        int staticArgb = animated ? 0 : ChromaColour.specialToSimpleRGB(cfg.textColor);
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int color;
            if (animated) {
                color = style.toArgb(cx, 0);
            } else if (cfg.chromaMode != 0) {
                color = ChromaColour.applyChromaShift(staticArgb, cx, 0, cfg.chromaMode, cfg.chromaSize);
            } else {
                color = staticArgb;
            }
            DrawContextUtils.drawText(fr, String.valueOf(c), cx, y, color, shadow);
            cx += DrawContextUtils.getCharWidth(fr, c);
        }
    }

    private static int animatedX(ChatTabsBar.Entry entry) {
        int target = entry.x;
        LerpUtils.LerpingFloat anim = slide.get(entry.tab);
        if (anim == null) {
            anim = new LerpUtils.LerpingFloat(target, SLIDE_TIME_MS);
            slide.put(entry.tab, anim);
            return target;
        }
        if (anim.getTarget() != target) {
            anim.setTarget(target);
            anim.resetTimer();
        }
        anim.tick();
        return Math.round(anim.getValue());
    }

    private static boolean inBox(ChatTabsBar.Entry entry, int x, int y, int mouseX, int mouseY) {
        return mouseY >= y && mouseY < y + ChatTabsBar.BOX_HEIGHT && mouseX >= x && mouseX < x + entry.w;
    }
}

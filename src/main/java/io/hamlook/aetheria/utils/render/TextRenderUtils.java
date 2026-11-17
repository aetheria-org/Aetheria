package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.features.qol.TooltipHandler;
import io.hamlook.aetheria.utils.compat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public final class TextRenderUtils {

    private static final int DEFAULT_TOOLTIP_Z = 400;

    private TextRenderUtils() {
    }

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        float factor = Math.min(1, len / (float) DrawContextUtils.getStringWidth(fr, str));
        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int colour, float factor) {
        GlStateManagerCompat.scale(factor, factor, 1);
        //#if MC < 1.21
        fr.drawString(str, x / factor, y / factor, colour, shadow);
        //#else
        //$$ DrawContextUtils.drawText(fr, str, x / factor, y / factor, colour, shadow);
        //#endif
        GlStateManagerCompat.scale(1 / factor, 1 / factor, 1);
    }

    public static void drawStringCenteredScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = DrawContextUtils.getStringWidth(fr, str);
        float factor = Math.min(1, len / (float) strLen);
        int newLen = Math.min(strLen, len);
        drawStringScaled(str, fr, x - newLen / 2f, y - 8 * factor / 2f, shadow, colour, factor);
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos, int color, float uiScale, boolean displayScale) {
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, uiScale * scaleDisplay);

        GlStateManagerCompat.translate(xPos, yPos, 0);
        GlStateManagerCompat.scale(finalScale, finalScale, 1f);
        //#if MC < 1.21
        MinecraftCompat.getFontRenderer().drawString(text, 0, 0, color);
        //#else
        //$$ DrawContextUtils.drawText(MinecraftCompat.getFontRenderer(), text, 0f, 0f, color, false);
        //#endif
        GlStateManagerCompat.popMatrix();
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos, float uiScale, boolean displayScale) {
        drawStringScaleAware(text, xPos, yPos, -1, uiScale, displayScale);
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos, float uiScale) {
        drawStringScaleAware(text, xPos, yPos, uiScale, true);
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos, int color, float uiScale, boolean displayScale) {
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, uiScale * scaleDisplay);

        GlStateManagerCompat.translate(xPos, yPos, 0);
        GlStateManagerCompat.scale(finalScale, finalScale, 1f);

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        //#if MC < 1.21
        fr.drawString(text, -DrawContextUtils.getStringWidth(fr, text) / 2f, -fr.FONT_HEIGHT / 2f, color, false);
        //#else
        //$$ DrawContextUtils.drawText(fr, text, -DrawContextUtils.getStringWidth(fr, text) / 2f, -fr.fontHeight / 2f, color, false);
        //#endif
        GlStateManagerCompat.popMatrix();
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos, float uiScale, boolean displayScale) {
        drawCenteredStringScaleAware(text, xPos, yPos, -1, uiScale, displayScale);
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos, float uiScale) {
        drawCenteredStringScaleAware(text, xPos, yPos, uiScale, true);
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, FontRenderer font) {
        drawHoveringText(textLines, mouseX, mouseY, DEFAULT_TOOLTIP_Z, font);
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int zLevel, FontRenderer font) {
        drawHoveringText(textLines, mouseX, mouseY, GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight(), -1, zLevel, font);
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font) {
        drawHoveringText(textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, DEFAULT_TOOLTIP_Z, font);
    }

    private static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, int zLevel, FontRenderer font) {
        if (textLines.isEmpty()) return;

        // DrawContext buffers GUI content until the frame ends, so the raw tooltip
        // background must flush it first or it renders underneath the buffered items.
        //#if MC < 1.21
        //#else
        //$$ DrawContextUtils.flushIfPresent();
        //#endif

        GlStateManagerCompat.disableRescaleNormal();
        RenderHelperCompat.disableStandardItemLighting();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        try {
            int tooltipTextWidth = 0;
            for (String line : textLines) {
                tooltipTextWidth = Math.max(tooltipTextWidth, DrawContextUtils.getStringWidth(font, line));
            }

            boolean needsWrap = false;
            int titleLinesCount = 1;
            int tooltipX = mouseX + 12;

            if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
                tooltipX = mouseX - 16 - tooltipTextWidth;
                if (tooltipX < 4) {
                    tooltipTextWidth = mouseX > screenWidth / 2 ? mouseX - 12 - 8 : screenWidth - 16 - mouseX;
                    needsWrap = true;
                }
            }
            if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
                tooltipTextWidth = maxTextWidth;
                needsWrap = true;
            }

            if (needsWrap) {
                int wrappedWidth = 0;
                List<String> wrapped = new ArrayList<>();
                for (int i = 0; i < textLines.size(); i++) {
                    List<String> wl = DrawContextUtils.wrapFormattedText(font, textLines.get(i), tooltipTextWidth);
                    if (i == 0) titleLinesCount = wl.size();
                    for (String l : wl) {
                        wrappedWidth = Math.max(wrappedWidth, DrawContextUtils.getStringWidth(font, l));
                        wrapped.add(l);
                    }
                }
                tooltipTextWidth = wrappedWidth;
                textLines = wrapped;
                tooltipX = mouseX > screenWidth / 2 ? mouseX - 16 - tooltipTextWidth : mouseX + 12;
            }

            int tooltipY = mouseY - 12;
            int tooltipHeight = 8 + (textLines.size() > 1 ? (textLines.size() - 1) * 10 : 0) + (textLines.size() > titleLinesCount ? 2 : 0);
            if (tooltipY + tooltipHeight + 6 > screenHeight) {
                tooltipY = screenHeight - tooltipHeight - 6;
            }

            TooltipHandler.onTooltipDrawn(tooltipX, tooltipY, tooltipHeight, tooltipTextWidth);
            TooltipHandler.tick();
            TooltipHandler.pushTransform();
            try {
                final int bg = 0xF0100010;
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, bg, bg);
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, bg, bg);
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, bg, bg);
                RenderUtils.drawGradientRect(zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, bg, bg);
                RenderUtils.drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, bg, bg);

                final int borderStart = 0x505000FF;
                final int borderEnd = (borderStart & 0xFEFEFE) >> 1 | borderStart & 0xFF000000;
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 2, tooltipY + tooltipHeight + 2, borderStart, borderEnd);
                RenderUtils.drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 2, borderStart, borderEnd);
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 2, borderStart, borderStart);
                RenderUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderEnd, borderEnd);

                GlStateManagerCompat.pushMatrix();
                try {
                    GlStateManagerCompat.translate(0, 0, zLevel);
                    for (int i = 0; i < textLines.size(); i++) {
                        DrawContextUtils.drawTextWithShadow(font, textLines.get(i), tooltipX, tooltipY, -1);
                        if (i + 1 == titleLinesCount) tooltipY += 2;
                        tooltipY += 10;
                    }
                } finally {
                    GlStateManagerCompat.popMatrix();
                }
            } finally {
                TooltipHandler.popTransform();
            }
        } finally {
            GlStateManagerCompat.enableLighting();
            GlStateManagerCompat.enableDepth();
            RenderHelperCompat.enableStandardItemLighting();
            GlStateManagerCompat.enableRescaleNormal();
            GlStateManagerCompat.disableLighting();
        }
    }

    public static void drawHoveringText(String text, int mouseX, int mouseY, FontRenderer font) {
        List<String> lines = new ArrayList<>();
        lines.add(text);
        drawHoveringText(lines, mouseX, mouseY, font);
    }

    public static void drawItemTooltip(net.minecraft.item.ItemStack stack, int mouseX, int mouseY, FontRenderer font) {
        if (stack == null) return;
        Minecraft mc = MinecraftCompat.getMinecraft();
        List<String> tooltip = ItemCompatKt.getTooltipCompat(stack, MinecraftCompat.getLocalPlayer(), mc.gameSettings.advancedItemTooltips);
        drawHoveringText(tooltip, mouseX, mouseY, font);
    }

    // --- Gradient rendering ---
    public static void drawStringGradientScaleAware(String text, float xPos, float yPos,
                                                    int startColor, int endColor,
                                                    float uiScale, boolean displayScale) {
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, uiScale * scaleDisplay);

        GlStateManagerCompat.translate(xPos, yPos, 0);
        GlStateManagerCompat.scale(finalScale, finalScale, 1f);

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        float currentX = 0;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            String ch = String.valueOf(text.charAt(i));
            float progress = len == 1 ? 0f : (float) i / (len - 1);
            int color = interpolateColor(startColor, endColor, progress);
            DrawContextUtils.drawText(fr, ch, currentX, 0, color, false);
            currentX += DrawContextUtils.getStringWidth(fr, ch);
        }

        GlStateManagerCompat.popMatrix();
    }

    public static void drawCenteredStringGradientScaleAware(String text, float xPos, float yPos,
                                                            int startColor, int endColor,
                                                            float uiScale, boolean displayScale) {
        FontRenderer fr = MinecraftCompat.getFontRenderer();
        // compute total width at scale 1
        int totalWidth = DrawContextUtils.getStringWidth(fr, text);
        float halfWidth = totalWidth / 2f;
        drawStringGradientScaleAware(text, xPos - halfWidth, yPos - fr.FONT_HEIGHT / 2f,
                startColor, endColor, uiScale, displayScale);
    }

    private static int interpolateColor(int start, int end, float factor) {
        int a1 = (start >> 24) & 0xFF;
        int r1 = (start >> 16) & 0xFF;
        int g1 = (start >> 8) & 0xFF;
        int b1 = start & 0xFF;

        int a2 = (end >> 24) & 0xFF;
        int r2 = (end >> 16) & 0xFF;
        int g2 = (end >> 8) & 0xFF;
        int b2 = end & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
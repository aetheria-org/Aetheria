package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.features.misc.ScrollableTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;

import java.util.ArrayList;
import java.util.List;

public final class TextRenderUtils {

    private TextRenderUtils() {
    }

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        float factor = Math.min(1, len / (float) fr.getStringWidth(str));
        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int colour, float factor) {
        GlStateManager.scale(factor, factor, 1);
        fr.drawString(str, x / factor, y / factor, colour, shadow);
        GlStateManager.scale(1 / factor, 1 / factor, 1);
    }

    public static void drawStringCenteredScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = Math.min(1, len / (float) strLen);
        int newLen = Math.min(strLen, len);
        drawStringScaled(str, fr, x - newLen / 2f, y - 8 * factor / 2f, shadow, colour, factor);
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos,int color, float uiScale, boolean displayScale) {
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, uiScale * scaleDisplay);

        GlStateManager.translate(xPos, yPos, 0);
        GlStateManager.scale(finalScale, finalScale, 1f);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos, float uiScale, boolean displayScale) {
        drawStringScaleAware(text,xPos,yPos,-1,uiScale,displayScale);
    }

    public static void drawStringScaleAware(String text, float xPos, float yPos, float uiScale) {
        drawStringScaleAware(text, xPos, yPos, uiScale, true);
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos,int color, float uiScale, boolean displayScale) {
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, uiScale * scaleDisplay);

        GlStateManager.translate(xPos, yPos, 0);
        GlStateManager.scale(finalScale, finalScale, 1f);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        fr.drawString(text, -fr.getStringWidth(text) / 2f, -fr.FONT_HEIGHT / 2f, color, false);
        GlStateManager.popMatrix();
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos, float uiScale, boolean displayScale) {
        drawCenteredStringScaleAware(text, xPos, yPos, -1,uiScale, displayScale);
    }

    public static void drawCenteredStringScaleAware(String text, float xPos, float yPos, float uiScale) {
        drawCenteredStringScaleAware(text, xPos, yPos, uiScale, true);
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font) {
        mouseY += ScrollableTooltips.scrollOffset;

        if (textLines.isEmpty()) {
            GlStateManager.disableLighting();
            return;
        }

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        int tooltipTextWidth = 0;
        for (String line : textLines) tooltipTextWidth = Math.max(tooltipTextWidth, font.getStringWidth(line));

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
                List<String> wl = font.listFormattedStringToWidth(textLines.get(i), tooltipTextWidth);
                if (i == 0) titleLinesCount = wl.size();
                for (String l : wl) {
                    wrappedWidth = Math.max(wrappedWidth, font.getStringWidth(l));
                    wrapped.add(l);
                }
            }
            tooltipTextWidth = wrappedWidth;
            textLines = wrapped;
            tooltipX = mouseX > screenWidth / 2 ? mouseX - 16 - tooltipTextWidth : mouseX + 12;
        }

        int tooltipY = mouseY - 12;
        int tooltipHeight = 8 + (textLines.size() > 1 ? (textLines.size() - 1) * 10 : 0) + (textLines.size() > titleLinesCount ? 2 : 0);
        if (tooltipY + tooltipHeight + 6 > screenHeight) tooltipY = screenHeight - tooltipHeight - 6;

        final int z = 300, bg = 0xF0100010;
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, bg, bg);
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, bg, bg);
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, bg, bg);
        RenderUtils.drawGradientRect(z, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, bg, bg);
        RenderUtils.drawGradientRect(z, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, bg, bg);

        final int borderStart = 0x505000FF;
        final int borderEnd = (borderStart & 0xFEFEFE) >> 1 | borderStart & 0xFF000000;
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 2, tooltipY + tooltipHeight + 2, borderStart, borderEnd);
        RenderUtils.drawGradientRect(z, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 2, borderStart, borderEnd);
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 2, borderStart, borderStart);
        RenderUtils.drawGradientRect(z, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderEnd, borderEnd);

        for (int i = 0; i < textLines.size(); i++) {
            font.drawStringWithShadow(textLines.get(i), tooltipX, tooltipY, -1);
            if (i + 1 == titleLinesCount) tooltipY += 2;
            tooltipY += 10;
        }

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableLighting();
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, FontRenderer font) {
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
        drawHoveringText(textLines, mouseX, mouseY, sr.getScaledWidth(), sr.getScaledHeight(), -1, font);
    }

    public static void drawHoveringText(String text, int mouseX, int mouseY, FontRenderer font) {
        List<String> lines = new ArrayList<>();
        lines.add(text);
        drawHoveringText(lines, mouseX, mouseY, font);
    }

    public static void drawItemTooltip(net.minecraft.item.ItemStack stack, int mouseX, int mouseY, FontRenderer font) {
        if (stack == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        drawHoveringText(tooltip, mouseX, mouseY, font);
    }
}
package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.CustomEmoji;
import io.hamlook.aetheria.features.chat.emoji.EmojiLinks;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.features.chat.emoji.SpritePos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.HashMap;
import java.util.Map;

public final class RenderUtils {

    private static final ResourceLocation SEARCH_BAR_TEX = Resources.SEARCH_BAR_TEX;
    private static final ResourceLocation SEARCH_BAR_TEX_GOLD = Resources.SEARCH_BAR_TEX_GOLD;
    private static final Map<ResourceLocation, Boolean> RESOURCE_CACHE = new HashMap<>();

    private RenderUtils() {
    }

    public static void drawSearchBar(GuiTextField field, boolean useTexture) {
        drawSearchBar(field, useTexture, false);
    }

    public static void drawSearchBar(GuiTextField field, boolean useTexture, boolean useGoldTexture) {
        if (field == null) return;

        int x = field.xPosition;
        int y = field.yPosition;
        int w = field.width;
        int h = field.height;

        GlStateManager.color(1f, 1f, 1f, 1f);

        ResourceLocation texture = useGoldTexture ? SEARCH_BAR_TEX_GOLD : SEARCH_BAR_TEX;
        if (!useTexture || !drawSearchBarTexture(texture, x, y, w, h)) {
            Gui.drawRect(x, y, x + w, y + h, 0xFF2C2C2C);
            Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF111111);
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        String text = field.getText();
        int textY = y - 4 + h / 2;
        int maxWidth = Math.max(8, w - 10);
        String display = fr.trimStringToWidth(text, maxWidth);

        if (field.isFocused()) {
            String toDisplay = display.isEmpty() ? "§7Search..." : display;
            fr.drawStringWithShadow(toDisplay, x + 5, textY, display.isEmpty() ? 0x8F8F8F : 0xFFFFFFFF);

            if (System.currentTimeMillis() % 1000 > 500) {
                int cursor = Math.min(field.getCursorPosition(), text.length());
                String beforeCursor = text.substring(0, cursor);
                int beforeWidth = fr.getStringWidth(fr.trimStringToWidth(beforeCursor, maxWidth));
                Gui.drawRect(x + 5 + beforeWidth, y - 5 + h / 2, x + 6 + beforeWidth, y + 4 + h / 2, 0xFFFFFFFF);
            }
        } else {
            String toDisplay = display.isEmpty() ? "§7Search..." : display;
            fr.drawString(toDisplay, x + 5, textY, 0x8F8F8F);
        }
    }

    private static boolean drawSearchBarTexture(ResourceLocation texture, int x, int y, int w, int h) {
        if (!resourceExists(texture)) return false;

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        for (int yi = 0; yi <= 2; yi++) {
            for (int xi = 0; xi <= 2; xi++) {
                float uMin = 0f, uMax = 4f / 20f;
                int px = x, pw = 4;
                if (xi == 1) {
                    px += 4;
                    uMin = 4f / 20f;
                    uMax = 16f / 20f;
                    pw = w - 8;
                } else if (xi == 2) {
                    px += w - 4;
                    uMin = 16f / 20f;
                    uMax = 1f;
                }

                float vMin = 0f, vMax = 4f / 20f;
                int py = y, ph = 4;
                if (yi == 1) {
                    py += 4;
                    vMin = 4f / 20f;
                    vMax = 16f / 20f;
                    ph = h - 8;
                } else if (yi == 2) {
                    py += h - 4;
                    vMin = 16f / 20f;
                    vMax = 1f;
                }

                drawSearchBarTexturedRect(px, py, pw, ph, uMin, uMax, vMin, vMax);
            }
        }

        GlStateManager.disableBlend();
        return true;
    }

    private static void drawSearchBarTexturedRect(int x, int y, int w, int h, float uMin, float uMax, float vMin, float vMax) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + h, 0).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + w, y + h, 0).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + w, y, 0).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, 0).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }

    private static boolean resourceExists(ResourceLocation location) {
        return RESOURCE_CACHE.computeIfAbsent(location, loc -> {
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(loc);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    public static void drawWorldCircle(double radius, int steps, float lineWidth, float r, float g, float b, float a) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        for (int i = 0; i <= steps; i++) {
            double angle = (Math.PI * 2) * i / steps;
            wr.pos(Math.cos(angle) * radius, 0, Math.sin(angle) * radius).endVertex();
        }
        tess.draw();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    public static void drawFloatingRectDark(int x, int y, int width, int height) {
        drawFloatingRectDark(x, y, width, height, true);
    }

    public static void drawFloatingRectDark(int x, int y, int width, int height, boolean shadow) {
        int alpha = OpenGlHelper.isFramebufferEnabled() ? 0xf0000000 : 0xff000000;
        int main = alpha | 0x202020;
        int light = 0xff2e2e2e;
        int dark = 0xff101010;
        Gui.drawRect(x, y, x + 1, y + height, light);
        Gui.drawRect(x + 1, y, x + width, y + 1, light);
        Gui.drawRect(x + width - 1, y + 1, x + width, y + height, dark);
        Gui.drawRect(x + 1, y + height - 1, x + width - 1, y + height, dark);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, main);
        if (shadow) {
            Gui.drawRect(x + width, y + 2, x + width + 2, y + height + 2, 0x70000000);
            Gui.drawRect(x + 2, y + height, x + width, y + height + 2, 0x70000000);
        }
    }

    public static void drawFloatingRect(int x, int y, int width, int height) {
        drawFloatingRectWithAlpha(x, y, width, height, 0xFF, true);
    }

    public static void drawFloatingRectWithAlpha(int x, int y, int width, int height, int alpha, boolean shadow) {
        int main = (alpha << 24) | 0xc0c0c0;
        int light = (alpha << 24) | 0xf0f0f0;
        int dark = (alpha << 24) | 0x909090;
        Gui.drawRect(x, y, x + 1, y + height, light);
        Gui.drawRect(x + 1, y, x + width, y + 1, light);
        Gui.drawRect(x + width - 1, y + 1, x + width, y + height, dark);
        Gui.drawRect(x + 1, y + height - 1, x + width - 1, y + height, dark);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, main);
        if (shadow) {
            Gui.drawRect(x + width, y + 2, x + width + 2, y + height + 2, (alpha * 3 / 5) << 24);
            Gui.drawRect(x + 2, y + height, x + width, y + height + 2, (alpha * 3 / 5) << 24);
        }
    }

    public static void drawInnerBox(int left, int top, int width, int height) {
        Gui.drawRect(left, top, left + width, top + height, 0x60080808);
        Gui.drawRect(left, top, left + 1, top + height, 0xff080808);
        Gui.drawRect(left, top, left + width, top + 1, 0xff080808);
        Gui.drawRect(left + width - 1, top, left + width, top + height, 0xff282828);
        Gui.drawRect(left, top + height - 1, left + width, top + height, 0xff282828);
    }

    public static void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1, filter);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedRectNoBlend(x, y, width, height, uMin, uMax, vMin, vMax, filter);
        GlStateManager.disableBlend();
    }

    public static void drawTexturedRectNoBlend(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + height, 0).tex(uMin, vMax).endVertex();
        wr.pos(x + width, y + height, 0).tex(uMax, vMax).endVertex();
        wr.pos(x + width, y, 0).tex(uMax, vMin).endVertex();
        wr.pos(x, y, 0).tex(uMin, vMin).endVertex();
        t.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        final double f = 0.00390625;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + height, 0).tex((textureX) * f, (textureY + height) * f).endVertex();
        wr.pos(x + width, y + height, 0).tex((textureX + width) * f, (textureY + height) * f).endVertex();
        wr.pos(x + width, y, 0).tex((textureX + width) * f, (textureY) * f).endVertex();
        wr.pos(x, y, 0).tex((textureX) * f, (textureY) * f).endVertex();
        t.draw();
    }


    public static void drawGradientRect(int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float sA = (startColor >> 24 & 255) / 255f, sR = (startColor >> 16 & 255) / 255f;
        float sG = (startColor >> 8 & 255) / 255f, sB = (startColor & 255) / 255f;
        float eA = (endColor >> 24 & 255) / 255f, eR = (endColor >> 16 & 255) / 255f;
        float eG = (endColor >> 8 & 255) / 255f, eB = (endColor & 255) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(right, top, zLevel).color(sR, sG, sB, sA).endVertex();
        wr.pos(left, top, zLevel).color(sR, sG, sB, sA).endVertex();
        wr.pos(left, bottom, zLevel).color(eR, eG, eB, eA).endVertex();
        wr.pos(right, bottom, zLevel).color(eR, eG, eB, eA).endVertex();
        t.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawLine(int x1, int y1, int x2, int y2, int color, float lineWidth) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(lineWidth);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        tess.draw();

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // Binds and draws the texture for a :name:/alias emoji token at (x, y) as a
    // size x size square. Returns false (drawing nothing) if the emoji isn't
    // known/loaded yet, so callers can fall back to rendering the raw text.
    public static boolean drawEmoji(String nameOrAlias, float x, float y, float size) {
        EmojiManager.Emoji emoji = EmojiManager.getEmoji(nameOrAlias);
        if (emoji != null) {
            String sheetName = EmojiManager.EMOJI_THEMES[ATHRConfig.feature.chat.emojiConfig.emojiTheme];
            ResourceLocation texture = EmojiLinks.getSpriteResource(sheetName);
            int sheetW = EmojiManager.getSheetWidth(sheetName);

            GlStateManager.pushMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

            float uMin = (float) emoji.sheetX / sheetW;
            float uMax = (float)(emoji.sheetX + EmojiLinks.SHEET_RESOLUTION) / sheetW;
            float vMin = (float) emoji.sheetY / sheetW;
            float vMax = (float)(emoji.sheetY + EmojiLinks.SHEET_RESOLUTION) / sheetW;
            drawTexturedRect(x, y, size, size, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
            GlStateManager.popMatrix();
            return true;
        }

        CustomEmoji customEmoji = EmojiManager.getCustomEmoji(nameOrAlias);
        if (customEmoji != null) {
            int sheetW = EmojiManager.getSheetWidth(EmojiLinks.CUSTOM_SHEET);
            if (sheetW <= 0 || customEmoji.sprites.isEmpty()) {
                Aetheria.logger.info("[EMOJI] Cannot render custom emoji :" + nameOrAlias + ": — sheetW=" + sheetW + ", sprites=" + customEmoji.sprites.size());
                return false;
            }

            int frameIndex = 0;
            if (customEmoji.animated && customEmoji.frametime > 0) {
                int elapsed = EmojiManager.getAnimationTime();
                frameIndex = Math.floorDiv(elapsed, customEmoji.frametime) % customEmoji.sprites.size();
            }
            SpritePos pos = customEmoji.sprites.get(frameIndex);

            float uMin = (float) pos.x / sheetW;
            float uMax = (float)(pos.x + customEmoji.width) / sheetW;
            float vMin = (float) pos.y / sheetW;
            float vMax = (float)(pos.y + customEmoji.height) / sheetW;
            if (uMax > 1f || vMax > 1f) {
                Aetheria.logger.info("[EMOJI] UV OOB for :" + nameOrAlias + ": sheetW=" + sheetW + " pos=(" + pos.x + "," + pos.y + ") size=" + customEmoji.width + "x" + customEmoji.height + " uMax=" + uMax + " vMax=" + vMax);
                return false;
            }

            ResourceLocation texture = EmojiLinks.getSpriteResource(EmojiLinks.CUSTOM_SHEET);
            GlStateManager.pushMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

            drawTexturedRect(x, y, size, size, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
            GlStateManager.popMatrix();
            return true;
        }
        Aetheria.logger.info("[EMOJI] Emoji is NULL");
        return false;
    }

    public static int renderStringTrimWidth(String str, boolean shadow, int x, int y, int width, int color, int maxLines) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;

        if (str == null || str.isEmpty()) return 0;

        String[] words = str.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int linesRendered = 0;
        int yOffset = 0;

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int testWidth = fr.getStringWidth(testLine);

            if (testWidth > width && currentLine.length() > 0) {
                if (shadow) {
                    fr.drawStringWithShadow(currentLine.toString(), x, y + yOffset, color);
                } else {
                    fr.drawString(currentLine.toString(), x, y + yOffset, color);
                }
                yOffset += fr.FONT_HEIGHT;
                linesRendered++;

                if (maxLines > 0 && linesRendered >= maxLines) {
                    return yOffset;
                }

                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            if (shadow) {
                fr.drawStringWithShadow(currentLine.toString(), x, y + yOffset, color);
            } else {
                fr.drawString(currentLine.toString(), x, y + yOffset, color);
            }
            yOffset += fr.FONT_HEIGHT;
        }

        return yOffset;
    }
}
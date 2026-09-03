package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.features.chat.emoji.CustomEmoji;
import io.hamlook.aetheria.features.chat.emoji.EmojiLinks;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.features.chat.emoji.SpritePos;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import net.minecraft.client.resources.DefaultPlayerSkin;
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

        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        ResourceLocation texture = useGoldTexture ? SEARCH_BAR_TEX_GOLD : SEARCH_BAR_TEX;
        if (!useTexture || !drawSearchBarTexture(texture, x, y, w, h)) {
            Gui.drawRect(x, y, x + w, y + h, 0xFF2C2C2C);
            Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF111111);
        }

        Minecraft mc = MinecraftCompat.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        String text = field.getText();
        int textY = y - 4 + h / 2;
        int maxWidth = Math.max(8, w - 10);
        String display = fr.trimStringToWidth(text, maxWidth);

        String toDisplay = display.isEmpty() ? "§7Search..." : display;
        if (field.isFocused()) {
            fr.drawStringWithShadow(toDisplay, x + 5, textY, display.isEmpty() ? 0x8F8F8F : 0xFFFFFFFF);

            int cursor = Math.min(field.getCursorPosition(), text.length());
            if (System.currentTimeMillis() % 1000 > 500) {
                String beforeCursor = text.substring(0, cursor);
                int beforeWidth = fr.getStringWidth(fr.trimStringToWidth(beforeCursor, maxWidth));
                Gui.drawRect(x + 5 + beforeWidth, y - 5 + h / 2, x + 6 + beforeWidth, y + 4 + h / 2, 0xFFFFFFFF);
            }
        } else {
            fr.drawString(toDisplay, x + 5, textY, 0x8F8F8F);
        }
    }

    private static boolean drawSearchBarTexture(ResourceLocation texture, int x, int y, int w, int h) {
        if (!resourceExists(texture)) return false;

        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);

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

        GlStateManagerCompat.disableBlend();
        return true;
    }

    private static void drawSearchBarTexturedRect(int x, int y, int w, int h, float uMin, float uMax, float vMin, float vMax) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);
        vb.pos(x, y + h, 0).tex(uMin, vMax).endVertex();
        vb.pos(x + w, y + h, 0).tex(uMax, vMax).endVertex();
        vb.pos(x + w, y, 0).tex(uMax, vMin).endVertex();
        vb.pos(x, y, 0).tex(uMin, vMin).endVertex();
        vb.draw();
    }

    private static boolean resourceExists(ResourceLocation location) {
        return RESOURCE_CACHE.computeIfAbsent(location, loc -> {
            try {
                MinecraftCompat.getMinecraft().getResourceManager().getResource(loc);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    public static boolean drawButton(int x, int y, int w, int h, String tooltip, Runnable drawIcon) {
        NineSliceUtils.draw(Resources.storageBackground(1), x, y, w, h, 6, 18);

        int[] mouse = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledResolution());
        boolean hovered = mouse[0] >= x && mouse[0] < x + w && mouse[1] >= y && mouse[1] < y + h;
        if (drawIcon != null) drawIcon.run();
        if (hovered) {
            Gui.drawRect(x + 1, y + 1, x + w - 2, y + h - 1, 0x33FFFFFF);
            if (tooltip != null && !tooltip.isEmpty()) {
                TextRenderUtils.drawHoveringText(tooltip, mouse[0], mouse[1], MinecraftCompat.getMinecraft().fontRendererObj);
            }
        }
        return hovered;
    }

    public static void drawWorldCircle(double radius, int steps, float lineWidth, float r, float g, float b, float a) {
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.disableDepth();
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, a);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINE_STRIP, TessellatorCompat.POSITION);
        for (int i = 0; i <= steps; i++) {
            double angle = (Math.PI * 2) * i / steps;
            vb.pos(Math.cos(angle) * radius, 0, Math.sin(angle) * radius).endVertex();
        }
        vb.draw();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableTexture2D();
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
        GlStateManagerCompat.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedRectNoBlend(x, y, width, height, uMin, uMax, vMin, vMax, filter);
        GlStateManagerCompat.disableBlend();
    }

    public static void drawTexturedRectNoBlend(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManagerCompat.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);
        vb.pos(x, y + height, 0).tex(uMin, vMax).endVertex();
        vb.pos(x + width, y + height, 0).tex(uMax, vMax).endVertex();
        vb.pos(x + width, y, 0).tex(uMax, vMin).endVertex();
        vb.pos(x, y, 0).tex(uMin, vMin).endVertex();
        vb.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void drawGradientRect(int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float sA = (startColor >> 24 & 255) / 255f, sR = (startColor >> 16 & 255) / 255f;
        float sG = (startColor >> 8 & 255) / 255f, sB = (startColor & 255) / 255f;
        float eA = (endColor >> 24 & 255) / 255f, eR = (endColor >> 16 & 255) / 255f;
        float eG = (endColor >> 8 & 255) / 255f, eB = (endColor & 255) / 255f;

        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.disableAlpha();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.shadeModel(7425);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
        vb.pos(right, top, zLevel).color(sR, sG, sB, sA).endVertex();
        vb.pos(left, top, zLevel).color(sR, sG, sB, sA).endVertex();
        vb.pos(left, bottom, zLevel).color(eR, eG, eB, eA).endVertex();
        vb.pos(right, bottom, zLevel).color(eR, eG, eB, eA).endVertex();
        vb.draw();

        GlStateManagerCompat.shadeModel(7424);
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.enableTexture2D();
    }

    public static void drawLine(int x1, int y1, int x2, int y2, int color, float lineWidth) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(lineWidth);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
        vb.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        vb.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        vb.draw();

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.popMatrix();
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

            GlStateManagerCompat.pushMatrix();
            GlStateManagerCompat.color(1f, 1f, 1f, 1f);
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);

            float uMin = (float) emoji.sheetX / sheetW;
            float uMax = (float) (emoji.sheetX + EmojiLinks.SHEET_RESOLUTION) / sheetW;
            float vMin = (float) emoji.sheetY / sheetW;
            float vMax = (float) (emoji.sheetY + EmojiLinks.SHEET_RESOLUTION) / sheetW;
            drawTexturedRect(x, y, size, size, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
            GlStateManagerCompat.popMatrix();
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
            float uMax = (float) (pos.x + customEmoji.width) / sheetW;
            float vMin = (float) pos.y / sheetW;
            float vMax = (float) (pos.y + customEmoji.height) / sheetW;
            if (uMax > 1f || vMax > 1f) {
                Aetheria.logger.info("[EMOJI] UV OOB for :" + nameOrAlias + ": sheetW=" + sheetW + " pos=(" + pos.x + "," + pos.y + ") size=" + customEmoji.width + "x" + customEmoji.height + " uMax=" + uMax + " vMax=" + vMax);
                return false;
            }

            ResourceLocation texture = EmojiLinks.getSpriteResource(EmojiLinks.CUSTOM_SHEET);
            GlStateManagerCompat.pushMatrix();
            GlStateManagerCompat.color(1f, 1f, 1f, 1f);
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);

            drawTexturedRect(x, y, size, size, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
            GlStateManagerCompat.popMatrix();
            return true;
        }
        return false;
    }

    public static void renderPlayerName(float pixelX, float pixelZ, int color, float headScale, float scale, String name, boolean centered) {
        if (name == null || name.isEmpty()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        float stringWidth = mc.fontRendererObj.getStringWidth(name);

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);

        int alpha = (color >> 24) & 0xFF;
        float nameAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, nameAlpha);

        if (centered) {
            GlStateManagerCompat.translate(pixelX, pixelZ, 0f);
            GlStateManagerCompat.scale(scale, scale, 1.0f);

            float paddingX = 3f;
            float paddingY = 2f;
            float x1 = -stringWidth / 2f - paddingX;
            float y1 = -mc.fontRendererObj.FONT_HEIGHT / 2f - paddingY;
            float x2 = stringWidth / 2f + paddingX;
            float y2 = mc.fontRendererObj.FONT_HEIGHT / 2f + paddingY;

            Gui.drawRect((int) x1, (int) y1, (int) x2, (int) y2, 0x60000000);
            GlStateManagerCompat.enableTexture2D();
            mc.fontRendererObj.drawString(name, (int) (-stringWidth / 2f), (int) (-mc.fontRendererObj.FONT_HEIGHT / 2f), 0xFFFFFFFF);
        } else {
            float headSize = headScale * 8f;
            float half = headSize / 2f;
            float cx = pixelX + half;
            float mapScale = Math.max(ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.scale, 0.01f);
            float cy = (pixelZ - headSize) + ATHRConfig.feature.dungeons.dungeonMapConfig.players.nameOffset / mapScale;

            float nameWidth = stringWidth * scale;
            float nameX = cx - nameWidth / 2f;

            GlStateManagerCompat.translate(nameX, cy, 0f);
            GlStateManagerCompat.scale(scale, scale, scale);
            mc.fontRendererObj.drawString(name, 0, 0, 0xFFFFFFFF);
        }

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManagerCompat.popMatrix();
    }

    public static void renderRoomName(float pixelX, float pixelZ, float scale, String name, int color) {
        if (name == null || name.isEmpty()) return;
        Minecraft mc = MinecraftCompat.getMinecraft();
        String[] words = name.split(" ");
        if (words.length == 0) return;
        int fontHeight = mc.fontRendererObj.FONT_HEIGHT + 1;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.translate(pixelX, pixelZ, 0f);
        GlStateManagerCompat.scale(scale, scale, 1.0f);
        float yTextOffset = words.length * fontHeight / -2f;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            mc.fontRendererObj.drawString(word, (int) (-mc.fontRendererObj.getStringWidth(word) / 2f), (int) (yTextOffset + i * fontHeight), color, true);
        }
        GlStateManagerCompat.popMatrix();
    }

    public static void renderPlayerHead(float x, float y, int color, float scale, ResourceLocation skin, float rotation) {
        if (skin == null) {
            skin = DefaultPlayerSkin.getDefaultSkinLegacy();
        }
        int alpha = (color >> 24) & 0xFF;
        float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        Minecraft mc = MinecraftCompat.getMinecraft();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.pushMatrix();
        float half = (scale * 8f) / 2f;
        float cx = x + half;
        float cy = (y - 1f) + half;
        GlStateManagerCompat.translate(cx, cy, 0f);
        GlStateManagerCompat.rotate(rotation, 0f, 0f, 1f);
        GlStateManagerCompat.translate(-cx, -cy, 0f);
        mc.getTextureManager().bindTexture(skin);
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, headAlpha);
        drawSkinRegion(x, y - 1f, scale * 8f, 8f);
        drawSkinRegion(x, y - 1f, scale * 8f, 40f);
        GlStateManagerCompat.popMatrix();
        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void renderPlayerArrow(float x, float y, float scale, float rotation, int rgbColor, boolean isSelf) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        float size = scale * 8f;
        float half = size / 2f;
        float cx = x + half;
        float cy = (y - 1f) + half;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManagerCompat.translate(cx, cy, 0f);
        GlStateManagerCompat.rotate(rotation, 0f, 0f, 1f);
        GlStateManagerCompat.translate(-half * 0.125f, half * 0.125f, 0f);

        mc.getTextureManager().bindTexture(Resources.DEFAULT_MAP_ICONS);

        int iconType = isSelf ? 1 : 0;
        // map_icons.png is a 32x32 atlas with a 4x4 grid of 8x8 icons.
        // Row coordinate must use INTEGER division first: float division
        // (iconType / 4f / 4f) shifts sampling 2px down, bleeding the row below.
        // iconType is 0 or 1, so both sprites live in atlas row 0 (v = 0..0.25).
        float u0 = (iconType % 4) / 4f;
        float u1 = u0 + 1 / 4f;
        float v0 = 0f;
        float v1 = 0.25f;

        int alphaByte = (rgbColor >>> 24) & 0xFF;
        float a = (alphaByte == 0) ? 1.0f : alphaByte / 255f;
        float r = ((rgbColor >> 16) & 0xFF) / 255f;
        float g = ((rgbColor >> 8) & 0xFF) / 255f;
        float b = (rgbColor & 0xFF) / 255f;
        GlStateManagerCompat.color(r, g, b, a);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);
        vb.pos(-half, half, 0.0D).tex(u0, v0).endVertex();
        vb.pos(half, half, 0.0D).tex(u1, v0).endVertex();
        vb.pos(half, -half, 0.0D).tex(u1, v1).endVertex();
        vb.pos(-half, -half, 0.0D).tex(u0, v1).endVertex();
        vb.draw();

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManagerCompat.popMatrix();
    }

    public static void renderFramedHead(float cx, float cy, float yaw, float frameSize, String frameColorStr, ResourceLocation skin, boolean flowChroma) {
        float headAlpha = 1f;
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (frameColorStr == null || frameColorStr.isEmpty()) frameColorStr = "0:255:85:255:85";
        if (skin == null) skin = DefaultPlayerSkin.getDefaultSkinLegacy();

        // Flow requires both the feature toggle and a chroma speed above 0 in
        // the color string; otherwise the solid picked color is rendered.
        boolean flow = flowChroma && ChromaColour.getSpeed(frameColorStr) > 0;
        int solidArgb = 0;
        int chromaBase = 0;
        int chromaMode = 0;
        float chromaSize = 0f;
        if (flow) {
            ChromaStyle style = ChromaStyle.of(frameColorStr);
            chromaBase = style.toArgb();
            chromaMode = style.getMode();
            chromaSize = style.getSize();
        } else {
            solidArgb = ChromaColour.specialToChromaRGB(frameColorStr);
        }

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(cx, cy, 0f);
        GlStateManagerCompat.rotate(yaw, 0f, 0f, 1f);

        float half = frameSize / 2f;
        float headHalf = half * 0.72f;

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.enableAlpha();

        GlStateManagerCompat.disableCull();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);

        VertexBuilder vb;

        GlStateManagerCompat.disableTexture2D();
        vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
        if (flow) {
            int strips = Math.max(4, (int) Math.ceil(frameSize));
            float stripW = frameSize / strips;
            for (int i = 0; i < strips; i++) {
                float sx = -half + i * stripW;
                float ex = (i == strips - 1) ? half : sx + stripW;
                int argb = ChromaColour.applyChromaShift(chromaBase, cx + sx, cy, chromaMode, chromaSize);
                float a = ((argb >>> 24) & 0xFF) / 255f;
                float r = argbR(argb), g = argbG(argb), b = argbB(argb);
                vb.pos(sx, -half, 0d).color(r, g, b, a).endVertex();
                vb.pos(sx, half, 0d).color(r, g, b, a).endVertex();
                vb.pos(ex, half, 0d).color(r, g, b, a).endVertex();
                vb.pos(ex, -half, 0d).color(r, g, b, a).endVertex();
            }
        } else {
            float a = ((solidArgb >>> 24) & 0xFF) / 255f;
            float r = argbR(solidArgb), g = argbG(solidArgb), b = argbB(solidArgb);
            vb.pos(-half, -half, 0d).color(r, g, b, a).endVertex();
            vb.pos(-half, half, 0d).color(r, g, b, a).endVertex();
            vb.pos(half, half, 0d).color(r, g, b, a).endVertex();
            vb.pos(half, -half, 0d).color(r, g, b, a).endVertex();
        }
        vb.draw();

        GlStateManagerCompat.enableTexture2D();
        mc.getTextureManager().bindTexture(skin);
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.color(1f, 1f, 1f, headAlpha);
        drawSkinRegion(-headHalf, -headHalf, headHalf * 2f, 8f);
        drawSkinRegion(-headHalf, -headHalf, headHalf * 2f, 40f);

        GlStateManagerCompat.disableTexture2D();
        float tw = frameSize / 3f;
        float th = frameSize / 3.5f;
        int cApex, cBaseL, cBaseR;
        if (flow) {
            cApex = ChromaColour.applyChromaShift(chromaBase, cx, cy - half - th, chromaMode, chromaSize);
            cBaseL = ChromaColour.applyChromaShift(chromaBase, cx - tw / 2f, cy - half, chromaMode, chromaSize);
            cBaseR = ChromaColour.applyChromaShift(chromaBase, cx + tw / 2f, cy - half, chromaMode, chromaSize);
        } else {
            cApex = solidArgb;
            cBaseL = solidArgb;
            cBaseR = solidArgb;
        }
        vb = TessellatorCompat.beginDraw(TessellatorCompat.TRIANGLES, TessellatorCompat.POSITION_COLOR);
        vb.pos(-tw / 2f, -half, 0d).color(argbR(cBaseL), argbG(cBaseL), argbB(cBaseL),
                ((cBaseL >>> 24) & 0xFF) / 255f).endVertex();
        vb.pos(tw / 2f, -half, 0d).color(argbR(cBaseR), argbG(cBaseR), argbB(cBaseR),
                ((cBaseR >>> 24) & 0xFF) / 255f).endVertex();
        vb.pos(0f, -half - th, 0d).color(argbR(cApex), argbG(cApex), argbB(cApex),
                ((cApex >>> 24) & 0xFF) / 255f).endVertex();
        vb.draw();

        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.popMatrix();
    }

    /** One textured layer of a skinhead quad at float coords (POSITION_TEX). Both face and hat layers share texture row y=8. */
    private static void drawSkinRegion(float x, float y, float size, float texU) {
        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);
        float u0 = texU / 64f, v0 = 8f / 64f, u1 = (texU + 8f) / 64f, v1 = 16f / 64f;
        vb.pos(x, y + size, 0d).tex(u0, v1).endVertex();
        vb.pos(x + size, y + size, 0d).tex(u1, v1).endVertex();
        vb.pos(x + size, y, 0d).tex(u1, v0).endVertex();
        vb.pos(x, y, 0d).tex(u0, v0).endVertex();
        vb.draw();
    }

    private static float argbR(int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    private static float argbG(int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    private static float argbB(int argb) {
        return (argb & 0xFF) / 255f;
    }

    public static void renderMapCheckmark(ResourceLocation texture, float x, float y, float size) {
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        drawTexturedRect(x, y, size, size, GL11.GL_NEAREST);
    }

    public static void drawRoundedButton(ResourceLocation texture, int x, int y, int w, int h, int r, int cornerSize, int texSize,boolean hovered) {
        if (texture == null) return;
        r = Math.min(r, Math.min(w, h) / 2);

        if (r <= 0) {
            NineSliceUtils.draw(texture, x, y, w, h, cornerSize, texSize,hovered);
            return;
        }

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0xFF);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        GlStateManagerCompat.colorMask(false, false, false, false);
        GlStateManagerCompat.depthMask(false);

        drawRoundedRect(x, y, w, h, r, 0xFFFFFFFF);

        GlStateManagerCompat.colorMask(true, true, true, true);
        GlStateManagerCompat.depthMask(true);
        GL11.glStencilMask(0x00);

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);

        NineSliceUtils.draw(texture, x, y, w, h, cornerSize, texSize,hovered);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void drawRoundedRect(int x,int y, int w, int h, int r , int color){
        int x1 = x + w;
        int y1 = y + h;
        r = Math.min(r,Math.min(x1-x,y1-y)/2);
        if (r <= 0) {
            Gui.drawRect(x,y,x1,y1,color);
            return;
        }
        Gui.drawRect(x + r, y, x1 - r, y1, color);
        Gui.drawRect(x, y + r, x + r, y1 - r, color);
        Gui.drawRect(x1 - r, y + r, x1, y1 - r, color);

        drawCornerArc(x, y, r, color, false, false);
        drawCornerArc(x1 - r, y, r, color, true, false);
        drawCornerArc(x, y1 - r, r, color, false, true);
        drawCornerArc(x1 - r, y1 - r, r, color, true, true);
    }
    public static void drawRoundedBorder(int x, int y, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min(x2 - x, y2 - y) / 2);
        if (r <= 0) {
            Gui.drawRect(x, y, x2, y + 1, color);
            Gui.drawRect(x, y2 - 1, x2, y2, color);
            Gui.drawRect(x, y + 1, x + 1, y2 - 1, color);
            Gui.drawRect(x2 - 1, y + 1, x2, y2 - 1, color);
            return;
        }
        Gui.drawRect(x + r, y, x2 - r, y + 1, color);
        Gui.drawRect(x + r, y2 - 1, x2 - r, y2, color);
        Gui.drawRect(x, y + r, x + 1, y2 - r, color);
        Gui.drawRect(x2 - 1, y + r, x2, y2 - r, color);

        drawCornerArc(x, y, r, color, false, false);
        drawCornerArc(x2 - r, y, r, color, true, false);
        drawCornerArc(x, y2 - r, r, color, false, true);
        drawCornerArc(x2 - r, y2 - r, r, color, true, true);
    }

    private static int cornerCut(int i, int r) {
        return (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - (double) (r - i - 1) * (r - i - 1))));
    }

    private static void drawCornerArc(int cx, int cy, int r, int color, boolean flipX, boolean flipY) {
        for (int i = 0; i < r; i++) {
            drawArcPixel(cx, cy, r, color, flipX, flipY, i, cornerCut(i, r));
        }
        for (int j = 0; j < r; j++) {
            int left = 0;
            while (left < r && cornerCut(left, r) > j) left++;
            if (left < r) drawArcPixel(cx, cy, r, color, flipX, flipY, left, j);
        }
    }

    private static void drawArcPixel(int cx, int cy, int r, int color, boolean flipX, boolean flipY, int u, int v) {
        int px = flipX ? cx + (r - 1 - u) : cx + u;
        int py = flipY ? cy + (r - 1 - v) : cy + v;
        Gui.drawRect(px, py, px + 1, py + 1, color);
    }
}
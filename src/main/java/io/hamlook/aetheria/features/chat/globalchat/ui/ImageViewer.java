package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

/**
 * Fullscreen modal image/gif viewer, opened when a ChatLine's media is
 * clicked. Modal: while visible it should consume all input before it
 * reaches the chat/composer behind it.
 */
public class ImageViewer {

    private List<GCImage> images = Collections.emptyList();
    private int index = 0;
    private boolean visible = false;

    private static final int MAX_WIDTH_FRACTION_PERCENT = 80;
    private static final int MAX_HEIGHT_FRACTION_PERCENT = 80;
    private static final int ARROW_HITBOX_SIZE = 30;
    private static final int PLACEHOLDER_SIZE = 200;

    private int lastImgX, lastImgY, lastImgW, lastImgH;
    private int lastPrevX, lastPrevY, lastNextX, lastNextY;

    public void open(List<GCImage> gallery, int startIndex) {
        if (gallery == null || gallery.isEmpty()) return;
        this.images = gallery;
        this.index = Math.max(0, Math.min(startIndex, gallery.size() - 1));
        this.visible = true;
    }

    public void close() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void draw(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (!visible || images.isEmpty()) return;

        net.minecraft.client.gui.Gui.drawRect(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 200).getRGB());

        GCImage current = images.get(index);
        int naturalW = (current != null && current.width > 0) ? current.width : PLACEHOLDER_SIZE;
        int naturalH = (current != null && current.height > 0) ? current.height : PLACEHOLDER_SIZE;

        int maxW = screenWidth * MAX_WIDTH_FRACTION_PERCENT / 100;
        int maxH = screenHeight * MAX_HEIGHT_FRACTION_PERCENT / 100;

        float scale = Math.min(maxW / (float) naturalW, maxH / (float) naturalH);
        scale = Math.min(scale, 4f); // don't blow tiny images up absurdly
        int drawW = Math.max(1, Math.round(naturalW * scale));
        int drawH = Math.max(1, Math.round(naturalH * scale));

        int imgX = (screenWidth - drawW) / 2;
        int imgY = (screenHeight - drawH) / 2;
        lastImgX = imgX; lastImgY = imgY; lastImgW = drawW; lastImgH = drawH;

        if (current != null) {
            // Always render as "focused" so a normally-reduced-motion sticker
            // still plays fully once the user has expanded it.
            ResourceLocation frame = current.getTextureToRender(true);
            if (frame != null) {
                GlStateManager.pushMatrix();
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableTexture2D();
                Minecraft.getMinecraft().getTextureManager().bindTexture(frame);
                RenderUtils.drawTexturedRect(imgX, imgY, drawW, drawH, 0f, 1f, 0f, 1f);
                GlStateManager.popMatrix();
            }
        }

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        if (images.size() > 1) {
            String prevLabel = "<";
            String nextLabel = ">";
            lastPrevX = imgX - ARROW_HITBOX_SIZE - 10;
            lastPrevY = imgY + drawH / 2 - ARROW_HITBOX_SIZE / 2;
            lastNextX = imgX + drawW + 10;
            lastNextY = lastPrevY;

            boolean prevHovered = isHovered(lastPrevX, lastPrevY, ARROW_HITBOX_SIZE, ARROW_HITBOX_SIZE, mouseX, mouseY);
            boolean nextHovered = isHovered(lastNextX, lastNextY, ARROW_HITBOX_SIZE, ARROW_HITBOX_SIZE, mouseX, mouseY);

            fr.drawString(prevLabel, lastPrevX + ARROW_HITBOX_SIZE / 2 - fr.getStringWidth(prevLabel) / 2,
                    lastPrevY + ARROW_HITBOX_SIZE / 2 - fr.FONT_HEIGHT / 2, prevHovered ? 0xFFFFFF : 0xAAAAAA);
            fr.drawString(nextLabel, lastNextX + ARROW_HITBOX_SIZE / 2 - fr.getStringWidth(nextLabel) / 2,
                    lastNextY + ARROW_HITBOX_SIZE / 2 - fr.FONT_HEIGHT / 2, nextHovered ? 0xFFFFFF : 0xAAAAAA);

            String counter = (index + 1) + " / " + images.size();
            fr.drawString(counter, screenWidth / 2 - fr.getStringWidth(counter) / 2, imgY - fr.FONT_HEIGHT - 6, 0xFFFFFF);
        }

        String closeHint = "Press ESC or click outside to close";
        fr.drawString(closeHint, screenWidth / 2 - fr.getStringWidth(closeHint) / 2, screenHeight - 20, 0x888888);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible) return false;

        if (images.size() > 1) {
            if (isHovered(lastPrevX, lastPrevY, ARROW_HITBOX_SIZE, ARROW_HITBOX_SIZE, mouseX, mouseY)) {
                index = (index - 1 + images.size()) % images.size();
                return true;
            }
            if (isHovered(lastNextX, lastNextY, ARROW_HITBOX_SIZE, ARROW_HITBOX_SIZE, mouseX, mouseY)) {
                index = (index + 1) % images.size();
                return true;
            }
        }

        if (!isHovered(lastImgX, lastImgY, lastImgW, lastImgH, mouseX, mouseY)) {
            close();
        }
        return true; // modal - always consume clicks while open
    }

    public void keyTyped(int keyCode) {
        if (!visible) return;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            close();
        } else if (keyCode == Keyboard.KEY_LEFT && images.size() > 1) {
            index = (index - 1 + images.size()) % images.size();
        } else if (keyCode == Keyboard.KEY_RIGHT && images.size() > 1) {
            index = (index + 1) % images.size();
        }
    }

    private boolean isHovered(int x, int y, int w, int h, int mX, int mY) {
        return mX > x && mX < x + w && mY > y && mY < y + h;
    }
}
package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.utils.MediaSaver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Full-size image viewer opened by clicking an attachment / GIF / image embed
 * in the chat. Shows the image at true resolution (fitted to the screen),
 * with left/right arrows, keyboard navigation and a bottom strip of preview
 * thumbnails. Images come from the shared {@link ImageManager} cache so no
 * re-download happens.
 */
public class ImageViewerScreen extends GuiScreen {

    public static class ImageRef {
        public final String name;
        public final String url;

        public ImageRef(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private final List<? extends ImageRef> images;
    private int current;

    private int closeX, closeY, closeW, closeH;
    private int leftX, leftY, rightX, arrowW, arrowH;
    private final List<int[]> thumbRects = new ArrayList<>();
    private String downloadMsg = null;
    private long downloadMsgUntil = 0;

    public ImageViewerScreen(List<? extends ImageRef> images, int startIndex) {
        this.images = images;
        this.current = images.isEmpty() ? 0 : Math.max(0, Math.min(startIndex, images.size() - 1));
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xC8101418);
        if (images.isEmpty()) {
            mc.displayGuiScreen(null);
            return;
        }

        GCImage img = getImage(images.get(current).url);

        int topMargin = 40;
        int sideMargin = 90;
        int bottomArea = 120;
        int availW = width - sideMargin * 2;
        int availH = height - topMargin - bottomArea;

        if (img != null && img.isLoaded && img.width > 0 && img.height > 0) {
            float ratio = img.width / (float) img.height;
            int drawW = Math.min(img.width, availW);
            int drawH = Math.round(drawW / ratio);
            if (drawH > availH) {
                drawH = availH;
                drawW = Math.round(drawH * ratio);
            }
            ResourceLocation tex = img.getTextureToRender(true);
            if (tex != null) {
                int ix = (width - drawW) / 2;
                int iy = topMargin + (availH - drawH) / 2;
                mc.getTextureManager().bindTexture(tex);
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableBlend();
                drawScaledCustomSizeModalRect(ix, iy, 0, 0, img.width, img.height, drawW, drawH, img.width, img.height);
                GlStateManager.disableBlend();
            }
        } else {
            String label = img != null && img.loadFailed ? "Could not load image" : "Loading...";
            drawCenteredString(fontRendererObj, label, width / 2, height / 2 - 8, 0xFF949BA4);
        }

        ImageRef ref = images.get(current);
        String info = ref.name != null && !ref.name.isEmpty()
                ? (current + 1) + " / " + images.size() + "  -  " + ref.name
                : (current + 1) + " / " + images.size();
        drawCenteredString(fontRendererObj, info, width / 2, 12, 0xFFB5BAC1);

        closeX = width - 36;
        closeY = 8;
        closeW = 24;
        closeH = 22;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH;
        drawRect(closeX, closeY, closeX + closeW, closeY + closeH, closeHover ? 0xFF404249 : 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow("X", closeX + (closeW - fontRendererObj.getStringWidth("X")) / 2f, closeY + 6, 0xFFDCDDDE);

        if (MediaSaver.isMediaUrl(ref.url)) {
            drawSaveButton(mouseX, mouseY);
        }

        arrowW = 40;
        arrowH = 40;
        leftX = 16;
        rightX = width - 16 - arrowW;
        leftY = height / 2 - arrowH / 2;
        drawArrowButton(leftX, leftY, true, current > 0, mouseX, mouseY);
        drawArrowButton(rightX, leftY, false, current < images.size() - 1, mouseX, mouseY);

        drawThumbnails(mouseX, mouseY);

        if (downloadMsg != null && System.currentTimeMillis() < downloadMsgUntil) {
            int toastY = height - 44;
            drawRect(20, toastY, width - 20, toastY + 20, 0xE62B2D31);
            drawCenteredString(fontRendererObj, downloadMsg, width / 2, toastY + 6, 0xFFDCDDDE);
        }
    }

    private void drawSaveButton(int mouseX, int mouseY) {
        int bx = closeX - 64;
        int bw = 56;
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= closeY && mouseY <= closeY + closeH;
        drawRect(bx, closeY, bx + bw, closeY + closeH, hover ? 0xFF404249 : 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow("Save", bx + (bw - fontRendererObj.getStringWidth("Save")) / 2f, closeY + 6, 0xFFDCDDDE);
    }

    private void drawArrowButton(int x, int y, boolean left, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX <= x + arrowW && mouseY >= y && mouseY <= y + arrowH;
        drawRect(x, y, x + arrowW, y + arrowH, hover ? 0x66FFFFFF : 0x2AFFFFFF);
        int cx = x + arrowW / 2;
        int cy = y + arrowH / 2;
        int s = 8;
        int color = enabled ? 0xFFDCDDDE : 0x555B5F64;
        if (left) {
            drawTriangle(cx - s, cy, cx + s, cy - s, cx + s, cy + s, color);
        } else {
            drawTriangle(cx + s, cy, cx - s, cy - s, cx - s, cy + s, color);
        }
    }

    private void drawThumbnails(int mouseX, int mouseY) {
        int thumb = 64;
        int gap = 8;
        int total = images.size() * thumb + (images.size() - 1) * gap;
        int startX = (width - total) / 2;
        if (total > width - 40) {
            thumb = Math.max(24, (width - 40 - (images.size() - 1) * gap) / images.size());
            total = images.size() * thumb + (images.size() - 1) * gap;
            startX = 20;
        }
        int ty = height - thumb - 24;
        thumbRects.clear();
        for (int i = 0; i < images.size(); i++) {
            int tx = startX + i * (thumb + gap);
            GCImage img = getImage(images.get(i).url);
            if (img != null && img.isLoaded && img.width > 0 && img.height > 0) {
                ResourceLocation tex = img.getTextureToRender(true);
                if (tex != null) {
                    float ratio = img.width / (float) img.height;
                    int srcW, srcH, srcX = 0, srcY = 0;
                    if (ratio > 1f) { srcW = srcH = img.height; srcX = (img.width - srcW) / 2; }
                    else { srcW = srcH = img.width; srcY = (img.height - srcH) / 2; }
                    mc.getTextureManager().bindTexture(tex);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    GlStateManager.enableBlend();
                    drawScaledCustomSizeModalRect(tx, ty, srcX, srcY, srcW, srcH, thumb, thumb, img.width, img.height);
                    GlStateManager.disableBlend();
                }
            } else {
                drawRect(tx, ty, tx + thumb, ty + thumb, 0xFF232428);
            }
            boolean sel = i == current;
            boolean hover = mouseX >= tx && mouseX <= tx + thumb && mouseY >= ty && mouseY <= ty + thumb;
            int border = sel ? 0xFF5865F2 : hover ? 0x99FFFFFF : 0x40FFFFFF;
            drawRect(tx, ty, tx + thumb, ty + 2, border);
            drawRect(tx, ty + thumb - 2, tx + thumb, ty + thumb, border);
            drawRect(tx, ty, tx + 2, ty + thumb, border);
            drawRect(tx + thumb - 2, ty, tx + thumb, ty + thumb, border);
            thumbRects.add(new int[]{tx, ty, thumb, thumb});
        }
    }

    private void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2i(x1, y1);
        GL11.glVertex2i(x2, y2);
        GL11.glVertex2i(x3, y3);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
    }

    private GCImage getImage(String url) {
        if (url == null || url.isEmpty()) return null;
        return ImageManager.images.get(ImageManager.getOrCreateImage(url, false));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH) {
                mc.displayGuiScreen(null);
                return;
            }
            ImageRef ref = images.get(current);
            if (MediaSaver.isMediaUrl(ref.url) && mouseX >= closeX - 64 && mouseX <= closeX - 8 && mouseY >= closeY && mouseY <= closeY + closeH) {
                downloadCurrent();
                return;
            }
            if (current > 0 && mouseX >= leftX && mouseX <= leftX + arrowW && mouseY >= leftY && mouseY <= leftY + arrowH) {
                current--;
                return;
            }
            if (current < images.size() - 1 && mouseX >= rightX && mouseX <= rightX + arrowW && mouseY >= leftY && mouseY <= leftY + arrowH) {
                current++;
                return;
            }
            for (int i = 0; i < thumbRects.size(); i++) {
                int[] r = thumbRects.get(i);
                if (mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3]) {
                    current = i;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        } else if (keyCode == Keyboard.KEY_LEFT) {
            if (current > 0) current--;
        } else if (keyCode == Keyboard.KEY_RIGHT) {
            if (current < images.size() - 1) current++;
        } else if (keyCode == Keyboard.KEY_S) {
            downloadCurrent();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private void downloadCurrent() {
        final ImageRef ref = images.get(current);
        if (ref == null || !MediaSaver.isMediaUrl(ref.url)) return;
        CompletableFuture.runAsync(() -> {
            try {
                String path = MediaSaver.save(ref.url, ref.name);
                mc.addScheduledTask(() -> flashDownloadMsg("Saved to " + path));
            } catch (Exception e) {
                mc.addScheduledTask(() -> flashDownloadMsg("Download failed: " + e.getMessage()));
            }
        });
    }

    private void flashDownloadMsg(String msg) {
        downloadMsg = msg;
        downloadMsgUntil = System.currentTimeMillis() + 3500;
    }
}

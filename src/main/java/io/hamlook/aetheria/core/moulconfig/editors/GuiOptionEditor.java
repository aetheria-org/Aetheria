// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.FontRenderer;

public abstract class GuiOptionEditor {

    protected final ConfigProcessor.ProcessedOption option;
    private static final int HEIGHT = 45;

    public GuiOptionEditor(ConfigProcessor.ProcessedOption option) {
        this.option = option;
    }

    protected int controlCenterX(int x, int width) {
        return x + width * 5 / 6;
    }

    protected int controlLeft(int x, int width, int controlWidth) {
        return controlCenterX(x, width) - controlWidth / 2;
    }

    protected int controlY(int y, int height, int controlHeight) {
        return y + Math.max(0, (height - controlHeight) / 2);
    }

    public void render(int x, int y, int width) {
        int height = getHeight();

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        ThemeContext.panel(x, y, width, height);
        ThemeContext.rect(x, y, 2, height, ThemeContext.ACCENT);
        TextRenderUtils.drawStringScaledMaxWidth(option.name, fr, x + 10, y + 6, false, width * 2 / 3 - 18, ThemeContext.TEXT);
        renderDescription(x, y, width);
    }

    protected void renderDescription(int x, int y, int width) {
        FontRenderer fr = MinecraftCompat.getFontRenderer();
        float scale = 1;
        int descWidth = Math.max(20, width * 2 / 3 - 18);
        int lineCount = DrawContextUtils.wrapFormattedText(fr, option.desc, descWidth).size();

        if (lineCount <= 0) return;

        float paraHeight = 9 * lineCount - 1;

        while (paraHeight > HEIGHT - 27 && scale > 0.5f) {
            scale -= 1 / 8f;
            lineCount = DrawContextUtils.wrapFormattedText(fr, option.desc, (int) (descWidth / scale)).size();
            paraHeight = (int) (9 * scale * lineCount - 1 * scale);
        }
        if (paraHeight > HEIGHT - 27) {
            scale *= (HEIGHT - 27f) / paraHeight;
            paraHeight = HEIGHT - 27f;
        }

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(x + 10, y + 23, 0);
        GlStateManagerCompat.scale(scale, scale, 1);

        DrawContextUtils.drawSplitString(fr, option.desc, 0, 0, (int) (descWidth / scale), ThemeContext.MUTED);

        GlStateManagerCompat.popMatrix();
    }

    public int getHeight() {
        return HEIGHT;
    }

    public abstract boolean mouseInput(int x, int y, int width, int mouseX, int mouseY);

    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        return mouseInput(x, y, width, mouseX, mouseY);
    }

    public abstract boolean keyboardInput();

    public boolean capturesEscape() {
        return false;
    }

    public boolean keyboardInput(char typedChar, int keyCode) {
        return keyboardInput();
    }

    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        return false;
    }

    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        return mouseInputOverlay(x, y, width, mouseX, mouseY);
    }

    public void renderOverlay(int x, int y, int width) {
    }

    public void resetTransientState() {
    }
}

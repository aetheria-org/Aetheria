package io.hamlook.aetheria.features.dungeons.caseopening;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

public class FloatFontRenderer {
    private final FontRenderer fontRenderer;

    public FloatFontRenderer(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    public void drawCenteredString(String text, float x, float y, int color, boolean shadow) {
        float textWidth = DrawContextUtils.getStringWidth(fontRenderer, text);
        GL11.glPushMatrix();
        GL11.glTranslatef(x - textWidth / 2f, y - 4.5f, 0f);
        DrawContextUtils.drawText(fontRenderer, text, 0, 0, color, shadow);
        GL11.glPopMatrix();
    }
}
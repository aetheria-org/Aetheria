package io.hamlook.aetheria.features.custommenu.ui.buttons;

import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

/** Central renderer for non-textured button variants. */
public final class ButtonRenderer {
    private ButtonRenderer() { }

    public static void drawRounded(int x, int y, int width, int height, ButtonStyle style, boolean hovered) {
        int radius = Math.min(6, Math.min(width, height) / 2);
        int fill = hovered ? lighten(style.fillColor, 18) : style.fillColor;
        Gui.drawRect(x + radius, y, x + width - radius, y + height, fill);
        Gui.drawRect(x, y + radius, x + width, y + height - radius, fill);
        drawCircle(x + radius, y + radius, radius, fill);
        drawCircle(x + width - radius, y + radius, radius, fill);
        drawCircle(x + radius, y + height - radius, radius, fill);
        drawCircle(x + width - radius, y + height - radius, radius, fill);
        if (style == ButtonStyle.ROUNDED_OUTLINE || hovered) outline(x, y, width, height, style.accentColor);
    }

    private static void outline(int x, int y, int width, int height, int color) {
        Gui.drawRect(x + 5, y, x + width - 5, y + 1, color);
        Gui.drawRect(x + 5, y + height - 1, x + width - 5, y + height, color);
        Gui.drawRect(x, y + 5, x + 1, y + height - 5, color);
        Gui.drawRect(x + width - 1, y + 5, x + width, y + height - 5, color);
    }

    private static void drawCircle(int cx, int cy, int radius, int color) {
        GL11.glColor4ub((byte) (color >> 16), (byte) (color >> 8), (byte) color, (byte) (color >> 24));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 20; i++) {
            double a = Math.PI * 2 * i / 20.0;
            GL11.glVertex2f((float) (cx + Math.cos(a) * radius), (float) (cy + Math.sin(a) * radius));
        }
        GL11.glEnd();
    }

    private static int lighten(int color, int amount) {
        int r = Math.min(255, (color >> 16 & 255) + amount), g = Math.min(255, (color >> 8 & 255) + amount), b = Math.min(255, (color & 255) + amount);
        return color & 0xff000000 | r << 16 | g << 8 | b;
    }
}

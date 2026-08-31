package io.hamlook.aetheria.features.custommenu.ui.buttons;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.render.RenderUtils;

/** Central renderer for non-textured button variants. */
public final class ButtonRenderer {
    private ButtonRenderer() { }

    public static void drawRounded(int x, int y, int width, int height, ButtonStyle style, boolean hovered) {
        int radius = Math.min(6, Math.min(width, height) / 2);
        RenderUtils.drawRoundedButton(Resources.betterContainerNineSlice(style.index),x,y,width,height,radius,6,18,hovered);
    }
}

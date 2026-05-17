package com.jef.justenoughfakepixel.features.profile.viewer.ui.util;

import com.jef.justenoughfakepixel.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class StringDrawer {

    public static void drawString(String text, float xPos, float yPos, float uiScale, boolean displayScale) {
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();

        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = uiScale * scaleDisplay;

        finalScale = Math.max(0.25f, finalScale);
        GlStateManager.translate(xPos * finalScale, yPos * finalScale, 0);

        GlStateManager.scale(finalScale, finalScale, 1.0f);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, 0, 0, -1);

        GlStateManager.popMatrix();
    }

    public static void drawString(String text, float xPos, float yPos, float uiScale) {
        drawString(text, xPos, yPos, uiScale, true);
    }
}
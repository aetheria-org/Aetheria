package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.gui.ScaledResolution;

public class ResolutionUtils {
    private static ScaledResolution scaledResolution;

    public static int getHeight() {
        scaledResolution = GuiScreenUtils.getScaledResolution();
        return scaledResolution.getScaledHeight();
    }

    public static int getWidth() {
        scaledResolution = GuiScreenUtils.getScaledResolution();
        return scaledResolution.getScaledWidth();
    }

    public static int getFactor() {
        scaledResolution = GuiScreenUtils.getScaledResolution();
        return scaledResolution.getScaleFactor();
    }

    public static float getXRatio(int x) {
        return x / 1920f;
    }

    public static float getXStatic(int x) {
        return getWidth() * getXRatio(x);
    }

    public static float getYStatic(int y) {
        return getHeight() * getYRatio(y);
    }

    public static float getYRatio(int y) {
        return y / 1080f;
    }
}

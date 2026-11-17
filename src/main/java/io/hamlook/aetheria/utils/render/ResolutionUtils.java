package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.GuiScreenUtils;

public class ResolutionUtils {
    public static int getHeight() { return GuiScreenUtils.getScaledWindowHeight(); }
    public static int getWidth()  { return GuiScreenUtils.getScaledWindowWidth(); }
    public static int getFactor() { return GuiScreenUtils.getScaleFactor(); }
    public static float getXRatio(int x) { return x / 1920f; }
    public static float getXStatic(int x) { return getWidth() * getXRatio(x); }
    public static float getYStatic(int y) { return getHeight() * getYRatio(y); }
    public static float getYRatio(int y) { return y / 1080f; }
}

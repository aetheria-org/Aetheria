package io.hamlook.aetheria.features.custommenu.util;

/**
 * The single coordinate conversion layer for CMM.
 *
 * Percentages are relative to the active GuiScreen viewport. GuiScreen already
 * exposes Minecraft's GUI-scaled coordinate space, so these values must not be
 * converted through a second design-space scale.
 */
public final class PercentageUtils {
    private PercentageUtils() { }

    private static int baseWidth() { int value = ScreenHelper.getScaledWidth(); return value > 0 ? value : 854; }
    private static int baseHeight() { int value = ScreenHelper.getScaledHeight(); return value > 0 ? value : 480; }

    public static int x(float percent) { return Math.round(baseWidth() * percent / 100f); }
    public static int y(float percent) { return Math.round(baseHeight() * percent / 100f); }
    public static int width(float percent) { return Math.max(1, Math.round(baseWidth() * percent / 100f)); }
    public static int height(float percent) { return Math.max(1, Math.round(baseHeight() * percent / 100f)); }
    public static int centerX() { return x(50f); }
    public static int centerY() { return y(50f); }
    public static int centeredX(float widthPercent) { return x(50f - widthPercent / 2f); }
    public static int centeredY(float heightPercent) { return y(50f - heightPercent / 2f); }
    public static float xPercent(float pixels) { return pixels * 100f / Math.max(1, x(100f)); }
    public static float yPercent(float pixels) { return pixels * 100f / Math.max(1, y(100f)); }
    public static float widthPercent(float pixels) { return pixels * 100f / Math.max(1, width(100f)); }
    public static float heightPercent(float pixels) { return pixels * 100f / Math.max(1, height(100f)); }
}

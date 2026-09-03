package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.FloatBuffer;
import java.util.LinkedList;

public class Utils {

    private static final LinkedList<Integer> guiScales = new LinkedList<>();
    //Labymod compatibility
    private static final FloatBuffer projectionMatrixOld = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer modelviewMatrixOld = BufferUtils.createFloatBuffer(16);
    private static ScaledResolution lastScale = GuiScreenUtils.getScaledResolution();

    public static boolean overlayShouldRender(Integer type, boolean... booleans) {
        return overlayShouldRender(false, type, 9, booleans);
    }

    public static boolean overlayShouldRender(boolean hideOnf3, Integer type, Integer checkType, boolean... booleans) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        for (boolean aBoolean : booleans) if (!aBoolean) return false;
        if (hideOnf3) {
            if (mc.gameSettings.showDebugInfo || (mc.gameSettings.keyBindPlayerList.isKeyDown() && (!mc.isIntegratedServerRunning() || mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1))) {
                return false;
            }
        }
        return ((type == null && ModCompat.isModLoaded("labymod")) || type == checkType);
    }

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len / (float) strLen;
        factor = Math.min(1, factor);

        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int colour, float factor) {
        GlStateManagerCompat.scale(factor, factor, 1);
        fr.drawString(str, x / factor, y / factor, colour, shadow);
        GlStateManagerCompat.scale(1 / factor, 1 / factor, 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);
        vb.pos(x, y + height, 0.0D).tex(uMin, vMax).endVertex();
        vb.pos(x + width, y + height, 0.0D).tex(uMax, vMax).endVertex();
        vb.pos(x + width, y, 0.0D).tex(uMax, vMin).endVertex();
        vb.pos(x, y, 0.0D).tex(uMin, vMin).endVertex();
        vb.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManagerCompat.disableBlend();
    }

    public static void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1, filter);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
    }

    public static void resetGuiScale() {
        guiScales.clear();
    }

    public static ScaledResolution peekGuiScale() {
        return lastScale;
    }

    public static ScaledResolution pushGuiScale(int scale) {
        if (guiScales.isEmpty()) {
            if (ModCompat.isModLoaded("labymod")) {
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixOld);
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewMatrixOld);
            }
        }

        if (scale < 0) {
            if (!guiScales.isEmpty()) {
                guiScales.pop();
            }
        } else {
            if (scale == 0) {
                guiScales.push(MinecraftCompat.getMinecraft().gameSettings.guiScale);
            } else {
                guiScales.push(scale);
            }
        }

        int newScale = !guiScales.isEmpty() ? Math.max(0, Math.min(4, guiScales.peek())) : MinecraftCompat.getMinecraft().gameSettings.guiScale;
        if (newScale == 0) newScale = MinecraftCompat.getMinecraft().gameSettings.guiScale;

        int oldScale = MinecraftCompat.getMinecraft().gameSettings.guiScale;
        MinecraftCompat.getMinecraft().gameSettings.guiScale = newScale;
        ScaledResolution scaledresolution = GuiScreenUtils.getScaledResolution();
        MinecraftCompat.getMinecraft().gameSettings.guiScale = oldScale;

        if (!guiScales.isEmpty()) {
            GlStateManagerCompat.viewport(0, 0, MinecraftCompat.getMinecraft().displayWidth, MinecraftCompat.getMinecraft().displayHeight);
            GlStateManagerCompat.matrixMode(GL11.GL_PROJECTION);
            GlStateManagerCompat.loadIdentity();
            GlStateManagerCompat.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
            GlStateManagerCompat.matrixMode(GL11.GL_MODELVIEW);
            GlStateManagerCompat.loadIdentity();
            GlStateManagerCompat.translate(0.0F, 0.0F, -2000.0F);
        } else {
            if (ModCompat.isModLoaded("labymod") && projectionMatrixOld.limit() > 0 && modelviewMatrixOld.limit() > 0) {
                GlStateManagerCompat.matrixMode(GL11.GL_PROJECTION);
                GL11.glLoadMatrix(projectionMatrixOld);
                GlStateManagerCompat.matrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadMatrix(modelviewMatrixOld);
            } else {
                GlStateManagerCompat.matrixMode(GL11.GL_PROJECTION);
                GlStateManagerCompat.loadIdentity();
                GlStateManagerCompat.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
                GlStateManagerCompat.matrixMode(GL11.GL_MODELVIEW);
                GlStateManagerCompat.loadIdentity();
                GlStateManagerCompat.translate(0.0F, 0.0F, -2000.0F);
            }
        }

        lastScale = scaledresolution;
        return scaledresolution;
    }

    public static void drawStringCentered(String str, FontRenderer fr, float x, float y, boolean shadow, int colour) {
        int strLen = fr.getStringWidth(str);

        float x2 = x - strLen / 2f;
        float y2 = y - fr.FONT_HEIGHT / 2f;

        GL11.glTranslatef(x2, y2, 0);
        fr.drawString(str, 0, 0, colour, shadow);
        GL11.glTranslatef(-x2, -y2, 0);
    }

    public static void copyToClipboard(String str) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
    }

    public static String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        if (sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /** Like {@link #formatDuration(long)} but shows "<1m" for sub-minute values. */
    public static String formatDuration(long ms, boolean underOneMinuteAsLess) {
        if (underOneMinuteAsLess && ms > 0 && ms < 60_000L) return "<1m";
        return formatDuration(ms);
    }

    public static String formatClock(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    private static final char[] SUFFIXES = {'\0', 'k', 'M', 'B', 'T'};
    public static String shortNumberFormat(double num, int iteration) {
        boolean negative = num < 0;
        double n = Math.abs(num);

        if (n < 1000) {
            if (iteration == 0) {
                return String.valueOf((int) n);
            }
            double d = ((long) (n * 10)) / 10.0;
            boolean isRound = (d * 10) % 10 == 0;
            String number = isRound || d > 9.99 ? String.valueOf((int) d) : String.valueOf(d);
            return number + SUFFIXES[iteration];
        }
        return (negative ? "-" : "") + shortNumberFormat(n / 1000.0, iteration + 1);
    }
}
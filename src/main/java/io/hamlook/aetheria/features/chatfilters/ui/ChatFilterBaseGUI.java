package io.hamlook.aetheria.features.chatfilters.ui;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

abstract class ChatFilterBaseGUI extends GuiScreen {

    protected static final double BTN_W = 90;
    protected static final double BTN_H = 28;
    protected static final double TOGGLE_W = 75;
    protected static final double TOGGLE_H = 26;

    protected float configScale() {
        return ATHRConfig.feature.chat.chatFilterConfig.uiScale;
    }

    protected int getScaledX(double entry) {
        return (int) (ResolutionUtils.getXStatic(1) * entry * 2.0 * configScale());
    }

    protected int getScaledY(double entry) {
        return (int) (ResolutionUtils.getYStatic(1) * entry * 2.0 * configScale());
    }

    protected void startScissor(int x, int y, int width, int height) {
        ScaledResolution res = new ScaledResolution(mc);
        int scale = res.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    protected void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    protected void drawScrollbar(int trackX, int listY, int listH, int totalH, int scrollY, int maxScroll) {
        if (maxScroll <= 0) return;
        int barW = getScaledX(5);
        Gui.drawRect(trackX, listY, trackX + barW, listY + listH, 0x44000000);
        int thumbH = Math.max(getScaledY(20), listH * listH / Math.max(listH, totalH));
        int thumbY = listY + (int) ((float) scrollY / maxScroll * (listH - thumbH));
        Gui.drawRect(trackX, thumbY, trackX + barW, thumbY + thumbH, 0xFFAAAAAA);
    }

    protected boolean tryStartScrollbarDrag(int mouseX, int mouseY, int trackX, int listY, int listH, int totalH, int scrollY, int maxScroll) {
        if (maxScroll <= 0) return false;
        int barW = getScaledX(5);
        int thumbH = Math.max(getScaledY(20), listH * listH / Math.max(listH, totalH));
        int thumbY = listY + (int) ((float) scrollY / maxScroll * (listH - thumbH));
        return mouseX >= trackX - getScaledX(4) && mouseX <= trackX + barW + getScaledX(4) && mouseY >= thumbY && mouseY <= thumbY + thumbH;
    }

    protected int clampScroll(int scroll, int maxScroll) {
        return Math.max(0, Math.min(scroll, maxScroll));
    }

    protected int applyDraggedScroll(int mouseY, int dragMode, int dragStartY, int dragStartScroll, int currentScroll, int layoutH, int totalH, int maxScroll) {
        int result = currentScroll;

        switch (dragMode) {
            case 1: {
                int thumbH = Math.max(getScaledY(20), layoutH * layoutH / Math.max(layoutH, totalH));

                result = dragStartScroll + (int) ((mouseY - dragStartY) * (maxScroll == 0 ? 0 : maxScroll / (float) (layoutH - thumbH)));
                break;
            }
            case 2:
                result = dragStartScroll - (mouseY - dragStartY);
                break;
        }
        return clampScroll(result, maxScroll);
    }
}
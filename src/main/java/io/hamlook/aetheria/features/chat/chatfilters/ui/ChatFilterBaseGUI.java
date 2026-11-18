package io.hamlook.aetheria.features.chat.chatfilters.ui;

import io.hamlook.aetheria.ThemeContext;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
abstract class ChatFilterBaseGUI extends AetheriaBaseScreen {

    private LerpUtils.LerpingFloat openFade;
    protected final ThemeContext.HitList buttonHits = new ThemeContext.HitList();

    protected static final double BTN_H = ThemeContext.BTN_H;
    protected static final double TOGGLE_H = ThemeContext.BTN_H;
    protected static final double FIELD_H = ThemeContext.FIELD_H;

    protected float configScale() {
        return ATHRConfig.feature.chat.chatFilterConfig.uiScale;
    }

    protected float openProgress() {
        if (openFade == null) {
            openFade = new LerpUtils.LerpingFloat(0f, 180);
            openFade.resetTimer();
        }
        openFade.setTarget(1f);
        openFade.tick();
        return LerpUtils.sigmoidZeroOne(openFade.getValue());
    }

    protected int buttonWidth(FontRenderer font, String label) {
        return ThemeContext.buttonWidth(font, label);
    }

    protected int buttonHeight() {
        return ThemeContext.FIELD_H;
    }

    protected int compactButtonHeight() {
        return ThemeContext.BTN_H;
    }

    protected int smallButtonHeight() {
        return ThemeContext.SMALL_H;
    }

    protected boolean clickThemeButton(int mouseX, int mouseY) {
        return buttonHits.click(mouseX, mouseY);
    }

    protected void beginThemeButtons(int mouseX, int mouseY) {
        buttonHits.clear(mouseX, mouseY);
    }

    /**
     * Re-runs this screen's layout pass. Legacy calls vanilla initGui;
     * modern has no such method, so the layout hook runs directly.
     */
    protected void refreshGui() {
        //#if MC < 1.21
        initGui();
        //#else
        //$$ onInitGui();
        //#endif
    }

    protected int getScaledX(double entry) {
        return (int) (ResolutionUtils.getXStatic(1) * entry * configScale());
    }

    protected int getScaledY(double entry) {
        return (int) (ResolutionUtils.getYStatic(1) * entry * configScale());
    }

    protected void startScissor(int x, int y, int width, int height) {
        int scale = GuiScreenUtils.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GlStateManagerCompat.scissor(x * scale, GuiScreenUtils.getDisplayHeight() - (y + height) * scale, width * scale, height * scale);
    }

    protected void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    protected void drawScrollbar(int trackX, int listY, int listH, int totalH, int scrollY, int maxScroll) {
        if (maxScroll <= 0) return;
        int barW = getScaledX(5);
        ThemeContext.rect(trackX, listY, barW, listH, ThemeContext.INSET);
        int thumbH = Math.max(getScaledY(20), listH * listH / Math.max(listH, totalH));
        int thumbY = listY + (int) ((float) scrollY / maxScroll * (listH - thumbH));
        ThemeContext.rect(trackX, thumbY, barW, thumbH, ThemeContext.ACCENT);
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

package io.hamlook.aetheria.network;

import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public abstract class NetworkNoticeScreen extends GuiScreen {

    protected static final int PANEL_W = 400;
    protected static final int PANEL_H = 280;
    protected static final int NAV_W = 80;
    protected static final int NAV_H = 22;
    protected static final int NAV_PAD = 14;
    protected static final float ANIM_SPEED = 0.12f;

    protected int px, py;
    protected float animOffset;

    @Override
    public void initGui() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
    }

    @Override
    public void updateScreen() {
        if (animOffset != 0) {
            animOffset *= (1f - ANIM_SPEED * 3);
            if (Math.abs(animOffset) < 0.5f) animOffset = 0;
        }
    }

    protected abstract void drawPanelBackground();

    protected abstract void drawPageContent(int mouseX, int mouseY, int slide);

    protected abstract void drawNavigation(int mouseX, int mouseY);

    protected void drawTooltips(int mouseX, int mouseY) {
    }

    protected abstract boolean handleClick(int mouseX, int mouseY, int mouseButton);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(1f, 1f, 1f, 1f);

        drawPanelBackground();

        float slide = animOffset;
        float alpha = 1f - Math.min(1f, Math.abs(slide) / (PANEL_W * 0.4f));

        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px * scale, mc.displayHeight - (py + PANEL_H) * scale, PANEL_W * scale, PANEL_H * scale);

        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        drawPageContent(mouseX, mouseY, (int) slide);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.color(1f, 1f, 1f, 1f);

        drawNavigation(mouseX, mouseY);
        drawTooltips(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (animOffset != 0) return;
        if (!handleClick(mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void drawNavBtn(int x, int y, String label, int color, boolean hovered) {
        RenderUtils.drawFloatingRectDark(x, y, NAV_W, NAV_H, false);
        if (hovered) Gui.drawRect(x, y, x + NAV_W, y + NAV_H, 0x18FFFFFF);
        drawCenteredString(fontRendererObj, label, x + NAV_W / 2, y + (NAV_H - fontRendererObj.FONT_HEIGHT) / 2, color);
    }

    protected void drawWrapped(String text, int cx, int y, int color) {
        for (String line : text.split("\n")) {
            StringBuilder seg = new StringBuilder();
            for (String word : line.split(" ")) {
                String test = seg.length() == 0 ? word : seg + " " + word;
                if (fontRendererObj.getStringWidth(test) > 340) {
                    drawCenteredString(fontRendererObj, seg.toString(), cx, y, color);
                    y += fontRendererObj.FONT_HEIGHT + 2;
                    seg = new StringBuilder(word);
                } else {
                    seg = new StringBuilder(test);
                }
            }
            if (seg.length() > 0) {
                drawCenteredString(fontRendererObj, seg.toString(), cx, y, color);
                y += fontRendererObj.FONT_HEIGHT + 2;
            }
        }
    }

    protected boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    protected int navY() {
        return py + PANEL_H - NAV_H - NAV_PAD;
    }
}
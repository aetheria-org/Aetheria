package io.hamlook.aetheria.network;

import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import org.lwjgl.opengl.GL11;

public abstract class NetworkNoticeScreen extends AetheriaBaseScreen {

    protected static final int PANEL_W = 400;
    protected static final int PANEL_H = 280;
    protected static final int NAV_W = 80;
    protected static final int NAV_H = 22;
    protected static final int NAV_PAD = 14;
    protected static final float ANIM_SPEED = 0.12f;

    protected int px, py;
    protected float animOffset;

    @Override
    public void onInitGui() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
    }

    //#if MC < 1.21
    @Override
    public void updateScreen() {
        tickNotice();
    }
    //#else
    //$$ @Override
    //$$ public void tick() {
    //$$     tickNotice();
    //$$ }
    //#endif

    private void tickNotice() {
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
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        //#if MC < 1.21
        drawDefaultBackground();
        //#else
        //$$ // modern background handled via Screen.renderBackground
        //#endif
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        drawPanelBackground();

        float slide = animOffset;
        float alpha = 1f - Math.min(1f, Math.abs(slide) / (PANEL_W * 0.4f));

        int scale = GuiScreenUtils.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GlStateManagerCompat.scissor(px * scale, GuiScreenUtils.getDisplayHeight() - (py + PANEL_H) * scale, PANEL_W * scale, PANEL_H * scale);

        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.color(1f, 1f, 1f, alpha);
        drawPageContent(mouseX, mouseY, (int) slide);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        drawNavigation(mouseX, mouseY);
        drawTooltips(mouseX, mouseY);
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (animOffset != 0) return;
        if (!handleClick(mouseX, mouseY, mouseButton)) {
        }
    }

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        if (KeybindHelper.isEscapeKey(keyCode)) return;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void drawNavBtn(int x, int y, String label, int color, boolean hovered) {
        RenderUtils.drawFloatingRectDark(x, y, NAV_W, NAV_H, false);
        if (hovered) DrawContextUtils.fill(x, y, x + NAV_W, y + NAV_H, 0x18FFFFFF);
        TextRenderUtils.drawCenteredStringScaleAware(label, x + NAV_W / 2, y + (NAV_H - MinecraftCompat.getFontRenderer().FONT_HEIGHT) / 2, color, 1f, false);
    }

    protected void drawWrapped(String text, int cx, int y, int color) {
        for (String line : text.split("\n")) {
            StringBuilder seg = new StringBuilder();
            for (String word : line.split(" ")) {
                String test = seg.length() == 0 ? word : seg + " " + word;
                if (DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), test) > 340) {
                    TextRenderUtils.drawCenteredStringScaleAware(seg.toString(), cx, y, color, 1f, false);
                    y += MinecraftCompat.getFontRenderer().FONT_HEIGHT + 2;
                    seg = new StringBuilder(word);
                } else {
                    seg = new StringBuilder(test);
                }
            }
            if (seg.length() > 0) {
                TextRenderUtils.drawCenteredStringScaleAware(seg.toString(), cx, y, color, 1f, false);
                y += MinecraftCompat.getFontRenderer().FONT_HEIGHT + 2;
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
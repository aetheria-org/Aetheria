package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

public abstract class Overlay {

    protected static final int LINE_HEIGHT = 10;
    protected static final int PADDING = 3;

    protected int lastW;
    protected int lastH;

    protected Overlay(int defaultW, int defaultH) {
        this.lastW = defaultW;
        this.lastH = defaultH;
    }

    public static void drawRoundedRect(int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w - x, h - y) / 2);
        if (r <= 0) {
            Gui.drawRect(x, y, w, h, color);
            return;
        }

        Gui.drawRect(x + r, y, w - r, h, color);
        Gui.drawRect(x, y + r, x + r, h - r, color);
        Gui.drawRect(w - r, y + r, w, h - r, color);

        for (int i = 0; i < r; i++) {
            int cut = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - (double) (r - i - 1) * (r - i - 1))));
            Gui.drawRect(x + i, y + cut, x + i + 1, y + r, color);
            Gui.drawRect(w - i - 1, y + cut, w - i, y + r, color);
            Gui.drawRect(x + i, h - r, x + i + 1, h - cut, color);
            Gui.drawRect(w - i - 1, h - r, w - i, h - cut, color);
        }
    }

    public int getOverlayWidth() {
        return lastW;
    }

    public int getOverlayHeight() {
        return lastH;
    }

    public abstract List<String> getLines(boolean preview);

    public abstract Position getPosition();

    public abstract float getScale();

    public abstract int getBgColor();

    public abstract int getCornerRadius();

    protected abstract boolean isEnabled();

    protected boolean extraGuard() {
        return true;
    }

    @SubscribeEvent
    public final void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (ATHRConfig.feature == null || !isEnabled()) return;
        render(false);
    }

    public void render(boolean preview) {
        if (!preview && (OverlayUtils.shouldHide() || !extraGuard())) return;

        List<String> lines = getLines(preview);
        if (lines == null || lines.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float scale = getScale();

        int w = getBaseWidth();
        for (String line : lines)
            w = Math.max(w, mc.fontRendererObj.getStringWidth(line) + PADDING * 2);
        int h = lines.size() * LINE_HEIGHT + PADDING * 2;
        lastW = w;
        lastH = h;

        ScaledResolution sr = new ScaledResolution(mc);
        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (w * scale));
        int y = pos.getAbsY(sr, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) drawRoundedRect(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), bgColor);

        int dy = 0;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line, 0, dy, 0xFFFFFF);
            dy += LINE_HEIGHT;
        }

        GL11.glPopMatrix();
    }

    protected int getBaseWidth() {
        return 20;
    }
}
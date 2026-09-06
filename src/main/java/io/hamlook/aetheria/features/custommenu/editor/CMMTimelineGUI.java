package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class CMMTimelineGUI extends AetheriaBaseScreen {
    private final CMMElement element; private final GuiScreen parent; private boolean playing; private long started; private float scrub;
    public CMMTimelineGUI(CMMElement element, GuiScreen parent) { this.element = element; this.parent = parent; }
    private int left() { return PercentageUtils.x(7f); }
    private int timelineWidth() { return Math.max(1, PercentageUtils.width(86f)); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); }
    @Override protected void onDrawScreen(int mx, int my, float pt) {
        ScreenHelper.updateScreenDimensions(width, height); drawRect(0, 0, width, height, 0xF0121218); int l = left(), t = PercentageUtils.centeredY(10f), w = timelineWidth(), trackH = PercentageUtils.height(10f);
        TextRenderUtils.drawCenteredStringScaleAware("Animation Timeline", PercentageUtils.centerX(), t - PercentageUtils.height(9f), 0xFFFFFFFF, 2f, true);
        drawRect(l, t, l + w, t + trackH, 0xFF262630); drawRect(l, t + trackH/2, l + w, t + trackH/2 + 1, 0xFF6EA7C4);
        int duration = element.animation == null ? 250 : Math.max(1, element.animation.durationMs);
        float progress = playing ? ((System.currentTimeMillis() - started) % duration) / (float) duration : scrub;
        drawRect(l + Math.round(progress * w) - 2, t - PercentageUtils.height(2f), l + Math.round(progress * w) + 2, t + trackH + PercentageUtils.height(2f), 0xFFFFFFFF);
        TextRenderUtils.drawCenteredStringScaleAware("Click timeline to scrub | Space: play/pause | Escape: return", PercentageUtils.centerX(), t + PercentageUtils.height(17f), 0xFFB8B8C8, 1f, false);
    }
    @Override protected void onMouseClicked(int mx, int my, int button) { int l=left(), t=PercentageUtils.centeredY(10f), w=timelineWidth(), trackH=PercentageUtils.height(10f); if (button == 0 && my >= t-PercentageUtils.height(2f) && my <= t+trackH+PercentageUtils.height(2f) && mx>=l&&mx<=l+w) { playing = false; scrub = Math.max(0f, Math.min(1f, (mx - l) / (float) w)); } }
    @Override protected void onKeyTyped(char c, int key) { if (key == Keyboard.KEY_SPACE) { playing = !playing; started = System.currentTimeMillis(); } if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent); }
}

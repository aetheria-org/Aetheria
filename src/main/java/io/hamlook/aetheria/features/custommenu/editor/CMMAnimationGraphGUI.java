package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.animation.AnimationCurve;
import io.hamlook.aetheria.features.custommenu.animation.AnimationType;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Small normalized curve editor; points are persisted directly on the selected element.
 */
public class CMMAnimationGraphGUI extends AetheriaBaseScreen {
    private final CMMElement element;
    private final GuiScreen parent;
    private int selectedPoint = -1;
    private int left, top, graphW = 500, graphH = 280;

    public CMMAnimationGraphGUI(CMMElement element, GuiScreen parent) {
        this.element = element;
        this.parent = parent;
    }

    @Override
    public void onResize(net.minecraft.client.Minecraft mc, int w, int h) {
        super.onResize(mc, w, h);
        ScreenHelper.updateScreenDimensions(w, h);
        graphW = PercentageUtils.width(58.55f);
        graphH = PercentageUtils.height(58.33f);
    }

    @Override
    protected void onDrawScreen(int mx, int my, float partialTicks) {
        ScreenHelper.updateScreenDimensions(width, height);
        graphW = PercentageUtils.width(58.55f);
        graphH = PercentageUtils.height(58.33f);
        drawRect(0, 0, width, height, 0xF0121218);
        left = PercentageUtils.centeredX(58.55f);
        top = PercentageUtils.centeredY(58.33f);
        AnimationType[] presets = {AnimationType.NONE, AnimationType.FADE, AnimationType.EASE_IN, AnimationType.EASE_OUT, AnimationType.EASE_IN_OUT, AnimationType.CUSTOM};
        int presetW = PercentageUtils.width(7.95f), presetGap = PercentageUtils.width(0.47f), presetLeft = PercentageUtils.centeredX(50.5f), presetTop = top - PercentageUtils.height(15f), presetH = PercentageUtils.height(5f);
        for (int i = 0; i < presets.length; i++) {
            int x = presetLeft + i * (presetW + presetGap);
            boolean h = mx >= x && mx < x + presetW && my >= presetTop && my < presetTop + presetH;
            drawRect(x, presetTop, x + presetW, presetTop + presetH, h ? 0xFF3B6982 : 0xFF292932);
            TextRenderUtils.drawCenteredStringScaleAware(presets[i].name(), x + presetW / 2f, presetTop + presetH / 2f, 0xFFFFFFFF, .55f, false);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Custom Animation Curve", PercentageUtils.centerX(), top - PercentageUtils.height(7f), 0xFFFFFFFF, 2f, true);
        drawRect(left, top, left + graphW, top + graphH, 0xFF20202A);
        drawRect(left, top + graphH - 1, left + graphW, top + graphH, 0xFF6EA7C4);
        drawRect(left, top, left + 1, top + graphH, 0xFF6EA7C4);
        AnimationCurve curve = getCurve();
        for (int i = 1; i < curve.points.size(); i++) {
            AnimationCurve.Point a = curve.points.get(i - 1), b = curve.points.get(i);
            drawRect(px(a.x), py(a.y), px(b.x) + 1, py(a.y) + 1, 0xFF65C8FF);
        }
        for (int i = 0; i < curve.points.size(); i++) {
            AnimationCurve.Point p = curve.points.get(i);
            drawRect(px(p.x) - 4, py(p.y) - 4, px(p.x) + 5, py(p.y) + 5, i == selectedPoint ? 0xFFFFFFFF : 0xFF65C8FF);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Click empty graph to add a point | Right-click a point to remove | Escape to return", PercentageUtils.centerX(), top + graphH + PercentageUtils.height(5f), 0xFFB8B8C8, 1f, false);
        TextRenderUtils.drawCenteredStringScaleAware("Type: " + element.animation.type + " (press E to use custom curve)", PercentageUtils.centerX(), top + graphH + PercentageUtils.height(9f), 0xFFE0E0E0, 1f, false);
    }

    private AnimationCurve getCurve() {
        if (element.animation == null)
            element.animation = new io.hamlook.aetheria.features.custommenu.animation.CMMAnimation();
        if (element.animation.customCurve == null) element.animation.customCurve = new AnimationCurve();
        return element.animation.customCurve;
    }

    private int px(float x) {
        return left + Math.round(Math.max(0f, Math.min(1f, x)) * graphW);
    }

    private int py(float y) {
        return top + graphH - Math.round(Math.max(0f, Math.min(1f, y)) * graphH);
    }

    @Override
    protected void onMouseClicked(int mx, int my, int button) {
        AnimationCurve curve = getCurve();
        int presetW = PercentageUtils.width(7.95f), presetGap = PercentageUtils.width(0.47f), presetLeft = PercentageUtils.centeredX(50.5f), presetTop = top - PercentageUtils.height(15f), presetH = PercentageUtils.height(5f);
        if (button == 0 && my >= presetTop && my < presetTop + presetH) {
            int index = (mx - presetLeft) / (presetW + presetGap);
            AnimationType[] presets = {AnimationType.NONE, AnimationType.FADE, AnimationType.EASE_IN, AnimationType.EASE_OUT, AnimationType.EASE_IN_OUT, AnimationType.CUSTOM};
            if (index >= 0 && index < presets.length) element.animation.type = presets[index];
            return;
        }
        if (button == 1) {
            for (int i = curve.points.size() - 1; i > 0 && i < curve.points.size() - 1; i--)
                if (Math.abs(px(curve.points.get(i).x) - mx) < 8 && Math.abs(py(curve.points.get(i).y) - my) < 8) {
                    curve.points.remove(i);
                    return;
                }
            return;
        }
        selectedPoint = -1;
        for (int i = 0; i < curve.points.size(); i++)
            if (Math.abs(px(curve.points.get(i).x) - mx) < 8 && Math.abs(py(curve.points.get(i).y) - my) < 8) {
                selectedPoint = i;
                return;
            }
        if (mx >= left && mx <= left + graphW && my >= top && my <= top + graphH)
            curve.points.add(new AnimationCurve.Point((mx - left) / (float) graphW, 1f - (my - top) / (float) graphH));
    }

    @Override
    protected void onMouseClickMove(int mx, int my, int button, long time) {
        if (selectedPoint < 0 || selectedPoint >= getCurve().points.size()) return;
        AnimationCurve.Point p = getCurve().points.get(selectedPoint);
        p.x = Math.max(0f, Math.min(1f, (mx - left) / (float) graphW));
        p.y = Math.max(0f, Math.min(1f, 1f - (my - top) / (float) graphH));
    }

    @Override
    protected void onKeyTyped(char c, int key) {
        if (key == Keyboard.KEY_E) element.animation.type = AnimationType.CUSTOM;
        if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent);
    }
}

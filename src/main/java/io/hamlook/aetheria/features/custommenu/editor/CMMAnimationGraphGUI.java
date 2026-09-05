package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.animation.AnimationCurve;
import io.hamlook.aetheria.features.custommenu.animation.AnimationType;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Small normalized curve editor; points are persisted directly on the selected element. */
public class CMMAnimationGraphGUI extends AetheriaBaseScreen {
    private final CMMElement element;
    private final GuiScreen parent;
    private int selectedPoint = -1;
    private int left, top, graphW = 500, graphH = 280;

    public CMMAnimationGraphGUI(CMMElement element, GuiScreen parent) { this.element = element; this.parent = parent; }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); graphW=ScreenHelper.getStaticWidth(500); graphH=ScreenHelper.getStaticHeight(280); }

    @Override protected void onDrawScreen(int mx, int my, float partialTicks) {
        ScreenHelper.updateScreenDimensions(width, height); graphW=ScreenHelper.getStaticWidth(500); graphH=ScreenHelper.getStaticHeight(280); drawRect(0, 0, width, height, 0xF0121218);
        left = width / 2 - graphW / 2; top = height / 2 - graphH / 2;
        AnimationType[] presets = {AnimationType.NONE, AnimationType.FADE, AnimationType.EASE_IN, AnimationType.EASE_OUT, AnimationType.EASE_IN_OUT, AnimationType.CUSTOM};
        for (int i=0;i<presets.length;i++) { int x=width/2-210+i*72; boolean h=mx>=x&&mx<x+68&&my>=top-72&&my<top-48; drawRect(x,top-72,x+68,top-48,h?0xFF3B6982:0xFF292932); TextRenderUtils.drawCenteredStringScaleAware(presets[i].name(),x+34,top-60,0xFFFFFFFF,.55f,false); }
        TextRenderUtils.drawCenteredStringScaleAware("Custom Animation Curve", width / 2f, top - 34, 0xFFFFFFFF, 2f, true);
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
        TextRenderUtils.drawCenteredStringScaleAware("Click empty graph to add a point | Right-click a point to remove | Escape to return", width / 2f, top + graphH + 24, 0xFFB8B8C8, 1f, false);
        TextRenderUtils.drawCenteredStringScaleAware("Type: " + element.animation.type + " (press E to use custom curve)", width / 2f, top + graphH + 44, 0xFFE0E0E0, 1f, false);
    }

    private AnimationCurve getCurve() { if (element.animation == null) element.animation = new io.hamlook.aetheria.features.custommenu.animation.CMMAnimation(); if (element.animation.customCurve == null) element.animation.customCurve = new AnimationCurve(); return element.animation.customCurve; }
    private int px(float x) { return left + Math.round(Math.max(0f, Math.min(1f, x)) * graphW); }
    private int py(float y) { return top + graphH - Math.round(Math.max(0f, Math.min(1f, y)) * graphH); }

    @Override protected void onMouseClicked(int mx, int my, int button) {
        AnimationCurve curve = getCurve();
        if (button == 0 && my >= top - 72 && my < top - 48) { int index=(mx-(width/2-210))/72; AnimationType[] presets={AnimationType.NONE,AnimationType.FADE,AnimationType.EASE_IN,AnimationType.EASE_OUT,AnimationType.EASE_IN_OUT,AnimationType.CUSTOM}; if(index>=0&&index<presets.length) element.animation.type=presets[index]; return; }
        if (button == 1) { for (int i = curve.points.size() - 1; i > 0 && i < curve.points.size() - 1; i--) if (Math.abs(px(curve.points.get(i).x) - mx) < 8 && Math.abs(py(curve.points.get(i).y) - my) < 8) { curve.points.remove(i); return; } return; }
        selectedPoint = -1;
        for (int i = 0; i < curve.points.size(); i++) if (Math.abs(px(curve.points.get(i).x) - mx) < 8 && Math.abs(py(curve.points.get(i).y) - my) < 8) { selectedPoint = i; return; }
        if (mx >= left && mx <= left + graphW && my >= top && my <= top + graphH) curve.points.add(new AnimationCurve.Point((mx - left) / (float) graphW, 1f - (my - top) / (float) graphH));
    }
    @Override protected void onMouseClickMove(int mx, int my, int button, long time) { if (selectedPoint < 0 || selectedPoint >= getCurve().points.size()) return; AnimationCurve.Point p = getCurve().points.get(selectedPoint); p.x = Math.max(0f, Math.min(1f, (mx - left) / (float) graphW)); p.y = Math.max(0f, Math.min(1f, 1f - (my - top) / (float) graphH)); }
    @Override protected void onKeyTyped(char c, int key) { if (key == Keyboard.KEY_E) element.animation.type = AnimationType.CUSTOM; if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent); }
}

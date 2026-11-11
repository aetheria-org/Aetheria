package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.qol.TooltipsConfig;
import io.hamlook.aetheria.events.ASMMouseEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;

@RegisterEvents
public class TooltipHandler {

    private static final LerpUtils.LerpingFloat SCROLL = new LerpUtils.LerpingFloat(0, 50);
    private static final LerpUtils.LerpingFloat ZOOM = new LerpUtils.LerpingFloat(1, 200);

    private static int contentKey;
    private static int drawCounter;
    private static int lastSeenCounter;
    private static boolean transformActive;
    private static float centerX;
    private static float centerY;

    private TooltipHandler() {
    }

    public static void onTooltipDrawn(float anchorX, float top, float height, int width) {
        int key = (int) (height * 31 + width);
        if (key != contentKey) {
            contentKey = key;
            SCROLL.setValue(0f);
            ZOOM.setValue(1f);
        }
        centerX = anchorX + width / 2f;
        centerY = top + height / 2f;
        drawCounter++;
    }

    public static void tick() {
        TooltipsConfig config = config();
        ZOOM.setTimeToReachTarget(config == null ? 200 : config.scale.zoomDurationMs);
        int scrollDuration = config == null ? 50 : config.scroll.scrollDurationMs;
        SCROLL.setTimeToReachTarget(scrollDuration);
        if (scrollDuration > 0) {
            SCROLL.tick();
        } else {
            SCROLL.setValue(SCROLL.getTarget());
        }
        ZOOM.tick();
        if (config != null && !config.scale.zoomSmooth) ZOOM.setValue(ZOOM.getTarget());
    }

    public static void pushTransform() {
        float scale = effectiveScale();
        if (scale == 1f && SCROLL.getValue() == 0f) return;
        transformActive = true;
        DrawContextUtils.pushMatrix();
        DrawContextUtils.translate(0f, -SCROLL.getValue(), 0f);
        DrawContextUtils.translate(centerX, centerY, 0f);
        DrawContextUtils.scale(scale, scale, 1f);
        DrawContextUtils.translate(-centerX, -centerY, 0f);
    }

    public static void popTransform() {
        if (!transformActive) return;
        transformActive = false;
        DrawContextUtils.popMatrix();
    }

    public static boolean isScaleKeyHeld() {
        TooltipsConfig config = config();
        return config != null && KeybindHelper.isKeyDown(config.scale.tooltipScaleKey);
    }

    static float effectiveScale() {
        TooltipsConfig config = config();
        if (config == null || !config.scale.tooltipScaling) return 1f;
        return config.scale.tooltipScale * ZOOM.getValue();
    }

    private static boolean tooltipDisplayed() {
        return drawCounter != lastSeenCounter;
    }

    private static TooltipsConfig config() {
        return ATHRConfig.feature == null || ATHRConfig.feature.qol.tooltips == null ? null : ATHRConfig.feature.qol.tooltips;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @HandleEvent(priority = HandleEvent.LOW)
    public void onMouseInput(ASMMouseEvent event) {
        if (event.dWheel == 0 || !tooltipDisplayed()) return;
        TooltipsConfig config = config();
        if (isScaleKeyHeld()) {
            if (config == null || !config.scale.tooltipScaling) return;
            float step = event.dWheel > 0 ? config.scale.zoomStep : 1f / config.scale.zoomStep;
            float limit = config.scale.zoomLimit;
            ZOOM.setTarget(clamp(ZOOM.getTarget() * step, 1f / limit, limit));
            ZOOM.resetTimer();
            event.cancel();
        } else if (config != null && config.scroll.tooltipScrolling) {
            SCROLL.setTarget(SCROLL.getTarget() + (event.dWheel < 0 ? 1 : -1) * config.scroll.tooltipScrollSpeed);
            SCROLL.resetTimer();
            event.cancel();
        }
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (drawCounter == lastSeenCounter) {
            SCROLL.setValue(0f);
            ZOOM.setValue(1f);
        }
        lastSeenCounter = drawCounter;
        if (!isScaleKeyHeld()) {
            ZOOM.setTarget(1f);
            ZOOM.resetTimer();
        }
    }
}

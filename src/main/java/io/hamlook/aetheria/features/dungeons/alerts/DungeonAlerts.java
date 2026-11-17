package io.hamlook.aetheria.features.dungeons.alerts;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.AlertsConfig;
import io.hamlook.aetheria.events.BloodClearedEvent;
import io.hamlook.aetheria.events.DungeonRoomEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.overlay.SimpleOverlay;
import io.hamlook.aetheria.utils.render.RenderUtils;

@RegisterEvents
public class DungeonAlerts extends SimpleOverlay {

    private static final long DISPLAY_MS = 2000L;
    private static final float SCALE = 2f;

    private static volatile long activeUntilMs;
    private static volatile AlertsConfig.Alert activeCfg;

    @HandleEvent
    public void onBloodCleared(BloodClearedEvent event) {
        AlertsConfig alerts = config();
        if (alerts != null) fire(alerts.blood);
    }

    @HandleEvent
    public void onRoomEvent(DungeonRoomEvent event) {
        AlertsConfig alerts = config();
        if (alerts == null) return;
        fire(event.type == DungeonRoomEvent.Type.ROOM_CLEARED
                ? alerts.roomCleared
                : alerts.secretsCleared);
    }

    private static void fire(AlertsConfig.Alert cfg) {
        if (cfg == null || !cfg.enabled) return;
        activeUntilMs = System.currentTimeMillis() + DISPLAY_MS;
        activeCfg = cfg;
        if (cfg.hasChatText()) ChatUtils.sendMessage(cfg.chatText);
        SoundUtils.playSound(cfg.soundId());
    }

    @Override
    public boolean shouldRender() {
        AlertsConfig.Alert cfg = activeCfg;
        return cfg != null && cfg.enabled && cfg.hasAlertText()
                && System.currentTimeMillis() < activeUntilMs;
    }

    @Override
    public void render(int scaledWidth, int scaledHeight) {
        AlertsConfig.Alert cfg = activeCfg;
        RenderUtils.drawAlertText(scaledWidth, scaledHeight,
                cfg == null ? "" : cfg.alertText, "",
                activeUntilMs, DISPLAY_MS, SCALE, false);
    }

    private static AlertsConfig config() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.dungeons.alerts;
    }
}

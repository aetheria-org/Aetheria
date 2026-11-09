package io.hamlook.aetheria.features.mining.bal;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.mining.BalConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.overlay.SimpleOverlay;
import io.hamlook.aetheria.utils.render.RenderUtils;

@RegisterEvents
public class BalAlertOverlay extends SimpleOverlay {

    private static final long DISPLAY_MS = 3000L;
    private static final String[] SOUND_IDS = {
            "note.pling", "mob.enderdragon.growl", "mob.cat.meow", "random.orb", "random.levelup"
    };

    private static volatile long activeUntilMs;
    private static volatile String displayText = "";

    public static void trigger(String text, BalConfig cfg) {
        activeUntilMs = System.currentTimeMillis() + DISPLAY_MS;
        displayText = text;
        if (cfg != null && cfg.playSound) playAlertSound(cfg.alertSound);
    }

    public static void playAlertSound(int index) {
        SoundUtils.playSound(SOUND_IDS[Math.max(0, Math.min(SOUND_IDS.length - 1, index))]);
    }

    @Override
    public boolean shouldRender() {
        BalConfig cfg = config();
        return cfg != null && cfg.enabled && cfg.showOnScreen && System.currentTimeMillis() < activeUntilMs;
    }

    @Override
    public void render(int scaledWidth, int scaledHeight) {
        BalConfig cfg = config();
        long remainingMs = Math.max(0L, activeUntilMs - System.currentTimeMillis());
        String sub = "§7" + Math.max(1, (int) Math.ceil(remainingMs / 1000.0)) + "s";
        RenderUtils.drawAlertText(scaledWidth, scaledHeight, displayText, sub, activeUntilMs, DISPLAY_MS,
                cfg == null ? 2f : cfg.alertScale, cfg == null || cfg.fadeInOut);
    }

    private static BalConfig config() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.mining.bal;
    }
}

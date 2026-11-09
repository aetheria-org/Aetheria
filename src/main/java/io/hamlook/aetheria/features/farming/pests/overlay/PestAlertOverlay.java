package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestAlertConfig;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.SimpleOverlay;
import io.hamlook.aetheria.utils.render.RenderUtils;


@RegisterEvents
public class PestAlertOverlay extends SimpleOverlay {

    private static final long DISPLAY_MS = 3000L;
    private static final long SOUND_REPEAT_MS = 1000L;
    private static final String[] SOUND_IDS = {
            "note.pling", "mob.enderdragon.growl", "mob.cat.meow", "random.orb", "random.levelup"
    };

    private static volatile boolean armed = true;
    private static volatile long lastRemainingMs = -1L;
    private static volatile long parsedAtMs = 0L;
    private static volatile long activeUntilMs = 0L;
    private static volatile long nextSoundAtMs = 0L;

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        PestAlertConfig cfg = config();
        long now = System.currentTimeMillis();

        if (cfg != null && cfg.enabled && cfg.playSound && now < activeUntilMs && now >= nextSoundAtMs) {
            SoundUtils.playSound(soundId(cfg.alertSound));
            nextSoundAtMs = now + SOUND_REPEAT_MS;
        }

        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getLocalPlayer() == null
                || SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN) {
            armed = true;
            lastRemainingMs = -1L;
            return;
        }
        if (cfg == null || !cfg.enabled) {
            armed = true;
            return;
        }

        long remaining = FarmingApi.getGardenCooldownMs();
        lastRemainingMs = remaining;
        parsedAtMs = now;
        if (remaining < 0) {
            armed = true;
            return;
        }
        long thresholdMs = cfg.alertBelowSeconds * 1000L;
        if (!armed && remaining > thresholdMs) armed = true;
        if (armed && remaining <= thresholdMs) {
            armed = false;
            activeUntilMs = now + DISPLAY_MS;
            nextSoundAtMs = now;
            if (cfg.chatMessage) {
                ChatUtils.sendMessage("§6[ASM] §ePest cooldown under §6" + cfg.alertBelowSeconds
                        + "s§e! §7(" + formatRemaining(remaining) + " left)");
            }
        }
    }

    public static void fireTest() {
        long now = System.currentTimeMillis();
        armed = false;
        lastRemainingMs = 0L;
        parsedAtMs = now;
        activeUntilMs = now + DISPLAY_MS;
        nextSoundAtMs = now;
        PestAlertConfig cfg = config();
        if (cfg != null && cfg.chatMessage) {
            ChatUtils.sendMessage("§6[ASM] §ePest alert test.");
        }
    }

    @Override
    public boolean shouldRender() {
        PestAlertConfig cfg = config();
        if (cfg == null || !cfg.enabled || !cfg.showOnScreen) return false;
        return System.currentTimeMillis() < activeUntilMs;
    }

    @Override
    public void render(int scaledWidth, int scaledHeight) {
        PestAlertConfig cfg = config();
        String cooldown = FarmingApi.getGardenCooldown();
        String stripped = ColorUtils.stripColor(cooldown == null ? "" : cooldown).trim();
        long now = System.currentTimeMillis();
        long remainWindowMs = Math.max(0L, activeUntilMs - now);
        int windowSec = (int) Math.ceil(remainWindowMs / 1000.0);

        String text;
        if (!stripped.isEmpty()) {
            text = cooldown;
        } else {
            long remainMs = lastRemainingMs >= 0
                    ? Math.max(0L, lastRemainingMs - (now - parsedAtMs))
                    : (cfg == null ? 30_000L : cfg.alertBelowSeconds * 1000L);
            text = formatRemaining(remainMs);
        }

        String main = "§c§lPEST ALERT! §r" + text;
        String sub = "§7" + Math.max(1, windowSec) + "s";
        RenderUtils.drawAlertText(scaledWidth, scaledHeight, main, sub, activeUntilMs, DISPLAY_MS,
                cfg == null ? 2f : cfg.alertScale, cfg == null || cfg.fadeInOut);
    }

    private static String formatRemaining(long ms) {
        long totalSec = (ms + 999) / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return m > 0 ? m + "m " + String.format("%02ds", s) : s + "s";
    }

    private static String soundId(int index) {
        return SOUND_IDS[Math.max(0, Math.min(SOUND_IDS.length - 1, index))];
    }

    private static PestAlertConfig config() {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming.pests == null) {
            return null;
        }
        return ATHRConfig.feature.farming.pests.pestAlert;
    }
}

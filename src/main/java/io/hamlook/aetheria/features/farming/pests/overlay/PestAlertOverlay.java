package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestAlertConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.SimpleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class PestAlertOverlay extends SimpleOverlay {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long DISPLAY_MS = 3000L;
    private static final long SOUND_REPEAT_MS = 1000L;
    private static final long FADE_IN_MS = 250L;
    private static final long FADE_OUT_MS = 450L;
    private static final long PULSE_PERIOD_MS = 600L;
    private static final String[] SOUND_IDS = {
            "note.pling", "mob.enderdragon.growl", "mob.cat.meow", "random.orb", "random.levelup"
    };

    private static volatile boolean armed = true;
    private static volatile long lastRemainingMs = -1L;
    private static volatile long parsedAtMs = 0L;
    private static volatile long activeUntilMs = 0L;
    private static volatile long nextSoundAtMs = 0L;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PestAlertConfig cfg = config();
        long now = System.currentTimeMillis();

        if (cfg != null && cfg.enabled && cfg.playSound && now < activeUntilMs && now >= nextSoundAtMs) {
            SoundUtils.playSound(soundId(cfg.alertSound));
            nextSoundAtMs = now + SOUND_REPEAT_MS;
        }

        if (mc.theWorld == null || mc.thePlayer == null
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
    public void render(ScaledResolution sr) {
        PestAlertConfig cfg = config();
        float scale = cfg == null ? 2f : Math.max(1f, cfg.alertScale);
        boolean animate = cfg == null || cfg.fadeInOut;

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

        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        String main = "§c§lPEST ALERT! §r" + text;
        String sub = "§7" + Math.max(1, windowSec) + "s";
        float subScale = Math.max(0.75f, scale * 0.5f);

        float elapsed = DISPLAY_MS - remainWindowMs;
        float envelope = Math.min(1f, elapsed / FADE_IN_MS) * Math.min(1f, remainWindowMs / FADE_OUT_MS);
        double phase = (elapsed % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float pulse = 0.4f + 0.6f * (float) Math.abs(Math.sin(phase * Math.PI));
        float mainAlpha = animate ? clamp(envelope * pulse) : 1f;
        float subAlpha = animate ? clamp(envelope) : 1f;

        int mainW = fr.getStringWidth(main);
        int subW = fr.getStringWidth(sub);
        int gap = 4;
        float blockH = fr.FONT_HEIGHT * scale + gap + fr.FONT_HEIGHT * subScale;
        float yMain = sr.getScaledHeight() * 0.44f - blockH / 2f;
        float xMain = (sr.getScaledWidth() - mainW * scale) / 2f;
        float xSub = (sr.getScaledWidth() - subW * subScale) / 2f;

        if (mainAlpha < 1f || subAlpha < 1f) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(xMain, yMain, 0);
        GlStateManager.scale(scale, scale, 1);
        fr.drawStringWithShadow(main, 0, 0, alpha(mainAlpha));
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(xSub, yMain + fr.FONT_HEIGHT * scale + gap, 0);
        GlStateManager.scale(subScale, subScale, 1);
        fr.drawStringWithShadow(sub, 0, 0, alpha(subAlpha));
        GlStateManager.popMatrix();

        if (mainAlpha < 1f || subAlpha < 1f) {
            GlStateManager.disableBlend();
        }
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int alpha(float a) {
        return ((int) (clamp(a) * 255f)) << 24 | 0xFFFFFF;
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

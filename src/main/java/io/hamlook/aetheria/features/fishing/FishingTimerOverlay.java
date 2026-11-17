package io.hamlook.aetheria.features.fishing;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.entity.Entity;

@RegisterEvents
public class FishingTimerOverlay {

    private boolean alertPlayed = false;

    private boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimer;
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (!isEnabled()) return;

        if (MinecraftCompat.getLocalPlayer() == null) return;

        Entity hook = EntityCompat.getFishHook(MinecraftCompat.getLocalPlayer());
        if (hook == null) {
            alertPlayed = false;
            return;
        }

        float seconds = hook.ticksExisted / 20f;
        int alertTime = ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertTime;

        if (seconds >= alertTime && !alertPlayed) {
            alertPlayed = true;
            playAlertSound();
        } else if (seconds < alertTime) {
            alertPlayed = false;
        }
    }

    private void playAlertSound() {
        SoundUtils.playSound("random.orb", 1f, 2f);
    }

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (!isEnabled()) return;

        if (MinecraftCompat.getLocalPlayer() == null) return;

        Entity hook = EntityCompat.getFishHook(MinecraftCompat.getLocalPlayer());
        if (hook == null) return;

        double x = EntityCompat.getPrevX(hook) + (EntityCompat.getPosX(hook) - EntityCompat.getPrevX(hook)) * event.partialTicks;
        double y = EntityCompat.getPrevY(hook) + (EntityCompat.getPosY(hook) - EntityCompat.getPrevY(hook)) * event.partialTicks + 0.5;
        double z = EntityCompat.getPrevZ(hook) + (EntityCompat.getPosZ(hook) - EntityCompat.getPrevZ(hook)) * event.partialTicks;

        float seconds = hook.ticksExisted / 20f;
        renderText(x, y, z, formatTimerText(seconds), colorForSeconds(seconds));
    }

    private String formatTimerText(float seconds) {
        return String.format("%.2fs", seconds);
    }

    private int colorForSeconds(float seconds) {
        if (ATHRConfig.feature == null) return 0xFFFFFFFF;
        boolean alerted = seconds >= ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertTime;
        return ChromaColour.specialToChromaRGB(alerted ? ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertColor : ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerNormalColor);
    }

    private void renderText(double x, double y, double z, String text, int color) {
        float scale = 0.025f;
        int width = DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), text) / 2;

        WorldRenderUtils.beginBillboard(x, y, z, scale);

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();

        WorldRenderUtils.drawBillboardText(text, -width, 0, color, true);

        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
        WorldRenderUtils.endBillboard();
    }
}
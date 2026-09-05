package io.hamlook.aetheria.features.fishing;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class FishingTimerOverlay {

    private boolean alertPlayed = false;

    private boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimer;
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isEnabled()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null) return;

        EntityFishHook hook = MinecraftCompat.getLocalPlayer().fishEntity;
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

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null) return;

        EntityFishHook hook = MinecraftCompat.getLocalPlayer().fishEntity;
        if (hook == null) return;

        double x = hook.lastTickPosX + (hook.posX - hook.lastTickPosX) * event.partialTicks;
        double y = hook.lastTickPosY + (hook.posY - hook.lastTickPosY) * event.partialTicks + 0.5;
        double z = hook.lastTickPosZ + (hook.posZ - hook.lastTickPosZ) * event.partialTicks;

        renderText(x, y, z, getTimerText(), getCurrentColor());
    }

    private String getTimerText() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getLocalPlayer().fishEntity == null) return "";

        float seconds = MinecraftCompat.getLocalPlayer().fishEntity.ticksExisted / 20f;
        return String.format("%.2fs", seconds);
    }

    private int getCurrentColor() {
        if (ATHRConfig.feature == null) return 0xFFFFFFFF;

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getLocalPlayer().fishEntity == null) return 0xFFFFFFFF;

        float seconds = MinecraftCompat.getLocalPlayer().fishEntity.ticksExisted / 20f;
        boolean alerted = seconds >= ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertTime;

        return ChromaColour.specialToChromaRGB(alerted ? ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertColor : ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerNormalColor);
    }

    private void renderText(double x, double y, double z, String text, int color) {
        Minecraft mc = MinecraftCompat.getMinecraft();

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(x - viewerX, y - viewerY, z - viewerZ);

        GlStateManagerCompat.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManagerCompat.rotate(mc.getRenderManager().playerViewX, 1, 0, 0);

        float scale = 0.025f;
        GlStateManagerCompat.scale(-scale, -scale, scale);

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        int width = fr.getStringWidth(text) / 2;

        fr.drawString(text, -width, 0, color, true);

        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.popMatrix();
    }
}
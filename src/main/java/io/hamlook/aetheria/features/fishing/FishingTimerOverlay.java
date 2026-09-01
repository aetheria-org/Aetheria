package io.hamlook.aetheria.features.fishing;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.projectile.EntityFishHook;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;

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

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        EntityFishHook hook = mc.thePlayer.fishEntity;
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

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        EntityFishHook hook = mc.thePlayer.fishEntity;
        if (hook == null) return;

        double x = hook.lastTickPosX + (hook.posX - hook.lastTickPosX) * event.partialTicks;
        double y = hook.lastTickPosY + (hook.posY - hook.lastTickPosY) * event.partialTicks + 0.5;
        double z = hook.lastTickPosZ + (hook.posZ - hook.lastTickPosZ) * event.partialTicks;

        renderText(x, y, z, getTimerText(), getCurrentColor());
    }

    private String getTimerText() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.fishEntity == null) return "";

        float seconds = mc.thePlayer.fishEntity.ticksExisted / 20f;
        return String.format("%.2fs", seconds);
    }

    private int getCurrentColor() {
        if (ATHRConfig.feature == null) return 0xFFFFFFFF;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.fishEntity == null) return 0xFFFFFFFF;

        float seconds = mc.thePlayer.fishEntity.ticksExisted / 20f;
        boolean alerted = seconds >= ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertTime;

        return ChromaColour.specialToChromaRGB(alerted ? ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerAlertColor : ATHRConfig.feature.fishing.fishingTimerConfig.fishingTimerNormalColor);
    }

    private void renderText(double x, double y, double z, String text, int color) {
        Minecraft mc = Minecraft.getMinecraft();

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x - viewerX, y - viewerY, z - viewerZ);

        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1, 0, 0);

        float scale = 0.025f;
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        FontRenderer fr = mc.fontRendererObj;
        int width = fr.getStringWidth(text) / 2;

        fr.drawString(text, -width, 0, color, true);

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class EntityHighlight {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private EntityHighlight() {
    }

    public static void renderEntityOutline(RenderEntityModelEvent event, Color color) {
        EntityLivingBase entity = event.getEntity();

        float gamma = mc.gameSettings.gammaSetting;
        boolean fancy = mc.gameSettings.fancyGraphics;
        mc.gameSettings.gammaSetting = Float.MAX_VALUE;
        mc.gameSettings.fancyGraphics = false;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.pushAttrib();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.depthMask(false);
        GlStateManagerCompat.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        GlStateManagerCompat.scale(1.03f, 1.03f, 1.03f);

        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.popAttrib();
        GlStateManagerCompat.popMatrix();

        mc.gameSettings.gammaSetting = gamma;
        mc.gameSettings.fancyGraphics = fancy;
    }
}
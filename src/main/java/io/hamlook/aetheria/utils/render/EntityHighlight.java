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

    /**
     * Legacy x-ray shell backend shared by Blood/Boss model modes: faint
     * 1.04x fill + crisp 1.025x edge, depth disabled. Modern equivalent is
     * vanilla glow via {@link EntityOutlines#xray} — this body is legacy-only.
     */
    public static void renderEntityOutline(RenderEntityModelEvent event, Color color) {
        //#if MC < 1.21
        EntityLivingBase entity = event.getEntity();

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.pushAttrib();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.depthMask(false);

        GlStateManagerCompat.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.22f);
        GlStateManagerCompat.scale(1.04f, 1.04f, 1.04f);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManagerCompat.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.88f);
        float shrink = 1.025f / 1.04f;
        GlStateManagerCompat.scale(shrink, shrink, shrink);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.popAttrib();
        GlStateManagerCompat.popMatrix();
        //#else
        //$$ // TODO(outline-strategy): modern glow pass (EntityOutlineRenderer pattern) replaces GL model re-render.
        //#endif
    }
}
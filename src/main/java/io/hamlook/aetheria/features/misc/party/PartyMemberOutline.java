package io.hamlook.aetheria.features.misc.party;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Draws a colored glow around party members' player models.
 * <p>
 * This is intentionally NOT an ESP/X-ray effect: GL_DEPTH_TEST is left enabled the
 * whole time, so the outline is subject to the same depth buffer as everything else
 * on screen - a wall or block between you and the party member hides it exactly like
 * it hides the player model itself. It only ever shows on a party member you could
 * already see normally.
 */
@RegisterEvents
public class PartyMemberOutline {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    @HandleEvent(priority = HandleEvent.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.partyMemberOutline.enabled) return;

        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer || entity.isInvisible()) return;

        if (ATHRConfig.feature.misc.partyMemberOutline.disableInDungeons && SkyblockData.isInDungeon()) return;

        if (!PartyMemberTracker.isPartyMember(entity.getName())) return;

        renderOutline(event, getColor());
    }

    private Color getColor() {
        int argb = ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.partyMemberOutline.outlineColor);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    // Two-pass model render, same technique as the other model outlines in this mod
    // (see EntityHighlight / BloodMobHighlight), but deliberately never touches
    // GL_DEPTH_TEST so occlusion by terrain still applies normally.
    private void renderOutline(RenderEntityModelEvent event, Color color) {
        EntityLivingBase entity = event.getEntity();

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.pushAttrib();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.depthMask(false);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        GlStateManagerCompat.color(r, g, b, a * 0.35f);
        GlStateManagerCompat.scale(1.05f, 1.05f, 1.05f);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManagerCompat.color(r, g, b, a);
        float shrink = 1.02f / 1.05f;
        GlStateManagerCompat.scale(shrink, shrink, shrink);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.popAttrib();
        GlStateManagerCompat.popMatrix();
    }
}
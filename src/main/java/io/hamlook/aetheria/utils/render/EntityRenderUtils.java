package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.events.RenderEntityOutlineEvent;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityRenderUtils {

    private static final double MAX_DIST_SQ = 64 * 64;

    private EntityRenderUtils() {
    }

    public static void renderBoxes(Map<EntityLivingBase, Color> mobs) {
        EntityLivingBase player = MinecraftCompat.getLocalPlayer();
        if (mobs.isEmpty() || player == null) return;

        Map<Color, List<AxisAlignedBB>> byColor = new HashMap<>();
        for (Map.Entry<EntityLivingBase, Color> entry : mobs.entrySet()) {
            EntityLivingBase mob = entry.getKey();
            if (EntityCompat.isDead(mob) || mob.getHealth() <= 0) continue;
            if (!FrustumUtils.isVisible(mob)) continue;
            if (EntityCompat.getDistanceSqTo(mob, player) > MAX_DIST_SQ) continue;
            byColor.computeIfAbsent(entry.getValue(), key -> new ArrayList<>())
                    .add(mob.getEntityBoundingBox().expand(0.1, 0.05, 0.1));
        }

        for (Map.Entry<Color, List<AxisAlignedBB>> entry : byColor.entrySet()) {
            WorldRenderUtils.drawEspBoxes(entry.getValue(), entry.getKey());
        }
    }

    // Legacy render- translucent GL shell. Modern uses vanilla glow see the QUEUED block below
    public static void renderOutline(RenderEntityModelEvent event, Color color) {
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
        //#endif
    }

    //#if MC >= 1.21
    //$$ private static final Map<Entity, Integer> QUEUED = new HashMap<>();
    //$$
    //$$ public static void beginFrame() {
    //$$     QUEUED.clear();
    //$$     RenderEntityOutlineEvent event = new RenderEntityOutlineEvent();
    //$$     event.post();
    //$$     QUEUED.putAll(event.getOutlines());
    //$$ }
    //$$
    //$$ public static boolean shouldOutline(Entity entity, boolean original) {
    //$$     return QUEUED.containsKey(entity) || original;
    //$$ }
    //$$
    //$$ public static int outlineColor(Entity entity, int original) {
    //$$     Integer color = QUEUED.get(entity);
    //$$     return color != null ? color : original;
    //$$ }
    //#endif
}

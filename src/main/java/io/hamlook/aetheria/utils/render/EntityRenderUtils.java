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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//#if MC >= 1.21
//$$ import io.hamlook.aetheria.api.event.AetheriaEventBus;
//#endif

//#if MC < 1.21
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
//#endif

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

    // Legacy draws a translucent model shell; modern enables the vanilla glow outline.
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

    public static void buildInterpolatedBoxes(List<AxisAlignedBB> out, Collection<Entity> targets,
            float partialTicks, double expandXZ, double expandY) {
        out.clear();
        for (Entity target : targets) {
            if (EntityCompat.isDead(target)) continue;

            AxisAlignedBB box = EntityCompat.getEntityBoundingBox(target).expand(expandXZ, expandY, expandXZ);
            double dx = offset(EntityCompat.getPosX(target), EntityCompat.getPrevPosX(target), partialTicks);
            double dy = offset(EntityCompat.getPosY(target), EntityCompat.getPrevPosY(target), partialTicks);
            double dz = offset(EntityCompat.getPosZ(target), EntityCompat.getPrevPosZ(target), partialTicks);
            out.add(new AxisAlignedBB(box.minX + dx, box.minY + dy, box.minZ + dz,
                    box.maxX + dx, box.maxY + dy, box.maxZ + dz));
        }
    }

    //#if MC < 1.21
    public static void renderEntityShell(Entity entity, float partialTicks, int argb, boolean throughWalls, float scale) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        boolean restoreShadow = renderManager.isRenderShadow();
        renderManager.setRenderShadow(false);

        double x = interpolatedPos(EntityCompat.getPosX(entity), EntityCompat.getPrevPosX(entity), partialTicks)
                - renderManager.viewerPosX;
        double y = interpolatedPos(EntityCompat.getPosY(entity), EntityCompat.getPrevPosY(entity), partialTicks)
                - renderManager.viewerPosY;
        double z = interpolatedPos(EntityCompat.getPosZ(entity), EntityCompat.getPrevPosZ(entity), partialTicks)
                - renderManager.viewerPosZ;

        GlStateManagerCompat.pushAttrib();
        // GlStateManager.pushAttrib saves only GL_ENABLE_BIT and GL_LIGHTING_BIT; texture env needs GL_TEXTURE_BIT.
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        applyShellColor(argb);

        if (throughWalls) {
            GlStateManagerCompat.disableDepth();
            GlStateManagerCompat.depthMask(false);
        } else {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1f, -1f);
        }

        WorldRenderUtils.pushMatrix();
        try {
            GlStateManagerCompat.translate(x, y, z);
            GlStateManagerCompat.scale(scale, scale, scale);
            GlStateManagerCompat.translate(-x, -y, -z);
            renderManager.renderEntityStatic(entity, partialTicks, false);
        } finally {
            WorldRenderUtils.popMatrix();
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            renderManager.setRenderShadow(restoreShadow);

            if (throughWalls) {
                GlStateManagerCompat.depthMask(true);
                GlStateManagerCompat.enableDepth();
            }
            GlStateManagerCompat.disableBlend();
            GlStateManagerCompat.enableLighting();
            GL11.glPopAttrib();
            GlStateManagerCompat.popAttrib();
        }
    }

    private static void applyShellColor(int argb) {
        FloatBuffer constant = BufferUtils.createFloatBuffer(4);
        constant.put(((argb >> 16) & 0xff) / 255f);
        constant.put(((argb >> 8) & 0xff) / 255f);
        constant.put((argb & 0xff) / 255f);
        constant.put(((argb >>> 24) & 0xff) / 255f);
        constant.flip();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, constant);
    }
    //#endif

    private static double offset(double current, double previous, float partialTicks) {
        return (current - previous) * partialTicks;
    }

    private static double interpolatedPos(double current, double previous, float partialTicks) {
        return previous + offset(current, previous, partialTicks);
    }

    //#if MC >= 1.21
    //$$ private static final Map<Entity, Integer> QUEUED = new HashMap<>();
    //$$
    //$$ public static void beginFrame() {
    //$$     if (!AetheriaEventBus.INSTANCE.hasListeners(RenderEntityOutlineEvent.class)) return;
    //$$     QUEUED.clear();
    //$$     RenderEntityOutlineEvent event = new RenderEntityOutlineEvent();
    //$$     event.post();
    //$$     QUEUED.putAll(event.getOutlines());
    //$$ }
    //$$
    //$$ public static boolean shouldOutline(Entity entity, boolean original) {
    //$$     return original || QUEUED.containsKey(entity);
    //$$ }
    //$$
    //$$ public static int outlineColor(Entity entity, int original) {
    //$$     Integer color = QUEUED.get(entity);
    //$$     return color != null ? color : original;
    //$$ }
    //#endif
}

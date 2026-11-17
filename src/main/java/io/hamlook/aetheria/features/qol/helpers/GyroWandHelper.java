package io.hamlook.aetheria.features.qol.helpers;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.features.qol.timers.ItemCooldowns;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.RaycastUtils;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@RegisterEvents
public class GyroWandHelper {

    private static final String GYRO_ID = "GYROKINETIC_WAND";
    private static final double RING_RADIUS = 8.5;
    private static final int RING_STEPS = 64;
    private static final double CYLINDER_HEIGHT = 1.0;
    private static final double REACH = 100.0;

    private static final int MODE_RING = 0;
    private static final int MODE_CYLINDER = 1;

    public static boolean isHoldingGyro() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        return MinecraftCompat.getLocalPlayer() != null && GYRO_ID.equals(ItemUtils.getInternalName(MinecraftCompat.getLocalPlayer().getHeldItem()));
    }

    private boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.gyroWandConfig.gyroWand;
    }

    private Vec3 getTargetPos(EntityPlayer player, float partialTicks) {
        BlockPos hit = RaycastUtils.raycastBlock(player, partialTicks, REACH);
        if (hit == null) return null;
        return new Vec3(hit.getX() + 0.5, hit.getY() + 1.0, hit.getZ() + 0.5);
    }

    @HandleEvent
    public void onRenderWorld(ASMRenderWorldEvent event) {
        if (!isEnabled() || !isHoldingGyro()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        EntityPlayer player = MinecraftCompat.getLocalPlayer();

        Vec3 target = getTargetPos(player, event.partialTicks);
        if (target == null) return;

        double px = EntityCompat.getPrevX(player) + (EntityCompat.getPosX(player) - EntityCompat.getPrevX(player)) * event.partialTicks;
        double py = EntityCompat.getPrevY(player) + (EntityCompat.getPosY(player) - EntityCompat.getPrevY(player)) * event.partialTicks;
        double pz = EntityCompat.getPrevZ(player) + (EntityCompat.getPosZ(player) - EntityCompat.getPrevZ(player)) * event.partialTicks;

        float[] color = displayColor();
        try {
            if (ATHRConfig.feature.qol.gyroWandConfig.gyroWandMode == MODE_CYLINDER) {
                drawCylinder(target, px, py, pz, color);
            } else {
                drawRing(target, px, py, pz, color);
            }
        } finally {
            GlStateManagerCompat.enableDepth();
            GlStateManagerCompat.enableTexture2D();
            GlStateManagerCompat.disableBlend();
            GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        }
    }

    private static float[] displayColor() {
        String chroma = ItemCooldowns.isOnCooldown(GYRO_ID)
                ? ATHRConfig.feature.qol.gyroWandConfig.gyroWandCooldownColor
                : ATHRConfig.feature.qol.gyroWandConfig.gyroWandReadyColor;
        int argb = ChromaColour.specialToChromaRGB(chroma);
        return new float[]{
                ((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f, ((argb >> 24) & 0xFF) / 255f};
    }

    private void drawRing(Vec3 target, double px, double py, double pz, float[] color) {
        float thickness = ATHRConfig.feature.qol.gyroWandConfig.gyroWandThickness;
        //#if MC < 1.21
        GL11.glPushMatrix();
        GL11.glTranslated(target.xCoord - px, target.yCoord - py, target.zCoord - pz);
        RenderUtils.drawWorldCircle(RING_RADIUS, RING_STEPS, thickness, color[0], color[1], color[2], color[3]);
        GL11.glPopMatrix();
        //#else
        //$$ if (!WorldRenderUtils.drawWorldCircleModern(target.x, target.y, target.z,
        //$$         RING_RADIUS, RING_STEPS, thickness, color[0], color[1], color[2], color[3])) {
        //$$     RenderUtils.drawWorldCircle(RING_RADIUS, RING_STEPS, thickness, color[0], color[1], color[2], color[3]);
        //$$ }
        //#endif
    }

    private void drawCylinder(Vec3 target, double px, double py, double pz, float[] color) {
        //#if MC < 1.21
        GL11.glPushMatrix();
        GL11.glTranslated(target.xCoord - px, target.yCoord - py, target.zCoord - pz);
        RenderUtils.drawFilledCylinder(RING_RADIUS, CYLINDER_HEIGHT, RING_STEPS, color[0], color[1], color[2], color[3]);
        GL11.glPopMatrix();
        //#else
        //$$ if (!WorldRenderUtils.drawCylinderModern(target.x, target.y, target.z, RING_RADIUS, CYLINDER_HEIGHT,
        //$$         RING_STEPS, color[0], color[1], color[2], color[3])) {
        //$$     RenderUtils.drawFilledCylinder(RING_RADIUS, CYLINDER_HEIGHT, RING_STEPS, color[0], color[1], color[2], color[3]);
        //$$ }
        //#endif
    }
}
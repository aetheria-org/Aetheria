package io.hamlook.aetheria.features.qol.helpers;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.features.qol.timers.ItemCooldowns;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.RaycastUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
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
    private static final double REACH = 100.0;

    private static final float[] COLOR_READY = {0.6f, 0.1f, 0.8f, 0.6f};
    private static final float[] COLOR_COOLDOWN = {1.0f, 0.2f, 0.2f, 0.6f};

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

        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        float[] color = ItemCooldowns.isOnCooldown(GYRO_ID) ? COLOR_COOLDOWN : COLOR_READY;
        float thickness = ATHRConfig.feature.qol.gyroWandConfig.gyroWandThickness;

        try {
            GL11.glPushMatrix();
            GL11.glTranslated(target.xCoord - px, target.yCoord - py, target.zCoord - pz);
            RenderUtils.drawWorldCircle(RING_RADIUS, RING_STEPS, thickness, color[0], color[1], color[2], color[3]);
        } finally {
            GL11.glPopMatrix();
            GlStateManagerCompat.enableDepth();
            GlStateManagerCompat.enableTexture2D();
            GlStateManagerCompat.disableBlend();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }
}
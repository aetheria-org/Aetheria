package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.features.qol.DamageNameplates;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IChatComponent;

public class RendererLivingEntityHook {

    /**
     * Replaces the entity's display name with a custom damage nameplate.
     * Called via @Redirect on getDisplayName() inside renderName().
     */
    public static IChatComponent getDisplayName(EntityLivingBase entity) {
        return DamageNameplates.replaceName(entity);
    }

    /**
     * Forces the player's own nametag to render when showOwnNametag is enabled.
     * Called via @Inject at HEAD of canRenderName().
     */
    public static Boolean shouldShowOwnNametag(EntityLivingBase entity) {
        if (ATHRConfig.feature == null) return null;
        if (ATHRConfig.feature.misc.showOwnNametag && entity == MinecraftCompat.getLocalPlayer()) {
            return true;
        }
        return null;
    }

    /**
     * Fires RenderEntityModelEvent after renderLayers() in doRender().
     * Called via @Inject after renderLayers() invoke.
     */
    public static void fireRenderEntityModelEvent(EntityLivingBase entity, float partialTicks, ModelBase mainModel) {
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float limbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float ageInTicks = entity.ticksExisted + partialTicks;
        float headYaw = entity.prevRotationYawHead + (entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
        float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        new RenderEntityModelEvent(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, 0.0625F, mainModel).post();
    }
}

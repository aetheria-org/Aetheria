package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.features.qol.DamageNameplates;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"))
    private Text athr$damageNameplate(Entity entity) {
        if (entity instanceof LivingEntity) {
            return DamageNameplates.replaceName((LivingEntity) entity);
        }
        return entity.getDisplayName();
    }
}

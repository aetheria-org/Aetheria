package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.features.cosmetics.ItemAnimationsConfig;
import io.hamlook.aetheria.features.cosmetics.itemanimations.ItemAnimations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class MixinLivingEntitySwingDuration {

    @Shadow
    private int getHandSwingDuration() {
        return 0;
    }

    @Redirect(method = "tickHandSwing",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getHandSwingDuration()I"),
            require = 1)
    private int ATHR$overrideSwingDuration(LivingEntity self) {
        if (!(self instanceof ClientPlayerEntity)) return getHandSwingDuration();
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return getHandSwingDuration();
        return ItemAnimations.swingLength(getHandSwingDuration(), config.swingSpeed, config.ignoreHaste);
    }
}

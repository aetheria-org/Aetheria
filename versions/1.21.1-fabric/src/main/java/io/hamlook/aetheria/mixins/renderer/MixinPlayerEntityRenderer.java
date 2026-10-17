package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.repo.PlayerSizeRepo;
import io.hamlook.aetheria.repo.data.PlayerSizeData;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("TAIL"))
    private void ATHR$repoPlayerScale(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.misc.playerSizeOnlyInSkyblock
                && !SkyblockData.isOnSkyblock()) return;
        PlayerSizeData data = PlayerSizeRepo.getScale(player.getName().getString());
        if (data == null) return;
        float x = data.x(), y = data.y(), z = data.z();
        if (x == 0 || y == 0 || z == 0) return;
        if (y < 0) matrices.translate(0f, y * 2, 0f);
        matrices.scale(x, y, z);
    }
}

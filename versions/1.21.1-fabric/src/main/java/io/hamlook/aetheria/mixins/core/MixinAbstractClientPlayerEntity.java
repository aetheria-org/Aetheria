package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {

    @Shadow
    private PlayerListEntry playerListEntry;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void ATHR$getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures base = cir.getReturnValue();
        if (base == null || this.playerListEntry == null) return;
        Cape cape = CapeManager.getCapeForPlayer(this.playerListEntry.getProfile().getName());
        if (cape == null || cape.resourceLocation == null) return;
        if (cape.resourceLocation.equals(base.capeTexture())) return;
        cir.setReturnValue(new SkinTextures(base.texture(), base.textureUrl(), cape.resourceLocation, base.elytraTexture(), base.model(), base.secure()));
    }
}

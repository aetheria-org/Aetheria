package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.features.qol.BetterContainers;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiChest.class)
public class MixinGuiChest_BetterContainers {


    @Redirect(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0))
    private void ATHR$redirectBindTexture(TextureManager tm, ResourceLocation location) {
        if (!BetterContainers.getInstance().tryBindTexture(tm, location)) {
            tm.bindTexture(location);
        }
    }
}
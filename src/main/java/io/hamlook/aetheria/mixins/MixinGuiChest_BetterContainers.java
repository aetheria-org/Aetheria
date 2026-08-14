package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.qol.BetterContainers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChest.class)
public class MixinGuiChest_BetterContainers {

    @Redirect(
            method = "drawGuiContainerBackgroundLayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
                    ordinal = 0)
    )
    private void ATHR$redirectBindTexture(TextureManager tm, ResourceLocation location) {
        if (!BetterContainers.getInstance().tryBindTexture(tm, location)) {
            tm.bindTexture(location);
        }
    }

    @ModifyConstant(method = "drawGuiContainerForegroundLayer", constant = @Constant(intValue = 4210752))
    private int ATHR$modifyContainerTitleColor(int original) {
        if (BetterContainers.isEnabled() && BetterContainers.getInstance().isLoaded()
                && ATHRConfig.feature.qol.betterContainers.style <= 1) {
            return 0;
        }
        return original;
    }

    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("RETURN"))
    private void ATHR$drawWatermark(int mouseX, int mouseY, CallbackInfo ci) {
        if (!BetterContainers.isEnabled() || !BetterContainers.getInstance().isLoaded()
                || ATHRConfig.feature == null) return;

        String label    = "ASM";
        int    textW    = Minecraft.getMinecraft().fontRendererObj.getStringWidth(label);
        int x = ((GuiChest)(Object)this).xSize - textW - 10;
        int    y        = 6;

        int baseColor   = ChromaColour.specialToChromaRGB(
                ATHRConfig.feature.qol.betterContainers.watermarkColor);
        int color       = ChromaColour.applyChromaShift(baseColor, x, y,
                ATHRConfig.feature.qol.betterContainers.watermarkChromaMode,
                ATHRConfig.feature.qol.betterContainers.watermarkChromaSize);

        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(label, x, y, color);
    }
}
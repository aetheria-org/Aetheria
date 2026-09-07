package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiChestHook;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChest.class)
public class MixinGuiChest {

    @Redirect(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0))
    private void ATHR$redirectBindTexture(TextureManager tm, ResourceLocation location) {
        if (!GuiChestHook.redirectBindTexture(tm, location)) {
            tm.bindTexture(location);
        }
    }

    @ModifyConstant(method = "drawGuiContainerForegroundLayer", constant = @Constant(intValue = 4210752))
    private int ATHR$modifyContainerTitleColor(int original) {
        return GuiChestHook.modifyContainerTitleColor(original);
    }

    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("RETURN"))
    private void ATHR$drawWatermark(int mouseX, int mouseY, CallbackInfo ci) {
        GuiChestHook.drawWatermark((GuiChest) (Object) this);
    }

    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At("HEAD"), cancellable = true)
    public void ATHR$cancelDrawBackground(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiChestHook.shouldCancelDraw()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("HEAD"), cancellable = true)
    public void ATHR$cancelDrawForeground(int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiChestHook.shouldCancelDraw()) {
            ci.cancel();
        }
    }
}

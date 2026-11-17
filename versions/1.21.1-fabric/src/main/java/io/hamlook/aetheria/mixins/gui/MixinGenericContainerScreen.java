package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.utils.hooks.GuiChestHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class MixinGenericContainerScreen {

    @Inject(method = "drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$prepareBetterContainers(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiChestHook.shouldCancelDraw()) {
            ci.cancel();
            return;
        }
        BetterContainers.getInstance().tryPrepareDynamic((GenericContainerScreen) (Object) this);
    }

    @ModifyArg(
            method = "drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),
            index = 0
    )
    private Identifier athr$betterContainersTexture(Identifier original) {
        if (!BetterContainers.isEnabled() || !BetterContainers.getInstance().isLoaded()) return original;
        return Resources.BETTER_CONTAINERS_DYNAMIC;
    }
}
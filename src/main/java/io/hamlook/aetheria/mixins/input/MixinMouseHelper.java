package io.hamlook.aetheria.mixins.input;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.mouse.LockMouse;
import io.hamlook.aetheria.features.farming.sensitivityreducer.SensitivityReducer;
import io.hamlook.aetheria.features.qol.CursorResetHandler;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.util.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class MixinMouseHelper {

    @Shadow public int deltaX;
    @Shadow public int deltaY;

    @Inject(method = "ungrabMouseCursor", at = @At("HEAD"), cancellable = true)
    private void ATHR$ungrabMouseCursor(CallbackInfo ci) {
        if (StorageManager.isOverlayActive()) {
            ci.cancel();
            MouseCompat.setGrabbed(false);
            MouseCompat.setCursorPosition(CursorResetHandler.cachedX, CursorResetHandler.cachedY);
            return;
        }
        if (ATHRConfig.feature.qol.preventCursorReset) {
            ci.cancel();
            MouseCompat.setGrabbed(false);
            MouseCompat.setCursorPosition(CursorResetHandler.cachedX, CursorResetHandler.cachedY);
        }
    }

    @Inject(method = "mouseXYChange", at = @At("RETURN"))
    private void ATHR$lockMouse(CallbackInfo ci) {
        if (LockMouse.isLocked() && MinecraftCompat.getMinecraft().currentScreen == null) {
            deltaX = 0;
            deltaY = 0;
        }
    }

    @Inject(method = "mouseXYChange", at = @At("RETURN"))
    private void ATHR$reduceSensitivity(CallbackInfo ci) {
        if (MinecraftCompat.getMinecraft().currentScreen != null) return;
        if (LockMouse.isLocked()) return;
        if (!SensitivityReducer.isActive()) return;
        float scale = SensitivityReducer.getSensitivityScale();
        deltaX = Math.round(deltaX * scale);
        deltaY = Math.round(deltaY * scale);
    }
}

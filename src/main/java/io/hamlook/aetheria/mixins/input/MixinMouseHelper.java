package io.hamlook.aetheria.mixins.input;

import io.hamlook.aetheria.utils.hooks.MouseHelperHook;
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
        if (MouseHelperHook.onUngrabMouseCursor()) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseXYChange", at = @At("RETURN"))
    private void ATHR$lockMouse(CallbackInfo ci) {
        int[] delta = {deltaX, deltaY};
        MouseHelperHook.onLockMouse(delta);
        deltaX = delta[0];
        deltaY = delta[1];
    }

    @Inject(method = "mouseXYChange", at = @At("RETURN"))
    private void ATHR$reduceSensitivity(CallbackInfo ci) {
        int[] delta = {deltaX, deltaY};
        if (MouseHelperHook.onReduceSensitivity(delta)) {
            deltaX = delta[0];
            deltaY = delta[1];
        }
    }
}

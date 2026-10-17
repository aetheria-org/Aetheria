package io.hamlook.aetheria.mixins.input;

import io.hamlook.aetheria.utils.compat.InputEventQueue;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.features.qol.CursorResetHandler;
import io.hamlook.aetheria.utils.hooks.MouseHelperHook;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.tutorial.TutorialManager;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void athr$pushButton(long window, int button, int action, int mods, CallbackInfo ci) {
        InputEventQueue.pushMouseButton(button, action == GLFW.GLFW_PRESS);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void athr$pushScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        InputEventQueue.pushMouseScroll(vertical);
    }

    @Inject(method = "onCursorPos", at = @At("RETURN"))
    private void athr$pushMove(long window, double x, double y, CallbackInfo ci) {
        InputEventQueue.pushMouseMoveAbs(x, y);
    }

    @Inject(method = "unlockCursor", at = @At("TAIL"))
    private void athr$ungrabCursor(CallbackInfo ci) {
        if (MouseHelperHook.shouldKeepCursorPosition()) {
            MouseCompat.setCursorPosition(CursorResetHandler.cachedX, CursorResetHandler.cachedY);
        }
    }

    @Redirect(method = "updateMouse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/tutorial/TutorialManager;onUpdateMouse(DD)V"))
    private void athr$lockTutorial(TutorialManager manager, double x, double y) {
        double[] delta = {x, y};
        MouseHelperHook.applyLookModifiersD(delta);
        manager.onUpdateMouse(delta[0], delta[1]);
    }

    @Redirect(method = "updateMouse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void athr$lockLook(ClientPlayerEntity player, double x, double y) {
        double[] delta = {x, y};
        MouseHelperHook.applyLookModifiersD(delta);
        player.changeLookDirection(delta[0], delta[1]);
    }
}

package io.hamlook.aetheria.mixins.input;

import io.hamlook.aetheria.utils.compat.InputEventQueue;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void athr$pushKey(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        InputEventQueue.pushKey(key, scancode, action != GLFW.GLFW_RELEASE, action == GLFW.GLFW_REPEAT);
    }

    @Inject(method = "onChar", at = @At("HEAD"))
    private void athr$pushChar(long window, int codepoint, int mods, CallbackInfo ci) {
        InputEventQueue.pushChar((char) codepoint);
    }
}

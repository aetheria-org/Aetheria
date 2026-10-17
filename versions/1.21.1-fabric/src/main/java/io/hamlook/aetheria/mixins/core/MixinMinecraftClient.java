package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow
    private Screen currentScreen;

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen athr$swapScreen(Screen screen) {
        ASMGuiOpenEvent event = new ASMGuiOpenEvent(screen);
        event.post();
        if (event.isCancelled()) return this.currentScreen;
        return event.gui;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void athr$consumeInputDuringTransition(CallbackInfo ci) {
        // Unconditional pump: our GLFW queue is private (vanilla has its own
        // callbacks), so draining it every tick only refreshes our last-state
        // snapshots — unlike legacy LWJGL2 where next() stole shared events.
        // Storage transition swallowing still holds: its handlers gate on
        // isTransitioning themselves.
        while (MouseCompat.next()) {
        }
        while (KeyboardCompat.next()) {
        }
    }
}

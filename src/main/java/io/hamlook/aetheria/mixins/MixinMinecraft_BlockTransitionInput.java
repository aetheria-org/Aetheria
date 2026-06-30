package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.features.storage.StorageManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft_BlockTransitionInput {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void consumeInputDuringTransition(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (StorageManager.isTransitioning() && mc.currentScreen == null) {
            while (Mouse.next()) { /* drain all mouse events */ }
            while (Keyboard.next()) { /* drain all keyboard events */ }
        }
    }
}

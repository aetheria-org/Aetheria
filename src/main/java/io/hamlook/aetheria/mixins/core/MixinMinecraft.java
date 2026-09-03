package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void ATHR$consumeInputDuringTransition(CallbackInfo ci) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (StorageManager.isTransitioning() && mc.currentScreen == null) {
            while (MouseCompat.next()) {
            }
            while (KeyboardCompat.next()) {
            }
        }
    }
}

package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.utils.hooks.HeldItemHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void ATHR$skipSwapAnimation(CallbackInfo ci) {
        if (!HeldItemHook.shouldSkipSwapAnimation()) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            mainHand = player.getMainHandStack();
            offHand = player.getOffHandStack();
        }
        equipProgressMainHand = 1.0F;
        prevEquipProgressMainHand = 1.0F;
        equipProgressOffHand = 1.0F;
        prevEquipProgressOffHand = 1.0F;
    }
}

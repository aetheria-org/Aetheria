package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.events.BlockBreakEvent;
import io.hamlook.aetheria.events.BlockClickEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void athr$onDamageBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        new BlockClickEvent(pos, direction).post();
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void athr$onBlockDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        new BlockBreakEvent(pos).post();
    }
}

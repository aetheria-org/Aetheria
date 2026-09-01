package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.events.BlockBreakEvent;
import io.hamlook.aetheria.events.BlockClickEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"))
    private void ATHR$onDamageBlock(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        new BlockClickEvent(pos, side).post();
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFX(ILnet/minecraft/util/BlockPos;I)V"))
    private void ATHR$onBlockDestroy(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        new BlockBreakEvent(pos).post();
    }
}

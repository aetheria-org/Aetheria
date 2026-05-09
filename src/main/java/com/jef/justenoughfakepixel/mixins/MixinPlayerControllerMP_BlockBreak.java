package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.events.BlockBreakEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP_BlockBreak {

    @Inject(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFX(ILnet/minecraft/util/BlockPos;I)V"))
    private void onBlockDestroy(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        MinecraftForge.EVENT_BUS.post(new BlockBreakEvent(pos));
    }
}

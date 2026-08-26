package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.events.BlockClickEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP_BlockClick {

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"))
    private void ATHR$onDamageBlock(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        MinecraftForge.EVENT_BUS.post(new BlockClickEvent(pos, side));
    }
}

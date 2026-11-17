package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiIngameHook;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void ATHR$renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (GuiIngameHook.shouldCancelScoreboard()) {
            ci.cancel();
        }
    }
}

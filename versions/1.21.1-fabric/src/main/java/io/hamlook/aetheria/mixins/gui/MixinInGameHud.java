package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiIngameHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true)
    private void ATHR$renderScoreboard(DrawContext drawContext, ScoreboardObjective objective, CallbackInfo ci) {
        if (GuiIngameHook.shouldCancelScoreboard()) {
            ci.cancel();
        }
    }
}

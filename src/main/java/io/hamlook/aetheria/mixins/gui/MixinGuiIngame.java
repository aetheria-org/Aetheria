package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void ATHR$renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (!CustomScoreboard.isActive()) return;
        if (OverlayUtils.isChatOpen()) return;
        if (OverlayUtils.isDebugActive() && ATHRConfig.feature.scoreboard.hideOnDebug) return;
        if (OverlayUtils.isTabHeld() && ATHRConfig.feature.scoreboard.hideOnTab) return;
        if (OverlayUtils.isStorageActive()) return;
        ci.cancel();
    }
}

package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;

public final class GuiIngameHook {

    private GuiIngameHook() {
    }

    public static boolean shouldCancelScoreboard() {
        if (!CustomScoreboard.isActive()) return false;
        if (OverlayUtils.isChatOpen()) return false;
        if (OverlayUtils.isDebugActive() && ATHRConfig.feature.scoreboard.hideOnDebug) return false;
        if (OverlayUtils.isTabHeld() && ATHRConfig.feature.scoreboard.hideOnTab) return false;
        return !OverlayUtils.isStorageActive();
    }
}

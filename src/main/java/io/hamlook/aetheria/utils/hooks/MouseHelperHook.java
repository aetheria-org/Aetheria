package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.mouse.LockMouse;
import io.hamlook.aetheria.features.farming.sensitivityreducer.SensitivityReducer;
import io.hamlook.aetheria.features.qol.CursorResetHandler;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

public class MouseHelperHook {

    /**
     * Handles ungrab logic. Cancels vanilla reset when storage overlay is active
     * or preventCursorReset is enabled.
     *
     * @return true if the mixin should cancel the vanilla ungrab
     */
    public static boolean onUngrabMouseCursor() {
        if (StorageManager.isOverlayActive()) {
            MouseCompat.setGrabbed(false);
            MouseCompat.setCursorPosition(CursorResetHandler.cachedX, CursorResetHandler.cachedY);
            return true;
        }
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.preventCursorReset) {
            MouseCompat.setGrabbed(false);
            MouseCompat.setCursorPosition(CursorResetHandler.cachedX, CursorResetHandler.cachedY);
            return true;
        }
        return false;
    }

    /**
     * Zeros deltas when lock mouse is active and no screen is open.
     */
    public static void onLockMouse(int[] delta) {
        if (LockMouse.isLocked() && MinecraftCompat.getCurrentScreen() == null) {
            delta[0] = 0;
            delta[1] = 0;
        }
    }

    /**
     * Applies sensitivity reduction scale when farming tool is held.
     *
     * @return true if deltas were modified
     */
    public static boolean onReduceSensitivity(int[] delta) {
        if (MinecraftCompat.getCurrentScreen() != null) return false;
        if (LockMouse.isLocked()) return false;
        if (!SensitivityReducer.isActive()) return false;

        float scale = SensitivityReducer.getSensitivityScale();
        delta[0] = Math.round(delta[0] * scale);
        delta[1] = Math.round(delta[1] * scale);
        return true;
    }
}

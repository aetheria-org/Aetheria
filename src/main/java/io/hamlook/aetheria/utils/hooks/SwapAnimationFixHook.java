package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;

public class SwapAnimationFixHook {

    public static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.bugFixes.swapAnimationFix;
    }
}

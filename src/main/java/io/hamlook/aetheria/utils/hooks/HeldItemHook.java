package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;

public class HeldItemHook {

    public static boolean shouldSkipSwapAnimation() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.noItemSwitchAnimation;
    }
}

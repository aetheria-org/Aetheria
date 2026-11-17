package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;

public class MiningProgressHook {

    public static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.mining.miningProgressFix;
    }
}

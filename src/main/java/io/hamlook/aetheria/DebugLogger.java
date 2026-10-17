package io.hamlook.aetheria;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DebugLogger {

    private static final Logger LOG = LogManager.getLogger("ATHR");
    private static final String PREFIX = "§8[§6ATHR Debug§8] §r";

    private DebugLogger() {}

    public static void log(String message) {
        LOG.info("[ATHR DEBUG] {}", message);
        if (ATHRConfig.feature != null && ATHRConfig.feature.debug.enableDebug) {
            ChatUtils.sendMessage(TextCompat.fromFormatted(PREFIX + message));
        }
    }
}
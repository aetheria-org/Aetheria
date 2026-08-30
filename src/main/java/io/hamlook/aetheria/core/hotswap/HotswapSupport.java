package io.hamlook.aetheria.core.hotswap;

import io.hamlook.aetheria.Aetheria;

public class HotswapSupport {

    private static final boolean isForgeSidePresent;
    private static final HotswapSupportHandle obj;

    static {
        boolean present = false;
        try {
            Class.forName("moe.nea.hotswapagentforge.forge.HotswapEvent");
            present = true;
        } catch (ClassNotFoundException ignored) {
        }
        isForgeSidePresent = present;

        HotswapSupportHandle handle = null;
        if (isForgeSidePresent) {
            try {
                handle = new HotswapSupportImpl();
            } catch (Throwable t) {
                Aetheria.logger.warning("[ATHR] HotSwap support failed to init: " + t.getMessage());
            }
        }
        obj = handle;
    }

    public static boolean isLoaded() {
        return obj != null && obj.isLoaded();
    }

    public static void load() {
        if (obj != null) {
            obj.load();
            Aetheria.logger.info("[ATHR] HotSwap support loaded");
        }
    }
}

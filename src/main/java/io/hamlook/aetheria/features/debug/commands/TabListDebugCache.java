package io.hamlook.aetheria.features.debug.commands;

import java.util.List;

/**
 * When active, /asmcopytablist copies this fake tablist instead of the real one.
 * Set with /asmtesttablist (reads from clipboard), cleared by running it again.
 * Mirrors SkyHanni's debugCache/shtesttablist pattern — lets you reproduce a
 * tablist-related bug from a paste someone sent you, without needing their
 * exact server state.
 */
public class TabListDebugCache {

    private static List<String> cache = null;

    private TabListDebugCache() {
    }

    public static boolean isActive() {
        return cache != null;
    }

    public static List<String> get() {
        return cache;
    }

    public static void set(List<String> lines) {
        cache = lines;
    }

    public static void clear() {
        cache = null;
    }
}

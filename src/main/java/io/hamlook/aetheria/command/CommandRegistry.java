package io.hamlook.aetheria.command;

import java.util.*;

public class CommandRegistry {
    private static final Set<String> REGISTERED = new HashSet<>();

    public static void register(String name, List<String> aliases) {
        REGISTERED.add(name.toLowerCase(Locale.ROOT));
        if (aliases != null) {
            for (String alias : aliases) {
                REGISTERED.add(alias.toLowerCase(Locale.ROOT));
            }
        }
    }

    public static boolean isRegistered(String name) {
        return name != null && REGISTERED.contains(name.toLowerCase(Locale.ROOT));
    }

    public static String firstWordOf(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) == '/') return null;
        int sp = trimmed.indexOf(' ');
        return sp == -1 ? trimmed : trimmed.substring(0, sp);
    }

    public static Set<String> allNames() {
        return Collections.unmodifiableSet(REGISTERED);
    }
}

package io.hamlook.aetheria.utils;

import java.util.Locale;

public class NameUtils {

    private NameUtils() {
    }

    public static String normalize(String name) {
        if (name == null) return null;
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}

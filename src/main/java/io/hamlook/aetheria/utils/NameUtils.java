package io.hamlook.aetheria.utils;

import java.util.Locale;

public class NameUtils {

    private NameUtils() {
    }

    public static String normalize(String name) {
        if (name == null) return null;
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    public static boolean isBackupFile(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".corrupted") || lower.contains(".corrupted.");
    }

    public static boolean isTempFile(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".tmp");
    }

    public static boolean isBackupOrTempFile(String name) {
        return isBackupFile(name) || isTempFile(name);
    }

    public static boolean isLiveJsonFile(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".json") && !isBackupOrTempFile(lower);
    }

    public static boolean isContainerFile(String name, String... prefixes) {
        if (!isLiveJsonFile(name) || prefixes == null || prefixes.length == 0) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        int dash = lower.indexOf('-');
        if (dash <= 0) return false;
        String prefix = lower.substring(0, dash);
        boolean known = false;
        for (String candidate : prefixes) {
            if (candidate != null && candidate.equals(prefix)) {
                known = true;
                break;
            }
        }
        if (!known) return false;
        String page = lower.substring(dash + 1, lower.length() - ".json".length());
        if (page.isEmpty()) return false;
        for (int i = 0; i < page.length(); i++) {
            if (!Character.isDigit(page.charAt(i))) return false;
        }
        return true;
    }
}

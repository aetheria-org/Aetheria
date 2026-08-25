package io.hamlook.aetheria.utils.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFormatter {

    private static final Pattern DURATION_TOKEN = Pattern.compile(
            "(\\d+)\\s*(d|days|day|h|hours|hour|hrs|hr|m|mins|min|minutes|minute|s|secs|sec|seconds|second)?",
            Pattern.CASE_INSENSITIVE);

    public static long parseDurationMs(String raw) {
        if (raw == null || raw.isEmpty()) return -1L;
        Matcher m = DURATION_TOKEN.matcher(raw.toLowerCase());
        long total = 0L;
        boolean any = false;
        while (m.find()) {
            long value;
            try {
                value = Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            String unit = m.group(2) == null ? "" : m.group(2);
            char first = unit.isEmpty() ? 's' : unit.charAt(0);
            any = true;
            switch (first) {
                case 'd':
                    total += value * 86_400_000L;
                    break;
                case 'h':
                    total += value * 3_600_000L;
                    break;
                case 'm':
                    total += value * 60_000L;
                    break;
                default:
                    total += value * 1_000L;
                    break;
            }
        }
        return any ? total : -1L;
    }

    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long ms = millis % 1000;

        if (totalSeconds >= 60) {
            long mins = totalSeconds / 60;
            long secs = totalSeconds % 60;
            return secs > 0
                    ? String.format("%dm %d.%ds", mins, secs, ms / 100)
                    : mins + "m";
        }

        return String.format("%d.%ds", totalSeconds, ms / 100);
    }

    public static String formatDungeonTime(long millis) {
        if (millis <= 0) return "0:00.000";
        long s = millis / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60) + "." + String.format("%03d", millis % 1000);
    }


    public static String formatCountdown(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;    s %= 60;

        if (d > 0) return String.format("%dd %dh %dm", d, h, m);
        if (h > 0) return String.format("%dh %dm",        h, m);
        if (m > 0) return String.format("%dm %ds",        m, s);
        return String.format("%d.%ds", s, (ms % 1000) / 100);
    }

    public static String getCountdownColor(long remainingMs, long totalMs) {
        if (totalMs <= 0) return "§c";
        double pct = (double) remainingMs / totalMs;
        if (pct > 0.5) return "§a";
        if (pct > 0.25) return "§e";
        return "§c";
    }
}
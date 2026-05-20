package com.jef.justenoughfakepixel.utils.time;

public class TimeFormatter {

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
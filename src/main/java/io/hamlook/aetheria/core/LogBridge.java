package io.hamlook.aetheria.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogBridge {

    private static boolean locationFailureLogged;

    private LogBridge() {
    }

    public static void install(Logger julLogger) {
        if (julLogger == null) return;
        for (Handler h : julLogger.getHandlers()) {
            if (h instanceof ForwardingHandler) return;
        }
        julLogger.addHandler(new ForwardingHandler());
        julLogger.setUseParentHandlers(false);
    }

    public static String stamp(String version) {
        return "[ATHR] Aetheria " + version + " build " + codeSourceTime();
    }

    private static String codeSourceTime() {
        URL location = null;
        try {
            location = LogBridge.class.getProtectionDomain().getCodeSource().getLocation();
            File file = locationFile(location);
            if (file == null || !file.exists()) {
                logLocationFailure(location, null);
                return "unknown";
            }
            long modified = file.isDirectory() ? newestModified(file) : file.lastModified();
            if (modified <= 0L) return "unknown";
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date(modified));
            return file.isDirectory() ? time : time + " (" + humanSize(file.length()) + ")";
        } catch (SecurityException | IllegalArgumentException e) {
            logLocationFailure(location, e);
            return "unknown";
        }
    }

    private static File locationFile(URL location) {
        if (location == null) return null;
        String spec = location.toString();
        if (spec.startsWith("jar:")) {
            int end = spec.indexOf("!/");
            spec = end >= 0 ? spec.substring(4, end) : spec.substring(4);
        }
        Throwable failure = null;
        try {
            return new File(new URI(spec));
        } catch (URISyntaxException | IllegalArgumentException e) {
            failure = e;
        }
        try {
            return new File(new URI(spec.replace(" ", "%20")));
        } catch (URISyntaxException | IllegalArgumentException retry) {
            if (failure != null) retry.addSuppressed(failure);
            logLocationFailure(location, retry);
            return null;
        }
    }

    private static void logLocationFailure(URL location, Throwable error) {
        if (locationFailureLogged) return;
        locationFailureLogged = true;
        org.apache.logging.log4j.LogManager.getLogger("Aetheria")
                .warn("[ATHR] Could not read the build time from " + location, error);
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static long newestModified(File dir) {
        long newest = dir.lastModified();
        File[] children = dir.listFiles();
        if (children == null) return newest;
        for (File child : children) {
            long candidate = child.isDirectory() ? newestModified(child) : child.lastModified();
            if (candidate > newest) newest = candidate;
        }
        return newest;
    }

    private static final class ForwardingHandler extends Handler {
        private final org.apache.logging.log4j.Logger target =
                org.apache.logging.log4j.LogManager.getLogger("Aetheria");

        @Override
        public void publish(LogRecord record) {
            if (record == null) return;
            String message = record.getMessage();
            Object[] params = record.getParameters();
            if (params != null && params.length > 0) {
                try {
                    message = java.text.MessageFormat.format(message, params);
                } catch (IllegalArgumentException ignored) {
                }
            }
            Throwable thrown = record.getThrown();
            Level level = record.getLevel();
            if (level.intValue() >= Level.SEVERE.intValue()) {
                if (thrown == null) target.error(message);
                else target.error(message, thrown);
            } else if (level.intValue() >= Level.WARNING.intValue()) {
                if (thrown == null) target.warn(message);
                else target.warn(message, thrown);
            } else if (level.intValue() >= Level.INFO.intValue()) {
                if (thrown == null) target.info(message);
                else target.info(message, thrown);
            } else {
                if (thrown == null) target.debug(message);
                else target.debug(message, thrown);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}

package io.hamlook.aetheria.core;

import java.io.File;
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
        try {
            URL location = LogBridge.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            long modified = file.isDirectory() ? newestModified(file) : file.lastModified();
            if (modified <= 0L) return "unknown";
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date(modified));
        } catch (URISyntaxException | SecurityException | IllegalArgumentException e) {
            return "unknown";
        }
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

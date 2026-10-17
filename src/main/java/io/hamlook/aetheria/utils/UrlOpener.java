package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;

import java.awt.*;
import java.net.URI;
import java.util.Locale;

public final class UrlOpener {

    private UrlOpener() {
    }

    public static boolean open(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception e) {
            Aetheria.logger.warning("UrlOpener: Desktop.browse failed for " + url + ": " + e);
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
            return true;
        } catch (Exception e) {
            Aetheria.logger.warning("UrlOpener: fallback launch failed for " + url + ": " + e);
            return false;
        }
    }
}

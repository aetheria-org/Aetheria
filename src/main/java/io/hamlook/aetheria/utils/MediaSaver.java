package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Saves media (images, animated images, videos) from a URL to
 * {@code .minecraft/aetheria/downloads/}. Only image/video/gif URLs are
 * accepted — anything else is rejected so the in-game UI can never download
 * non-media files.
 */
public final class MediaSaver {

    private static final Set<String> MEDIA_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "mp4", "webm", "mkv", "mov", "avi", "m4v", "wmv", "flv", "ts")));

    private static final String[] ILLEGAL = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    private MediaSaver() {}

    public static boolean isMediaUrl(String url) {
        if (url == null) return false;
        String base = url;
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot >= base.length() - 1) return false;
        return MEDIA_EXTENSIONS.contains(base.substring(dot + 1).toLowerCase());
    }

    /** Media file extension of a URL (lowercased, without dot), or null if it isn't a media URL. */
    public static String mediaExtension(String url) {
        if (url == null) return null;
        String base = url;
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot >= base.length() - 1) return null;
        String ext = base.substring(dot + 1).toLowerCase();
        return MEDIA_EXTENSIONS.contains(ext) ? ext : null;
    }

    /** Last path segment of a URL, sanitized for use as a file name. */
    public static String fileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "download";
        String base = url;
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int slash = base.lastIndexOf('/');
        if (slash >= 0 && slash < base.length() - 1) base = base.substring(slash + 1);
        String sanitized = base;
        for (String bad : ILLEGAL) sanitized = sanitized.replace(bad, "_");
        sanitized = sanitized.replaceAll("\\s+", "_").trim();
        return sanitized.isEmpty() ? "download" : sanitized;
    }

    private static String sanitize(String name) {
        if (name == null || name.isEmpty()) return "download";
        String s = name;
        for (String bad : ILLEGAL) s = s.replace(bad, "_");
        s = s.replaceAll("\\s+", "_").trim();
        return s.isEmpty() ? "download" : s;
    }

    /**
     * Downloads a media URL to {@code aetheria/downloads/<name>.<ext>} (name
     * collisions get a numeric suffix). Returns the relative path of the saved
     * file. Throws if the URL isn't image/video/gif or the download fails.
     */
    public static String save(String url, String name) throws IOException {
        if (!io.hamlook.aetheria.network.NetworkGuard.apiAllowed()) throw new IOException("Network disabled.");
        String ext = mediaExtension(url);
        if (ext == null) throw new IOException("Only images, videos and GIFs can be downloaded.");
        if (url == null || url.isEmpty()) throw new IOException("No download URL.");

        String base = name;
        if (base == null || base.isEmpty()) base = fileNameFromUrl(url);
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = sanitize(base);

        File dir = new File(MinecraftCompat.getMinecraft().mcDataDir, "aetheria/downloads");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Could not create downloads folder.");

        File target = new File(dir, base + "." + ext);
        int n = 1;
        while (target.exists()) {
            target = new File(dir, base + "_" + (n++) + "." + ext);
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Download failed: HTTP " + code);
        }
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
        return "aetheria/downloads/" + target.getName();
    }
}

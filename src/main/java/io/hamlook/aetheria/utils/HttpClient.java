package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClient {
    private static final int TIMEOUT_MS = 30000;
    private static final int IMAGE_TIMEOUT_MS = 30000;
    private static final String USER_AGENT = "Aetheria/" + Aetheria.VERSION;

    private static String readAll(HttpURLConnection conn) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    public FetchResult fetch(String url, String etag) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (etag != null) conn.setRequestProperty("If-None-Match", etag);

        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return new FetchResult(null, etag, false);
        }
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code);

        String newEtag = conn.getHeaderField("ETag");
        String body = readAll(conn);
        return new FetchResult(body, newEtag != null ? newEtag : etag, true);
    }

    /**
     * Fetches a remote image and decodes it into a {@link BufferedImage}.
     * Uses a dedicated timeout (image sheets can be large) separate from text fetches.
     * Throws on HTTP errors, connection failures or undecodable bodies.
     */
    public static BufferedImage fetchImage(String url) throws Exception {
        return fetchImage(url, USER_AGENT);
    }

    /** Same as {@link #fetchImage(String)} but with an explicit User-Agent (for backends that expect a fixed one). */
    public static BufferedImage fetchImage(String url, String userAgent) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(IMAGE_TIMEOUT_MS);
        conn.setReadTimeout(IMAGE_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", userAgent);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code);

        try (java.io.InputStream in = conn.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new RuntimeException("Undecodable image body from " + url);
            return img;
        }
    }

    public int post(String url, String body, String contentType) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    public static class FetchResult {
        private final String body;
        private final String etag;
        private final boolean modified;

        public FetchResult(String body, String etag, boolean modified) {
            this.body = body;
            this.etag = etag;
            this.modified = modified;
        }

        public String body() {
            return body;
        }

        public String etag() {
            return etag;
        }

        public boolean modified() {
            return modified;
        }
    }
}

package io.hamlook.aetheria.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClient {
    private static final int TIMEOUT_MS = 5000;
    private static final String USER_AGENT = "ATHR/1.0 (Minecraft 1.8.9)";

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

    public void post(String url, String body, String contentType) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        conn.getResponseCode();
        conn.disconnect();
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

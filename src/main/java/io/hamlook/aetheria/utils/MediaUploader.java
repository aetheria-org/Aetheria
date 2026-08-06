package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.repo.CapeAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Uploads a file through the CapeAPI {@code /upload-attachment} endpoint (which
 * forwards it to catbox.moe). Only images, videos and GIFs are accepted; the
 * server enforces the same whitelist. The response carries the file metadata
 * (name, size, type) that catbox.moe itself drops, so the chat can render a
 * file embed with that info on both Minecraft and Discord.
 */
public class MediaUploader {

    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "mp4", "webm", "mkv", "mov", "avi", "m4v", "wmv", "flv", "ts")));

    private MediaUploader() {}

    /** Result of a successful upload: direct file URL plus the preserved metadata. */
    public static class UploadResult {
        public String url;
        public String name;
        public long size;
        public String type;
    }

    /** Uploads a file to the CapeAPI upload endpoint and returns its URL + metadata. */
    public static UploadResult upload(File file) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("Selected file does not exist.");
        String fileName = file.getName().replace("\"", "").replace("\r", "").replace("\n", "");
        int dot = fileName.lastIndexOf('.');
        String ext = dot < 0 || dot >= fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException("Only images, videos and GIFs can be uploaded (got ." + (ext.isEmpty() ? "?" : ext) + ").");
        }
        long size = file.length();
        if (size <= 0) throw new IOException("Selected file is empty.");

        HttpURLConnection conn = (HttpURLConnection) new URL(CapeAPI.getAPIUrl("upload-attachment")).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(180000);
        conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
        conn.setRequestProperty("Content-Type", "application/x-aetheria-upload");
        conn.setRequestProperty("X-File-Name", fileName);
        conn.setRequestProperty("X-File-Type", mimeFor(ext));
        conn.setFixedLengthStreamingMode(size);

        try (InputStream in = new FileInputStream(file); OutputStream out = conn.getOutputStream()) {
            byte[] buf = new byte[65536];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }

        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        if (code != 200) {
            throw new IOException("Upload failed (HTTP " + code + "): " + body);
        }
        UploadResult result = GlobalChat.GSON.fromJson(body, UploadResult.class);
        if (result == null || result.url == null || result.url.isEmpty()) {
            throw new IOException("Unexpected upload response: " + body);
        }
        if (result.name == null || result.name.isEmpty()) result.name = fileName;
        if (result.type == null || result.type.isEmpty()) result.type = ext.toUpperCase();
        return result;
    }

    private static String mimeFor(String ext) {
        switch (ext) {
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "bmp": return "image/bmp";
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "mkv": return "video/x-matroska";
            case "mov": return "video/quicktime";
            case "avi": return "video/x-msvideo";
            case "m4v": return "video/x-m4v";
            case "wmv": return "video/x-ms-wmv";
            case "flv": return "video/x-flv";
            case "ts": return "video/mp2t";
            default: return "application/octet-stream";
        }
    }

    private static String readBody(HttpURLConnection conn, int code) throws IOException {
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}

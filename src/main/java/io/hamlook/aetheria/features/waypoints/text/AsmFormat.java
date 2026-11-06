package io.hamlook.aetheria.features.waypoints.text;

import com.google.gson.*;
import io.hamlook.aetheria.features.waypoints.WaypointGroup;
import io.hamlook.aetheria.features.waypoints.WaypointPoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

// format: ASMWP1:<base64>#<checksum>
// body is gzipped JSON in base64 so it survives the clipboard
// checksum is CRC32 of the base64 text, checked before decompression
// unknown fields are ignored, missing ones get defaults
final class AsmFormat implements WaypointTextFormat {

    static final int VERSION = 1;

    private static final String PREFIX = "ASMWP" + VERSION + ":";
    private static final Pattern HEADER = Pattern.compile("ASMWP(\\d+):", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int BUFFER_SIZE = 4096;

    private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, (byte) 0xff};

    private static void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return null;
        return element.getAsString();
    }

    private static double number(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive())
            throw new IllegalArgumentException("export data has an invalid point");
        JsonPrimitive value = element.getAsJsonPrimitive();
        if (!value.isNumber()) throw new IllegalArgumentException("export data has an invalid point");
        return value.getAsDouble();
    }

    private static String checksum(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes(StandardCharsets.UTF_8));
        return String.format(Locale.ROOT, "%08x", crc.getValue());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String name() {
        return "aetheria";
    }

    @Override
    public boolean matches(String text) {
        return HEADER.matcher(text).find();
    }

    @Override
    public WaypointGroup read(String text) {
        String compact = WHITESPACE.matcher(text).replaceAll("");
        Matcher header = HEADER.matcher(compact);
        if (!header.find()) throw new IllegalArgumentException("not an Aetheria export");
        checkVersion(Integer.parseInt(header.group(1)));
        return fromJson(new String(gunzip(decode(verify(compact.substring(header.end())))), StandardCharsets.UTF_8));
    }

    String write(WaypointGroup group) {
        String data = Base64.getEncoder().withoutPadding().encodeToString(gzip(toJson(group)));
        return PREFIX + data + "#" + checksum(data);
    }

    private void checkVersion(int version) {
        if (version == VERSION) return;
        if (version > VERSION) throw new IllegalArgumentException("this export is from a newer version of Aetheria");
        throw new IllegalArgumentException("unsupported export version " + version);
    }

    private String verify(String body) {
        int split = body.lastIndexOf('#');
        if (split < 0) throw new IllegalArgumentException("damaged text: checksum is missing");
        String data = body.substring(0, split);
        String expected = body.substring(split + 1).toLowerCase(Locale.ROOT);
        if (!checksum(data).equals(expected))
            throw new IllegalArgumentException("damaged text: checksum does not match");
        return data;
    }

    private byte[] decode(String data) {
        try {
            return Base64.getMimeDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("damaged text: data could not be decoded");
        }
    }

    private byte[] gunzip(byte[] compressed) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > WaypointText.MAX_BYTES) throw new IllegalArgumentException("damaged text: too large");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("damaged text: compressed data could not be read");
        }
    }

    private byte[] toJson(WaypointGroup group) {
        JsonObject root = new JsonObject();
        root.addProperty("name", nullToEmpty(group.name));
        root.addProperty("description", nullToEmpty(group.description));
        JsonArray points = new JsonArray();
        for (WaypointPoint point : group.waypoints) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", point.x);
            entry.addProperty("y", point.y);
            entry.addProperty("z", point.z);
            entry.addProperty("name", nullToEmpty(point.name));
            points.add(entry);
        }
        root.add("points", points);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] gzip(byte[] raw) {
        CRC32 crc = new CRC32();
        crc.update(raw);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(GZIP_HEADER, 0, GZIP_HEADER.length);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(raw);
            deflater.finish();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
        } finally {
            deflater.end();
        }
        writeIntLittleEndian(out, (int) crc.getValue());
        writeIntLittleEndian(out, raw.length);
        return out.toByteArray();
    }

    private WaypointGroup fromJson(String json) {
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) throw new IllegalStateException();
            root = element.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("export data is not valid JSON");
        }
        JsonElement points = root.get("points");
        if (points == null || !points.isJsonArray()) throw new IllegalArgumentException("export data has no points");
        WaypointGroup group = new WaypointGroup(nullToEmpty(text(root, "name")), nullToEmpty(text(root, "description")));
        JsonArray array = points.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) throw new IllegalArgumentException("export data has an invalid point");
            JsonObject entry = element.getAsJsonObject();
            String pointName = text(entry, "name");
            if (pointName == null || pointName.isEmpty()) pointName = String.valueOf(index + 1);
            group.waypoints.add(new WaypointPoint(number(entry, "x"), number(entry, "y"), number(entry, "z"), pointName));
        }
        return group;
    }
}


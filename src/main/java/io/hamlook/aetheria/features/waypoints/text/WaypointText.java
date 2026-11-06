package io.hamlook.aetheria.features.waypoints.text;

import io.hamlook.aetheria.features.waypoints.WaypointGroup;
import io.hamlook.aetheria.features.waypoints.WaypointPoint;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

// entry point, text <-> group
// read tries readers in order, first match wins, old exports keep importing
// untrusted text limits are applied here after parsing
public final class WaypointText {

    public static final int MAX_POINTS = 2000;
    public static final int MAX_POINT_NAME = 128;
    public static final int MAX_GROUP_NAME = 128;
    public static final int MAX_DESCRIPTION = 512;
    public static final int MAX_BYTES = 1 << 20;
    public static final double MAX_COORDINATE = 30_000_000.0;

    private static final AsmFormat ASM = new AsmFormat();
    private static final SoopyFormat SOOPY = new SoopyFormat();
    private static final PlainRowsFormat ROWS = new PlainRowsFormat();

    private static final List<WaypointTextFormat> READERS = Collections.unmodifiableList(Arrays.asList(ASM, SOOPY, ROWS));

    private static final Pattern NOISE = Pattern.compile("[\\uFEFF\\u200B\\u200C\\u200D\\u2060]");

    private WaypointText() {
    }

    public static String write(WaypointGroup group) {
        return ASM.write(group);
    }

    public static String writeSoopy(WaypointGroup group) {
        return SOOPY.write(group);
    }

    public static Result read(String raw) {
        if (raw == null) return Result.unmatched();
        String text = NOISE.matcher(raw).replaceAll("").trim();
        if (text.isEmpty()) return Result.unmatched();

        WaypointTextFormat reader = null;
        for (WaypointTextFormat candidate : READERS) {
            if (candidate.matches(text)) {
                reader = candidate;
                break;
            }
        }
        if (reader == null) return Result.unmatched();

        try {
            WaypointGroup group = reader.read(text);
            int skipped = sanitize(group);
            return Result.found(reader.name(), group, skipped);
        } catch (RuntimeException e) {
            return Result.failed(reader.name(), describe(e));
        }
    }

    private static int sanitize(WaypointGroup group) {
        if (group.waypoints == null) group.waypoints = new ArrayList<>();
        if (group.waypoints.size() > MAX_POINTS) {
            throw new IllegalArgumentException("too many waypoints (max " + MAX_POINTS + ")");
        }
        group.name = cut(group.name, MAX_GROUP_NAME);
        group.description = cut(group.description, MAX_DESCRIPTION);

        List<WaypointPoint> valid = new ArrayList<>(group.waypoints.size());
        int skipped = 0;
        for (WaypointPoint point : group.waypoints) {
            if (!isValid(point)) {
                skipped++;
                continue;
            }
            if (point.name == null || point.name.isEmpty()) point.name = String.valueOf(valid.size() + 1);
            point.name = cut(point.name, MAX_POINT_NAME);
            valid.add(point);
        }
        if (valid.isEmpty() && skipped > 0) throw new IllegalArgumentException("no valid coordinates found");
        group.waypoints = valid;
        return skipped;
    }

    private static boolean isValid(WaypointPoint point) {
        if (point == null) return false;
        return isCoordinate(point.x) && isCoordinate(point.y) && isCoordinate(point.z);
    }

    private static boolean isCoordinate(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && Math.abs(value) <= MAX_COORDINATE;
    }

    private static String cut(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String describe(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isEmpty() ? "damaged text" : message;
    }

    // format is null only when no reader matched
    @Getter
    public static final class Result {

        private final String format;
        private final WaypointGroup group;
        private final String error;
        private final int skipped;

        private Result(String format, WaypointGroup group, String error, int skipped) {
            this.format = format;
            this.group = group;
            this.error = error;
            this.skipped = skipped;
        }

        static Result found(String format, WaypointGroup group, int skipped) {
            return new Result(format, group, null, skipped);
        }

        static Result failed(String format, String error) {
            return new Result(format, null, error, 0);
        }

        static Result unmatched() {
            return new Result(null, null, "not a waypoint export", 0);
        }

        public boolean isFailed() {
            return group == null;
        }

        public boolean isMatched() {
            return format != null;
        }
    }
}
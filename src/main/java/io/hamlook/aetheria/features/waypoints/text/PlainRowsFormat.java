package io.hamlook.aetheria.features.waypoints.text;

import io.hamlook.aetheria.features.waypoints.WaypointGroup;
import io.hamlook.aetheria.features.waypoints.WaypointPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// plain text rows, x y z per line, read only
// rows that are not three numbers are skipped
final class PlainRowsFormat implements WaypointTextFormat {

    private static final Pattern ANY_DIGIT = Pattern.compile("\\d");
    private static final Pattern LINE_BREAK = Pattern.compile("[\\r\\n]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    @Override
    public String name() {
        return "rows";
    }

    @Override
    public boolean matches(String text) {
        return ANY_DIGIT.matcher(text).find();
    }

    @Override
    public WaypointGroup read(String text) {
        List<WaypointPoint> points = new ArrayList<>();
        for (String row : LINE_BREAK.split(text)) {
            String[] parts = SPACES.split(row.trim());
            if (parts.length < 3) continue;
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                points.add(new WaypointPoint(x, y, z, String.valueOf(points.size() + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (points.isEmpty()) throw new IllegalArgumentException("no coordinate rows found");
        WaypointGroup group = new WaypointGroup();
        group.waypoints = points;
        return group;
    }
}

package io.hamlook.aetheria.features.waypoints.text;

import com.google.gson.*;
import io.hamlook.aetheria.features.waypoints.WaypointGroup;
import io.hamlook.aetheria.features.waypoints.WaypointPoint;

import java.util.ArrayList;
import java.util.List;

// json array used by other waypoint mods
// read only keeps old exports importable
// points with numeric names are sorted, as the other mods expect
final class SoopyFormat implements WaypointTextFormat {

    private static String pointName(JsonObject entry, int index) {
        JsonElement options = entry.get("options");
        if (options == null || !options.isJsonObject()) return String.valueOf(index + 1);
        JsonElement name = options.getAsJsonObject().get("name");
        if (name == null || name.isJsonNull() || !name.isJsonPrimitive()) return String.valueOf(index + 1);
        return name.getAsString();
    }

    private static double number(JsonObject entry, String key) {
        JsonElement element = entry.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }
        if (element != null && element.isJsonPrimitive()) {
            try {
                return Double.parseDouble(element.getAsString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("text is not a waypoint list");
            }
        }
        throw new IllegalArgumentException("text is not a waypoint list");
    }

    @Override
    public String name() {
        return "soopy";
    }

    @Override
    public boolean matches(String text) {
        return text.startsWith("[");
    }

    @Override
    public WaypointGroup read(String text) {
        JsonArray array;
        try {
            JsonElement element = JsonParser.parseString(text);
            if (!element.isJsonArray()) throw new IllegalStateException();
            array = element.getAsJsonArray();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("text is not a waypoint list");
        }
        List<WaypointPoint> points = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) throw new IllegalArgumentException("text is not a waypoint list");
            JsonObject entry = element.getAsJsonObject();
            points.add(new WaypointPoint(number(entry, "x"), number(entry, "y"), number(entry, "z"), pointName(entry, index)));
        }
        points.sort((left, right) -> {
            try {
                return Integer.compare(Integer.parseInt(left.name), Integer.parseInt(right.name));
            } catch (NumberFormatException e) {
                return 0;
            }
        });
        WaypointGroup group = new WaypointGroup();
        group.waypoints = points;
        return group;
    }

    String write(WaypointGroup group) {
        JsonArray array = new JsonArray();
        for (WaypointPoint point : group.waypoints) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", point.x);
            entry.addProperty("y", point.y);
            entry.addProperty("z", point.z);
            entry.addProperty("r", 0);
            entry.addProperty("g", 1);
            entry.addProperty("b", 0);
            JsonObject options = new JsonObject();
            options.addProperty("name", point.name == null ? "" : point.name);
            entry.add("options", options);
            array.add(entry);
        }
        return new Gson().toJson(array);
    }
}

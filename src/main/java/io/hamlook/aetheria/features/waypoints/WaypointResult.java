package io.hamlook.aetheria.features.waypoints;

public final class WaypointResult {

    public final boolean ok;
    public final String message;

    private WaypointResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static WaypointResult ok(String message) {
        return new WaypointResult(true, message);
    }

    public static WaypointResult fail(String message) {
        return new WaypointResult(false, message);
    }
}

package io.hamlook.aetheria.features.waypoints.text;

import io.hamlook.aetheria.features.waypoints.WaypointGroup;

// one way of reading waypoint text
// matches is asked first, read only when matches
// unknown fields are ignored, missing ones get defaults
// read throws with a message for the player
public interface WaypointTextFormat {

    String name();

    boolean matches(String text);

    WaypointGroup read(String text);
}

package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import net.minecraft.client.gui.FontRenderer;

public interface MenuTab {

    String title();

    void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, int mouseX, int mouseY);

    default void onShow(WaypointGroupGui gui) {
    }

    default boolean mouse(WaypointGroupGui gui, int mouseX, int mouseY, int button, boolean pressed) {
        return false;
    }
    default boolean scroll(WaypointGroupGui gui, int amount) {
        return false;
    }
    default boolean key(WaypointGroupGui gui, char typedChar, int keyCode) {
        return false;
    }
    default boolean capturesEscape() {
        return false;
    }

    default boolean hasOverlay() {
        return false;
    }

    default void renderOverlay(WaypointGroupGui gui, FontRenderer fr) {
    }
}

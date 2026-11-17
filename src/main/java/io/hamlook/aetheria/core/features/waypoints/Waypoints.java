package io.hamlook.aetheria.core.features.waypoints;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorButton;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorKeybind;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class Waypoints {

    @Expose
    @ConfigOption(name = "Manage Waypoints", desc = "Open waypoint manager")
    @ConfigEditorButton(runnableId = "openWaypointGroupGui", buttonText = "Open")
    public boolean manageGroupsDummy = false;

    @Expose
    @ConfigOption(name = "Manager Key", desc = "Keybind to open the waypoint manager")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int waypointManagerKey = Keyboard.KEY_NONE;

    @Expose
    @Category(name = "Colors", desc = "Waypoint rendering colors")
    public WaypointColorsConfig colors = new WaypointColorsConfig();

    @Expose
    @Category(name = "Auto Advance", desc = "Settings for automatic waypoint progression")
    public AutoAdvanceConfig autoAdvance = new AutoAdvanceConfig();
}
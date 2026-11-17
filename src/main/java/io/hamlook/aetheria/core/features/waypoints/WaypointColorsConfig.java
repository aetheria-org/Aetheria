package io.hamlook.aetheria.core.features.waypoints;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class WaypointColorsConfig {

    @Expose
    @ConfigOption(name = "Box Colour", desc = "Colour of the ESP box drawn around the next waypoint")
    @ConfigEditorColour
    public String boxColour = "0:217:255:255:0";

    @Expose
    @ConfigOption(name = "Tracer Colour", desc = "Colour of the tracer from your position to the next waypoint")
    @ConfigEditorColour
    public String tracerColour = "0:255:255:255:0";

    @Expose
    @ConfigOption(name = "Label Colour", desc = "Colour of the waypoint name / number above each waypoint")
    @ConfigEditorColour
    public String labelColour = "0:255:255:255:255";

    @Expose
    @ConfigOption(name = "Distance Colour", desc = "Colour of the distance number shown next to each waypoint label")
    @ConfigEditorColour
    public String distanceLabelColour = "0:255:85:255:255";
}

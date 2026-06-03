package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations.*;

public class DungeonSecretFinderConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable secret finder and rotation detection in dungeon rooms")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @Category(name = "Toggles", desc = "Toggle various secret finder visual features on/off")
    public Toggles toggles = new Toggles();

    @Expose
    @Category(name = "Colors", desc = "Color settings for secret finder visuals")
    public Colors colors = new Colors();

    @Expose
    @Category(name = "Range", desc = "Distance ranges for auto-removing secrets upon collection")
    public Range range = new Range();

    @Expose
    @Category(name = "Other", desc = "Miscellaneous secret finder settings")
    public Other other = new Other();

    public static class Toggles {

        @Expose
        @ConfigOption(name = "Show Labels", desc = "Show text labels above secrets")
        @ConfigEditorBoolean
        public boolean showLabels = true;

        @Expose
        @ConfigOption(name = "Show Waypoints", desc = "Show beacon-style beams from sky to each secret")
        @ConfigEditorBoolean
        public boolean showWaypoints = true;

        @Expose
        @ConfigOption(name = "Show Tracer", desc = "Draw a tracer line to the nearest uncollected secret")
        @ConfigEditorBoolean
        public boolean showTracer = true;

        @Expose
        @ConfigOption(name = "Show Border Box", desc = "Show wireframe border boxes around secrets")
        @ConfigEditorBoolean
        public boolean showBorderBox = false;

        @Expose
        @ConfigOption(name = "Show Bounding Box", desc = "Show filled bounding boxes around secrets")
        @ConfigEditorBoolean
        public boolean showBoundingBox = false;

        @Expose
        @ConfigOption(name = "Show Through Walls", desc = "Waypoints and boxes appear even when behind walls")
        @ConfigEditorBoolean
        public boolean showThroughWalls = false;

        @Expose
        @ConfigOption(name = "Compact Labels", desc = "Show compact labels (\"1\") instead of full (\"1 - Chest\")")
        @ConfigEditorBoolean
        public boolean compactLabels = false;
    }

    public static class Colors {

        @Expose
        @ConfigOption(name = "Label Color", desc = "Color for secret label text")
        @ConfigEditorColour
        public String labelColor = "0:200:255:255:0";

        @Expose
        @ConfigOption(name = "Box Color", desc = "Color for secret bounding boxes (filled boxes)")
        @ConfigEditorColour
        public String boxColor = "0:80:255:200:0";

        @Expose
        @ConfigOption(name = "Waypoint Color", desc = "Color for secret waypoint beams")
        @ConfigEditorColour
        public String waypointColor = "0:200:255:255:0";

        @Expose
        @ConfigOption(name = "Tracer Color", desc = "Color for secret tracers")
        @ConfigEditorColour
        public String tracerColor = "0:200:255:255:0";
    }

    public static class Range {

        @Expose
        @ConfigOption(name = "Item Pickup Range", desc = "Max distance from player to auto-remove item secret waypoints when the action bar counter changes (higher = more aggressive removal, may false-positive)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double itemRemovalRange = 2.0;

        @Expose
        @ConfigOption(name = "Entrance Range", desc = "Horizontal distance to auto-remove entrance waypoints when you walk past them (higher = more forgiving, may remove before you actually enter)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double entranceRemovalRange = 1.5;

        @Expose
        @ConfigOption(name = "Interact Range", desc = "Max distance from a clicked block to mark a chest/lever/essence secret as collected (higher = less precise, may remove wrong secret)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double interactRemovalRange = 1.5;

        @Expose
        @ConfigOption(name = "Superboom Range", desc = "Max distance from a primed TNT to mark a superboom secret as collected (higher = more forgiving, may remove before it actually breaks)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double superboomRemovalRange = 2.0;
    }

    public static class Other {

        @Expose
        @ConfigOption(name = "Secret Check Interval", desc = "How often (in seconds) to check if essence secrets still have their skull block. Lower = faster removal, higher = better performance.")
        @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 30f, minStep = 1f)
        public double updateInterval = 2.0;

        @Expose
        @ConfigOption(name = "Label Scale", desc = "Size multiplier for secret labels (higher = larger text)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.25f)
        public double labelScale = 1.0;

        @Expose
        @ConfigOption(name = "Tracer Width", desc = "Width of tracer lines and room border outlines (higher = thicker)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 10.0f, minStep = 0.25f)
        public float tracerWidth = 1.0f;
    }
}

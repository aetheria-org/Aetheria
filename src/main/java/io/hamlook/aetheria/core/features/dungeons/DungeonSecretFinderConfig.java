package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class DungeonSecretFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Draw secret waypoints (labels, boxes, fills, tracers and beams)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @Category(name = "Waypoints", desc = "How secrets are drawn in the world")
    public Waypoints waypoints = new Waypoints();

    @Expose
    @Category(name = "Colors", desc = "Color settings for secret finder visuals")
    public Colors colors = new Colors();

    @Expose
    @Category(name = "Secrets", desc = "When secrets are marked as collected")
    public Secrets secrets = new Secrets();

    public static DungeonSecretFinderConfig get() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.dungeons.secrets.finder;
    }

    public static class Waypoints {

        @Expose
        @ConfigOption(name = "Labels", desc = "Show text labels above secrets")
        @ConfigEditorBoolean
        public boolean showLabels = true;

        @Expose
        @ConfigOption(name = "Box ESP", desc = "Draw an outline box around each secret. Visibility through walls follows Through Walls.")
        @ConfigEditorBoolean
        public boolean showBoxOutline = true;

        @Expose
        @ConfigOption(name = "Full Block", desc = "Draw a translucent filled box around each secret")
        @ConfigEditorBoolean
        public boolean showBoxFill = false;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draw a tracer line to an uncollected secret")
        @ConfigEditorBoolean
        public boolean showTracer = true;

        @Expose
        @ConfigOption(name = "Tracer Target", desc = "Which secret the tracer points at: the closest one, or the next one in the room's secret order")
        @ConfigEditorDropdown(values = {"Closest", "Next in Order"})
        public int tracerTarget = 0;

        @Expose
        @ConfigOption(name = "Through Walls", desc = "Draw labels, boxes, blocks, beams and solids even when behind walls")
        @ConfigEditorBoolean
        public boolean showThroughWalls = true;

        @Expose
        @ConfigOption(name = "Beams", desc = "Show a beam rising from each secret")
        @ConfigEditorBoolean
        public boolean showWaypoints = false;

        @Expose
        @ConfigOption(name = "Short Labels", desc = "Show short labels (\"1\") instead of full (\"1 - Chest\")")
        @ConfigEditorBoolean
        public boolean compactLabels = false;

        @Expose
        @ConfigOption(name = "Label Scale", desc = "Size multiplier for secret labels (higher = larger text)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.25f)
        public double labelScale = 1.0;

        @Expose
        @ConfigOption(name = "Tracer Width", desc = "Width of tracer lines and room border outlines (higher = thicker)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 10.0f, minStep = 0.25f)
        public float tracerWidth = 1.0f;

        @Expose
        @Category(name = "Target", desc = "Highlight the secret the tracer points at")
        public Target target = new Target();

        public static class Target {

            @Expose
            @ConfigOption(name = "Box", desc = "Draw a separate box around the secret the tracer points at")
            @ConfigEditorBoolean
            public boolean targetEsp = true;

            @Expose
            @ConfigOption(name = "Color", desc = "Color for the target secret box")
            @ConfigEditorColour
            public String targetColor = "0:220:255:60:60";

            @Expose
            @ConfigOption(name = "Box Width", desc = "Line width of the target secret box (higher = thicker)")
            @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 10f, minStep = 0.25f)
            public float targetWidth = 2.5f;

            @Expose
            @ConfigOption(name = "Through Walls", desc = "Draw the target box even when behind walls")
            @ConfigEditorBoolean
            public boolean throughWalls = true;
        }
    }

    public static class Colors {

        @Expose
        @ConfigOption(name = "Label Color", desc = "Color for secret label text")
        @ConfigEditorColour
        public String labelColor = "0:200:255:253:250";

        @Expose
        @ConfigOption(name = "Box ESP Color", desc = "Color for secret box outlines")
        @ConfigEditorColour
        public String outlineColor = "0:80:0:242:255";

        @Expose
        @ConfigOption(name = "Full Block Color", desc = "Color for filled secret boxes")
        @ConfigEditorColour
        public String fillColor = "0:80:255:200:0";

        @Expose
        @ConfigOption(name = "Beam Color", desc = "Color for secret beams")
        @ConfigEditorColour
        public String waypointColor = "0:200:54:208:255";

        @Expose
        @ConfigOption(name = "Tracer Color", desc = "Color for secret tracers")
        @ConfigEditorColour
        public String tracerColor = "0:200:255:0:192";
    }

    public static class Secrets {

        @Expose
        @ConfigOption(name = "Item Horizontal Range", desc = "§c§lDo not change. §rHorizontal reach (blocks) in which picking up a dropped secret item counts as the secret; tuned for dropped items.")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 12.0f, minStep = 0.5f)
        public double itemHorizontalRange = 7.5;

        @Expose
        @ConfigOption(name = "Item Vertical Range", desc = "§c§lDo not change. §rHow far below the secret block a picked-up item may be; dropped items can fall far.")
        @ConfigEditorSliderAnnotation(minValue = 2f, maxValue = 40f, minStep = 1f)
        public double itemVerticalRange = 20.0;

        @Expose
        @ConfigOption(name = "Entrance", desc = "Horizontal distance to auto-remove entrance waypoints when you walk past them (higher = more forgiving, may remove before you actually enter)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double entranceRemovalRange = 1.5;

        @Expose
        @ConfigOption(name = "Superboom", desc = "Max distance from a primed TNT to mark a superboom secret as collected (higher = more forgiving, may remove before it actually breaks)")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5.0f, minStep = 0.5f)
        public double superboomRemovalRange = 2.0;

        @Expose
        @ConfigOption(name = "Essence Check Interval", desc = "How often (in seconds) to check if essence secrets still have their skull block. Lower = faster removal, higher = better performance.")
        @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 30f, minStep = 1f)
        public double updateInterval = 2.0;
    }
}

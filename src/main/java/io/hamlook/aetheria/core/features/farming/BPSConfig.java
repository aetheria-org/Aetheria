package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class BPSConfig {

    @Expose
    @ConfigOption(name = "Enable BPS Calculator", desc = "Shows blocks broken per second while farming")
    @ConfigEditorBoolean
    public boolean bpsCalculator = true;

    @Expose
    @ConfigOption(name = "Hide in Chat", desc = "Hide the overlay when the chat GUI is open")
    @ConfigEditorBoolean
    public boolean hideOnChat = true;

    @Expose
    @ConfigOption(name = "Hide on Tab", desc = "Hide the overlay when the tab list is shown")
    @ConfigEditorBoolean
    public boolean hideOnTab = true;

    @Expose
    @ConfigOption(name = "Hide on F3 Debug", desc = "Hide the overlay when the F3 debug screen is open")
    @ConfigEditorBoolean
    public boolean hideOnDebug = true;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Edit the position of the BPS overlay")
    @ConfigEditorButton(runnableId = "openBpsEditor")
    public int bpsEditPosition = 0;

    @Expose
    @ConfigOption(name = "Require Farming Location", desc = "Only count blocks while in farming locations (Barn, Private Island)")
    @ConfigEditorBoolean
    public boolean bpsRequireFarmingIsland = true;

    @Expose
    @ConfigOption(name = "Show Only While Farming", desc = "Only show the overlay while actively farming (holding a farming tool and breaking crops)")
    @ConfigEditorBoolean
    public boolean showOnlyWhileFarming = true;

    @Expose
    @ConfigOption(name = "Reset Timeout", desc = "Seconds of inactivity before resetting BPS counter")
    @ConfigEditorSliderAnnotation(minValue = 1, maxValue = 30, minStep = 1)
    public int bpsResetTimeout = 5;

    @Expose
    public Position bpsPosition = new Position(-289, 43, false, false);

    @Expose
    @ConfigOption(name = "Scale", desc = "Scale of the BPS overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2.0f, minStep = 0.1f)
    public float bpsScale = 1.0f;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the BPS overlay")
    @ConfigEditorColour
    public int bpsBgColor = 0x80000000;

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Corner radius of the BPS overlay background")
    @ConfigEditorSliderAnnotation(minValue = 0, maxValue = 10, minStep = 1)
    public int bpsCornerRadius = 3;
}

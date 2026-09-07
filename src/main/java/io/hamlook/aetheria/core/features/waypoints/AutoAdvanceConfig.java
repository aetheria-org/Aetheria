package io.hamlook.aetheria.core.features.waypoints;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class AutoAdvanceConfig {

    @Expose
    @ConfigOption(name = "Advance Range", desc = "How close (blocks) you must be to the next waypoint before the timer starts")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 30f, minStep = 0.5f)
    public float advanceRange = 5.0f;

    @Expose
    @ConfigOption(name = "Advance Delay (ms)", desc = "How long (ms) you must stay within range before the waypoint auto-advances")
    @ConfigEditorSliderAnnotation(minValue = 250f, maxValue = 10000f, minStep = 250f)
    public float advanceDelayMs = 2000f;
}

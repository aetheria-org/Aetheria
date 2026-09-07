package io.hamlook.aetheria.core.features.fishing;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class FishingTimerConfig {

    @Expose
    @ConfigOption(name = "Enable Timer", desc = "Show fishing timer while fishing")
    @ConfigEditorBoolean
    public boolean fishingTimer = true;

    @Expose
    @ConfigOption(name = "Alert Time (seconds)", desc = "Time after which alert sound plays")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 60f, minStep = 1f)
    public int fishingTimerAlertTime = 15;

    @Expose
    @ConfigOption(name = "Normal Color", desc = "Text color before alert time")
    @ConfigEditorColour
    public String fishingTimerNormalColor = "237:255:255:0:0";

    @Expose
    @ConfigOption(name = "Alert Color", desc = "Text color after alert time")
    @ConfigEditorColour
    public String fishingTimerAlertColor = "0:255:255:246:0";
}

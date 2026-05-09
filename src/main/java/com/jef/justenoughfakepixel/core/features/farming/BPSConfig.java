package com.jef.justenoughfakepixel.core.features.farming;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;
import com.jef.justenoughfakepixel.core.config.utils.Position;

public class BPSConfig {

    @Expose
    @ConfigOption(name = "Enable BPS Calculator", desc = "Shows blocks broken per second while farming")
    @ConfigEditorBoolean
    public boolean bpsCalculator = false;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Edit the position of the BPS overlay")
    @ConfigEditorButton(runnableId = "openBpsEditor")
    public int bpsEditPosition = 0;

    @Expose
    @ConfigOption(name = "Require Farming Location", desc = "Only count blocks while in farming locations (Barn, Private Island)")
    @ConfigEditorBoolean
    public boolean bpsRequireFarmingIsland = true;

    @Expose
    @ConfigOption(name = "Reset Timeout", desc = "Seconds of inactivity before resetting BPS counter")
    @ConfigEditorSliderAnnotation(minValue = 1, maxValue = 30, minStep = 1)
    public int bpsResetTimeout = 5;

    @Expose
    public Position bpsPosition = new Position(10, 100, false, false);

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

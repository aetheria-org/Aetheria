package com.jef.justenoughfakepixel.core.features.overlays;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;

public class ProfileViewerConfig {

    @Expose
    @ConfigOption(name = "Scale", desc = "Adjust the overall scale of the overlay GUI")
    @ConfigEditorSliderAnnotation(minValue = 0.5f,maxValue = 1.5f,minStep = 0.1f)
    public float pvScale = 1f;

}

package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class GyroWandConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Shows the area of effect ring when holding the Gyrokinetic Wand")
    @ConfigEditorBoolean
    public boolean gyroWand = true;

    @Expose
    @ConfigOption(name = "Ring Thickness", desc = "Thickness of the area of effect ring")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5f, minStep = 0.5f)
    public float gyroWandThickness = 2f;
}

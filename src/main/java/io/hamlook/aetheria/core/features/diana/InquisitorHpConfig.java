package io.hamlook.aetheria.core.features.diana;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import io.hamlook.aetheria.utils.Position;

public class InquisitorHpConfig {

    @Expose
    @ConfigOption(name = "Show Inquisitor HP", desc = "Show a live HP bar for the nearest Minos Inquisitor")
    @ConfigEditorBoolean
    public boolean showInqHealthOverlay = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the Inquisitor HP overlay (alpha controls opacity)")
    @ConfigEditorColour
    public String inqBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the Inquisitor HP overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int inqCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the Inquisitor HP overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float inqScale = 1f;

    @Expose
    public Position inqHealthPos = new Position(4, 400);
}

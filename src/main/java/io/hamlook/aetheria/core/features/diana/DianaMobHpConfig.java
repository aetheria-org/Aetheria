package io.hamlook.aetheria.core.features.diana;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import io.hamlook.aetheria.utils.Position;

public class DianaMobHpConfig {

    @Expose
    @ConfigOption(name = "Show Diana Mob HP", desc = "Show a live HP bar for the nearest non-Inquisitor Diana mob – only appears after you dig one out")
    @ConfigEditorBoolean
    public boolean showDianaMobHealthOverlay = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the Diana Mob HP overlay (alpha controls opacity)")
    @ConfigEditorColour
    public String mobBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the Diana Mob HP overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int mobCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the Diana Mob HP overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float mobScale = 1f;

    @Expose
    public Position dianaMobHealthPos = new Position(4, 420);
}

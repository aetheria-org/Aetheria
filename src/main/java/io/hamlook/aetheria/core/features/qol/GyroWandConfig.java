package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class GyroWandConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Shows the area of effect ring when holding the Gyrokinetic Wand")
    @ConfigEditorBoolean
    public boolean gyroWand = true;

    @Expose
    @ConfigOption(name = "Display Mode", desc = "Ring shows a flat band; Cylinder shows a wireframe column")
    @ConfigEditorDropdown(values = {"Ring", "Cylinder"})
    //#if MC < 1.21
    public int gyroWandMode = 0;
    //#else
    //$$ public int gyroWandMode = 1;
    //#endif

    @Expose
    @ConfigOption(name = "Ring Thickness", desc = "Thickness of the area of effect ring (no effect in cylinder mode)")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 5f, minStep = 0.5f)
    public float gyroWandThickness = 2f;

    @Expose
    @ConfigOption(name = "Ready Color", desc = "Color when the wand is ready")
    @ConfigEditorColour
    public String gyroWandReadyColor = "0:153:153:26:204";

    @Expose
    @ConfigOption(name = "Cooldown Color", desc = "Color while the wand is on cooldown")
    @ConfigEditorColour
    public String gyroWandCooldownColor = "0:153:255:51:51";
}

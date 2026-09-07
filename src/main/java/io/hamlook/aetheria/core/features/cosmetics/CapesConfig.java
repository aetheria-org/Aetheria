package io.hamlook.aetheria.core.features.cosmetics;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorButton;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class CapesConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable/Disable whether you use your custom cape")
    @ConfigEditorBoolean
    public boolean capesEnabled = true;

    @Expose
    @ConfigOption(name = "Reload Capes", desc = "Reload and refetch all cape textures.")
    @ConfigEditorButton(runnableId = "reloadCapes", buttonText = "Reload")
    public String reloadCapes = "";

    @Expose
    @ConfigOption(name = "Reload Interval", desc = "Change how many minutes it takes before reloading all capes, must Reload Capes to take it into effect. [ HIGHLY AFFECTS PERFORMANCE, KEEP HIGHER FOR SMOOTHER GAMEPLAY ]")
    @ConfigEditorSliderAnnotation(minValue = 5, maxValue = 60, minStep = 1)
    public int reloadInterval = 15;
}

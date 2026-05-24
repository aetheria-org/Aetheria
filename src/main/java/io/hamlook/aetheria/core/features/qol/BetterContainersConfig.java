package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations.*;

public class BetterContainersConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable Improved Skyblock menus")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Style", desc = "Visual style for the chest background (1–7)")
    @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3", "Style 4", "Style 5", "Style 6", "Style 7"})
    public int style = 0;
}

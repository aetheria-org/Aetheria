package io.hamlook.aetheria.core.features.cosmetics;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class MainMenuConfig {

    @ConfigOption(name = "Enable Custom Main Menu",desc = "Enable/Disable the Custom Main Menu")
    @ConfigEditorBoolean
    public boolean enabled = true;

}

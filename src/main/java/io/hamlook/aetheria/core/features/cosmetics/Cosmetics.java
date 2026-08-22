package io.hamlook.aetheria.core.features.cosmetics;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
public class Cosmetics {

    @Expose
    @Category(name = "Capes", desc = "Settings for the Capes")
    public CapesConfig capes = new CapesConfig();


    @Expose
    @Category(name = "Custom Main Menu", desc = "Settings for customising the custom main menu")
    public MainMenuConfig customMenu = new MainMenuConfig();
}
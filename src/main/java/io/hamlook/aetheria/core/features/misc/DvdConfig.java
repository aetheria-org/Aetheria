package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class DvdConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the bouncing DVD logo screensaver while AFK/in menu")
    @ConfigEditorBoolean
    public boolean dvdScreensaver = false;

    @Expose
    @ConfigOption(name = "DVD Size", desc = "Size of the DVD logo (width in pixels)")
    @ConfigEditorSliderAnnotation(minValue = 50, maxValue = 300, minStep = 10)
    public int dvdSize = 80;

    @Expose
    @ConfigOption(name = "Hide in Chat", desc = "Hide the DVD logo when the chat GUI is open")
    @ConfigEditorBoolean
    public boolean hideOnChat = true;

    @Expose
    @ConfigOption(name = "Hide on Tab", desc = "Hide the DVD logo when the tab list is shown")
    @ConfigEditorBoolean
    public boolean hideOnTab = true;

    @Expose
    @ConfigOption(name = "Hide on F3 Debug", desc = "Hide the DVD logo when the F3 debug screen is open")
    @ConfigEditorBoolean
    public boolean hideOnDebug = true;
}

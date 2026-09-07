package io.hamlook.aetheria.core.features.storage;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class Storage {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable Custom Storage Overlay")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Scroll Speed", desc = "Change how fast or slow the scrolling is")
    @ConfigEditorSliderAnnotation(minValue = 0.1f, maxValue = 3, minStep = 0.01f)
    public float scrollSpeed = 1f;

    @Expose
    @ConfigOption(name = "Overlay Style", desc = "Choose the visual style of the storage overlay panels and slots")
    @ConfigEditorDropdown(values = {"Default", "Dark", "Wooden", "Ender", "Parchment"})
    public int overlayStyle = 0;

    @Expose
    @ConfigOption(name = "Active Container Highlight", desc = "How inactive backpacks are visually de-emphasised when one is selected")
    @ConfigEditorDropdown(values = {"Dim", "Classic"})
    public int activeContainerStyle = 0;

    @Expose
    @ConfigOption(name = "AutoJump to Active", desc = "Automatically scroll to the active container")
    @ConfigEditorBoolean
    public boolean autoScrollToActive = true;
}
package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class BetterContainersConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable Improved Skyblock menus")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Style", desc = "Visual style for the chest background (1–7)")
    @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3", "Style 4", "Style 5", "Style 6", "Style 7"})
    public int style = 0;

    @Expose
    @ConfigOption(name = "Hide Empty Pane Hover", desc = "Treat bound empty panes as absent: no hover highlight, tooltip or clicks")
    @ConfigEditorBoolean
    public boolean hideEmptyPaneHover = true;

    @Expose
    @ConfigOption(name = "Watermark Color", desc = "Color of the 'ASM' watermark in the top-right of chests. Enable chroma speed for animated chroma.")
    @ConfigEditorColour
    public String watermarkColor = "200:255:50:50:255";

    @Expose
    @ConfigOption(name = "Watermark Chroma Mode", desc = "0 = no spatial shift, 1 = diagonal wave (same as enchant chroma)")
    @ConfigEditorDropdown(values = {"None", "Diagonal"})
    public int watermarkChromaMode = 0;

    @Expose
    @ConfigOption(name = "Watermark Chroma Size", desc = "Wave size for the spatial chroma shift")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 200f, minStep = 1f)
    public float watermarkChromaSize = 50f;
}

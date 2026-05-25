package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations;

public class ChatFilterConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Enable ChatFilters", desc = "Enable the ChatFilters feture")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean chatFilters = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Open ChatFilters GUI", desc = "Open GUI to edit ChatFilters")
    @ConfigAnnotations.ConfigEditorButton(runnableId = "chatFiltersGUI",buttonText = "Open UI")
    public boolean openChatFilters = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "UI Scale", desc = "Scale multiplier for Chat Filters UI")
    @ConfigAnnotations.ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
    public float uiScale = 1.0f;
}

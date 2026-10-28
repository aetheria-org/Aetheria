package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ChatCompactingConfig {

    @Expose
    @ConfigOption(name = "Chat Compacting", desc = "Collapse repeated identical chat messages into one with a counter")
    @ConfigEditorBoolean
    public boolean compactingEnabled = true;

    @Expose
    @ConfigOption(name = "Expire Time (seconds)", desc = "How long until a compacted message resets. -1 = never expire")
    @ConfigEditorSliderAnnotation(minValue = -1, maxValue = 600, minStep = 1)
    public int expireTimeSeconds = 60;

    @Expose
    @ConfigOption(name = "Consecutive Only", desc = "Only compact messages that appear back-to-back with no other messages in between")
    @ConfigEditorBoolean
    public boolean consecutiveOnly = false;
}

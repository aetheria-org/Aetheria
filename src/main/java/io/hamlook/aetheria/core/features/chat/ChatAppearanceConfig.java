package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ChatAppearanceConfig {

    @Expose
    @ConfigOption(name = "Chat Background Opacity", desc = "Opacity of the chat background, 0 is fully transparent. Scales the vanilla opacity")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 100f, minStep = 1f)
    public int chatBackgroundOpacity = 100;

    @Expose
    @ConfigOption(name = "Animated Chat", desc = "New messages slide into view with a short animation")
    @ConfigEditorBoolean
    public boolean animatedChat = false;
}

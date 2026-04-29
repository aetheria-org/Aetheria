package com.jef.justenoughfakepixel.core.features.misc;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class ProtectItemConfig {

    @Expose
    @ConfigOption(name = "Protection Keybind", desc = "Hold the key and hover a slot (or hold an item) to toggle protection")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int protectionKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show Protected Star", desc = "Show a star overlay on items protected by /jefprotect")
    @ConfigEditorBoolean
    public boolean showProtectedStar = true;

    @Expose
    @ConfigOption(name = "Star Opacity", desc = "Opacity of the protection star overlay (0-100%)")
    @ConfigEditorSliderAnnotation(minValue = 0, maxValue = 100, minStep = 5)
    public int starOpacity = 100;

    @Expose
    @ConfigOption(name = "Show Chat Notifications", desc = "Show chat messages when protection blocks an action")
    @ConfigEditorBoolean
    public boolean showChatNotifications = true;
}
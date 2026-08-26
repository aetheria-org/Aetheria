package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class PestAlertConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Master toggle for pest cooldown alerts (off by default)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Alert Below", desc = "Seconds remaining at which the alert fires")
    @ConfigEditorSliderAnnotation(minValue = 5f, maxValue = 300f, minStep = 5f)
    public int alertBelowSeconds = 30;

    @Expose
    @ConfigOption(name = "On-Screen Alert", desc = "Show an on-screen banner while the alert is active")
    @ConfigEditorBoolean
    public boolean showOnScreen = true;

    @Expose
    @ConfigOption(name = "Chat Message", desc = "Send a chat message once when the cooldown crosses below the alert time")
    @ConfigEditorBoolean
    public boolean chatMessage = true;

    @Expose
    @ConfigOption(name = "Sound", desc = "Play a sound when the cooldown crosses below the alert time")
    @ConfigEditorBoolean
    public boolean playSound = true;

    @Expose
    @ConfigOption(name = "Alert Sound", desc = "Which sound the alert plays")
    @ConfigEditorDropdown(values = {"Pling", "Dragon Roar", "Cat Meow", "Orb", "Level Up"}, initialIndex = 0)
    public int alertSound = 0;

    @Expose
    @ConfigOption(name = "Alert Scale", desc = "Size of the on-screen alert text")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 4f, minStep = 0.25f)
    public float alertScale = 2f;

    @Expose
    @ConfigOption(name = "Fade In/Out", desc = "Flash and fade the alert text while it is shown")
    @ConfigEditorBoolean
    public boolean fadeInOut = true;
}

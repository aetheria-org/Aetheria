package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class BalConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Track Bal in the Crystal Hollows and alert before it respawns")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Respawn Time", desc = "Minutes between Bal's defeat and its respawn")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 10f, minStep = 1f)
    public int respawnMinutes = 5;

    @Expose
    @ConfigOption(name = "Alert Before", desc = "Seconds remaining when the respawn alert fires")
    @ConfigEditorSliderAnnotation(minValue = 5f, maxValue = 300f, minStep = 5f)
    public int alertBeforeSeconds = 90;

    @Expose
    @ConfigOption(name = "On-Screen Alert", desc = "Show an on-screen banner when the alert fires")
    @ConfigEditorBoolean
    public boolean showOnScreen = true;

    @Expose
    @ConfigOption(name = "Chat Message", desc = "Send a chat message when the alert fires")
    @ConfigEditorBoolean
    public boolean chatMessage = true;

    @Expose
    @ConfigOption(name = "Sound", desc = "Play a sound when the alert fires")
    @ConfigEditorBoolean
    public boolean playSound = true;

    @Expose
    @ConfigOption(name = "Alert Sound", desc = "Which sound the alert plays")
    @ConfigEditorDropdown(values = {"Pling", "Dragon Roar", "Cat Meow", "Orb", "Level Up"})
    public int alertSound = 0;

    @Expose
    @ConfigOption(name = "Alert Scale", desc = "Size of the on-screen alert text")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 4f, minStep = 0.25f)
    public float alertScale = 2f;

    @Expose
    @ConfigOption(name = "Fade In/Out", desc = "Flash and fade the alert text while it is shown")
    @ConfigEditorBoolean
    public boolean fadeInOut = true;

    @Expose
    @ConfigOption(name = "Clear On Spawn", desc = "Clear the timer when Bal's spawn message appears")
    @ConfigEditorBoolean
    public boolean clearOnSpawn = true;
}

package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class SecretSoundsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Play a sound when a secret is collected")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Sound", desc = "Chime to play. Custom uses the Custom Sound name.")
    @ConfigEditorDropdown(values = {"Blaze Hit", "Fire Ignite", "Orb", "Block Break", "Guardian Hit", "Pling", "Custom"})
    public int sound = 0;

    @Expose
    @ConfigOption(name = "Custom Sound", desc = "Sound name used when Sound is Custom. Typed as-is, so a name that is not mapped must already be the target version's name.")
    @ConfigEditorText
    public String customSound = "mob.blaze.hit";

    @Expose
    @ConfigOption(name = "Volume", desc = "Chime volume")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 1f, minStep = 0.05f)
    public float volume = 1.0f;

    @Expose
    @ConfigOption(name = "Pitch", desc = "Chime pitch, higher is shriller")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2f, minStep = 0.05f)
    public float pitch = 2.0f;

    @Expose
    @ConfigOption(name = "Play", desc = "Play the current sound with these settings")
    @ConfigEditorButton(runnableId = "testSecretChime", buttonText = "Play")
    public boolean testSound = false;
}

package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

import java.util.LinkedHashMap;

public class ItemLogAlertsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show on-screen alerts when configured items are picked up")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Alert Mode", desc = "Always: alert every time the item is picked up. First Time Only: only alert the first time you pick up that item per session.")
    @ConfigEditorDropdown(values = {"Always", "First Time Only"})
    public int alertMode = 1;

    @Expose
    @ConfigOption(name = "Use Item Display Name", desc = "Show the item's original colored name instead of custom alert text")
    @ConfigEditorBoolean
    public boolean useDisplayName = true;

    @Expose
    @ConfigOption(name = "Play Sound", desc = "Play a sound when an alert triggers")
    @ConfigEditorBoolean
    public boolean playSound = true;

    @Expose
    public LinkedHashMap<String, AlertEntry> alerts = new LinkedHashMap<>();

    public static class AlertEntry {

        @Expose
        public String customText = "";

        public AlertEntry() {
        }

        public AlertEntry(String customText) {
            this.customText = customText;
        }
    }
}

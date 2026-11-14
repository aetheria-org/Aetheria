package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class TimestampConfig {

    @Expose
    @ConfigOption(name = "Timestamps", desc = "Prepend a timestamp to every incoming chat message")
    @ConfigEditorBoolean
    public boolean timestampsEnabled = false;

    @Expose
    @ConfigOption(name = "24-Hour Clock", desc = "Use 24-hour time format instead of 12-hour AM/PM")
    @ConfigEditorBoolean
    public boolean timestamp24Hour = true;

    @Expose
    @ConfigOption(name = "Show Seconds", desc = "Include seconds in the timestamp (HH:mm:ss)")
    @ConfigEditorBoolean
    public boolean timestampShowSeconds = false;

    @Expose
    @ConfigOption(name = "Timestamp Style", desc = "Style of the timestamp bracket. 0 = [HH:mm]  1 = <HH:mm>")
    @ConfigEditorDropdown(values = {"[HH:mm]", "<HH:mm>"}, initialIndex = 0)
    public int timestampStyle = 0;
}

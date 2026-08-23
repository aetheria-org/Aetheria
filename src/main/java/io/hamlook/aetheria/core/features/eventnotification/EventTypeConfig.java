package io.hamlook.aetheria.core.features.eventnotification;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class EventTypeConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show popup countdowns for this event")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Notify 5 Minutes Before", desc = "Show a popup 5 minutes before this event starts")
    @ConfigEditorBoolean
    public boolean notify5Min = true;

    @Expose
    @ConfigOption(name = "Notify 1 Minute Before", desc = "Show a popup 1 minute before this event starts")
    @ConfigEditorBoolean
    public boolean notify1Min = true;
}

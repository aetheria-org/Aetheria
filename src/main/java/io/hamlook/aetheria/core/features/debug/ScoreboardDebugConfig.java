package io.hamlook.aetheria.core.features.debug;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorKeybind;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class ScoreboardDebugConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable scoreboard debug mode (allows the debug key to print scoreboard JSON to chat)")
    @ConfigEditorBoolean
    public boolean scoreboardDebug = false;

    @Expose
    @ConfigOption(name = "Debug Key", desc = "Print scoreboard JSON to chat (only works when Scoreboard Debug is enabled)")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int scoreboardDebugKey = Keyboard.KEY_NONE;
}

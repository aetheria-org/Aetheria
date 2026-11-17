package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorText;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class PlayerJoinLeaveConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Notify when a watched player joins or leaves your lobby")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Players List",
        desc = "Comma-separated player names to watch (case-sensitive)"
    )
    @ConfigEditorText
    public String playersList = "";

    @Expose
    @ConfigOption(
        name = "Join Message",
        desc = "Message shown when a watched player joins. %s = player name, && = §"
    )
    @ConfigEditorText
    public String joinMessage = "&&b%s &&ajoined your lobby.";

    @Expose
    @ConfigOption(
        name = "Leave Message",
        desc = "Message shown when a watched player leaves. %s = player name, && = §"
    )
    @ConfigEditorText
    public String leaveMessage = "&&b%s &&cleft your lobby.";
}

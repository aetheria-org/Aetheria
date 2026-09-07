package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class PlayerButtonsConfig {

    public static final String DEFAULT_BACKGROUND_COLOR = "0:240:30:31:34";
    public static final String DEFAULT_ACCENT_COLOR = "0:255:88:101:242";

    @Expose
    @ConfigOption(name = "Enable", desc = "Click a player's name in chat to open a menu with Whisper, View Profile, Auction House, Party Invite, Add Friend, Ignore, Visit Island and Visit Garden shortcuts")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Use In-Game View Profile", desc = "View Profile sends the server's /viewprofile command instead of opening Aetheria's own profile viewer")
    @ConfigEditorBoolean
    public boolean useIngameViewProfile = false;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the popup menu (alpha controls opacity)")
    @ConfigEditorColour
    public String backgroundColor = DEFAULT_BACKGROUND_COLOR;

    @Expose
    @ConfigOption(name = "Accent Color", desc = "Header and row-hover color of the popup menu")
    @ConfigEditorColour
    public String accentColor = DEFAULT_ACCENT_COLOR;

    @Expose
    @ConfigOption(name = "Reset Colors to Default", desc = "Restore the background and accent colors to their defaults")
    @ConfigEditorButton(runnableId = "resetPlayerButtonsColors", buttonText = "Reset")
    public boolean resetColorsDummy = false;
}

package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.chat.ChatUtils;

public class PlayerButtonsConfig {

    public static final String DEFAULT_BACKGROUND_COLOR = "0:240:30:31:34";
    public static final String DEFAULT_ACCENT_COLOR = "0:255:88:101:242";

    @Expose
    @ConfigOption(name = "Enable", desc = "Click a player's name in chat to open a menu with Whisper, View Profile, Auction House, Party Invite, Add Friend, Ignore, Visit Island and Visit Garden shortcuts")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Only In SkyBlock", desc = "Only make player names clickable while on SkyBlock")
    @ConfigEditorBoolean
    public boolean onlyInSkyblock = false;

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

    @Expose
    @Category(name = "Clickable Messages", desc = "Which chat message types get a clickable player name")
    public ClickableMessages clickableMessages = new ClickableMessages();

    public static class ClickableMessages {

        @Expose
        @ConfigOption(name = "Public Chat", desc = "Player names in public chat are clickable")
        @ConfigEditorBoolean
        public boolean publicChat = true;

        @Expose
        @ConfigOption(name = "Party Chat", desc = "Player names in party chat are clickable")
        @ConfigEditorBoolean
        public boolean party = true;

        @Expose
        @ConfigOption(name = "Guild Chat", desc = "Player names in guild chat are clickable")
        @ConfigEditorBoolean
        public boolean guild = true;

        @Expose
        @ConfigOption(name = "Whisper Received", desc = "The sender's name in a whisper you receive is clickable")
        @ConfigEditorBoolean
        public boolean whisperReceived = true;

        @Expose
        @ConfigOption(name = "Whisper Sent", desc = "The recipient's name in a whisper you send is clickable")
        @ConfigEditorBoolean
        public boolean whisperSent = true;

        @Expose
        @ConfigOption(name = "Donate Chat", desc = "Player names in donate chat are clickable")
        @ConfigEditorBoolean
        public boolean donate = true;
    }

    public boolean allows(ChatUtils.MessageType type) {
        if (type == null) return false;
        switch (type) {
            case PLAYER: return clickableMessages.publicChat;
            case PARTY: return clickableMessages.party;
            case GUILD: return clickableMessages.guild;
            case MSG_RECEIVED: return clickableMessages.whisperReceived;
            case MSG_SENT: return clickableMessages.whisperSent;
            case DONATE: return clickableMessages.donate;
            default: return false;
        }
    }
}

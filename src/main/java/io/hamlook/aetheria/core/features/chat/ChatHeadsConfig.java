package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ChatHeadsConfig {

    @Expose
    @ConfigOption(name = "Chat Heads", desc = "Show a small player head next to chat messages sent by players")
    @ConfigEditorBoolean
    public boolean chatHeads = true;

    @Expose
    @ConfigOption(name = "Offset Non-Player Messages", desc = "Indent non-player messages to align with player messages when Chat Heads is enabled")
    @ConfigEditorBoolean
    public boolean offsetNonPlayerMessages = true;

    @Expose
    @ConfigOption(name = "Hide Head on Consecutive", desc = "Don't repeat the head icon when the same player sends consecutive messages")
    @ConfigEditorBoolean
    public boolean hideHeadOnConsecutive = false;
}

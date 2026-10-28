package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class ChatConfig {

    @Expose
    @Category(name = "Chat Filters",desc = "Edit various parts of the Chat Filters Feature")
    public final ChatFilterConfig chatFilterConfig = new  ChatFilterConfig();

    @Expose
    @Category(name = "Emojis", desc = "Render :emoji_name: as textures in chat, with suggestions while typing")
    public final EmojiConfig emojiConfig = new EmojiConfig();

    @Expose
    @Category(name = "Chat Compacting", desc = "Collapse repeated identical chat messages into one with a counter")
    public final ChatCompactingConfig compacting = new ChatCompactingConfig();

    @Expose
    @Category(name = "Timestamps", desc = "Prepend a timestamp to every incoming chat message")
    public final TimestampConfig timestamps = new TimestampConfig();

    @Expose
    @Category(name = "Chat Heads", desc = "Show a small player head next to chat messages sent by players")
    public final ChatHeadsConfig heads = new ChatHeadsConfig();

    @Expose
    @Category(name = "Chat Appearance", desc = "Visual tweaks for chat")
    public final ChatAppearanceConfig appearance = new ChatAppearanceConfig();

    @Expose
    @Category(name = "Chat Copy", desc = "Click chat lines to copy them")
    public final ChatCopyConfig copy = new ChatCopyConfig();

    @Expose
    @Category(name = "Chat Ping", desc = "Play sounds and highlight messages when your name is mentioned in chat")
    public final ChatPingConfig chatPingConfig = new ChatPingConfig();

    @Expose
    @Category(name = "Player Buttons", desc = "Clickable player-name menu in chat: whisper, profile, AH, party invite, friend, ignore and visit shortcuts")
    public final PlayerButtonsConfig playerButtons = new PlayerButtonsConfig();
}
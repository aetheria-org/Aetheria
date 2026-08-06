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
    @ConfigOption(name = "Chat Compacting", desc = "Collapse repeated identical chat messages into one with a counter")
    @ConfigEditorBoolean
    public boolean compactingEnabled = true;

    @Expose
    @ConfigOption(name = "Expire Time (seconds)", desc = "How long until a compacted message resets. -1 = never expire")
    @ConfigEditorSliderAnnotation(minValue = -1, maxValue = 600, minStep = 1)
    public int expireTimeSeconds = 60;

    @Expose
    @ConfigOption(name = "Consecutive Only", desc = "Only compact messages that appear back-to-back with no other messages in between")
    @ConfigEditorBoolean
    public boolean consecutiveOnly = false;

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
    @ConfigEditorDropdown(values = {"[HH:mm]", "<HH:mm>"}, initialIndex = 1)
    public int timestampStyle = 0;

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

    @Expose
    @ConfigOption(name = "Transparent Chat", desc = "Make the chat background fully transparent")
    @ConfigEditorBoolean
    public boolean transparentChat = false;

    @Expose
    @ConfigOption(name = "Animated Chat", desc = "New messages slide into view with a short animation")
    @ConfigEditorBoolean
    public boolean animatedChat = false;

    @Expose
    @ConfigOption(name = "Chat Copy", desc = "Click on a chat line while chat is open to copy it. CTRL+Click copies the line, SHIFT+Click copies the full original message")
    @ConfigEditorBoolean
    public boolean chatCopyEnabled = false;

    @Expose
    @ConfigOption(name = "Copy with Formatting", desc = "Includes §ccolour codes when copying chat lines")
    @ConfigEditorBoolean
    public boolean chatCopyFormatted = false;

    @Expose
    @Category(name = "Chat Ping", desc = "Play sounds and highlight messages when your name is mentioned in chat")
    public final ChatPingConfig chatPingConfig = new ChatPingConfig();
}
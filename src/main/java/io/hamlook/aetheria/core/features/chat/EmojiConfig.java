package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import io.hamlook.aetheria.features.chat.emoji.EmojiLinks;

public class EmojiConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Render :emoji_name: tokens in chat as emoji textures instead of plain text. Other players without the mod just see the plain text.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Suggestions", desc = "Show a popup with matching emoji while typing :name in the chat box (Tab to complete)")
    @ConfigEditorBoolean
    public boolean suggestionsEnabled = true;

    @Expose
    @ConfigOption(name = "Emoji Themes", desc = "Choose which emoji theme the emojis use.")
    @ConfigAnnotations.ConfigEditorDropdown(values = {EmojiLinks.DISCORD_SHEET,EmojiLinks.GOOGLE_SHEET,EmojiLinks.IOS_SHEET})
    public int emojiTheme = 0;


    @Expose
    @ConfigOption(name = "Suggestion Bar BG", desc = "Choose which color the suggestion bar's background uses")
    @ConfigAnnotations.ConfigEditorColour
    public String suggestionBarBG = "0:120:8:8:8";

    @Expose
    @ConfigOption(name = "Bar Border", desc = "Add a border to the suggestion list")
    @ConfigEditorBoolean
    public boolean suggestionsBar = true;

    @Expose
    @ConfigOption(name = "Suggestion Bar Border", desc = "Choose which color the suggestion bar's border uses")
    @ConfigAnnotations.ConfigEditorColour
    public String suggestionBarBorder = "255:255:175:66:123";

    @Expose
    @ConfigOption(name = "Row Highlight Color", desc = "Choose which color highlights the hovered row in the suggestion bar")
    @ConfigAnnotations.ConfigEditorColour
    public String rowHighlightColor = "239:64:255:215:0";

    @Expose
    @ConfigOption(name = "Suggestion Bar Scale", desc = "Scale of the suggestion popup")
    @ConfigAnnotations.ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2.0f, minStep = 0.1f)
    public float suggestionBarScale = 1f;
}

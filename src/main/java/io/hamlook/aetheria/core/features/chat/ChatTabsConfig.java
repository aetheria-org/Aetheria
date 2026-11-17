package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatTabsConfig {

    @Expose
    @ConfigOption(name = "Enable Chat Tabs", desc = "Enable chat tabs (ALL, PARTY, PLAYER, MSG, GUILD, COOP) in chat")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "PARTY tab", desc = "Show the PARTY tab. Its messages appear only in ALL when disabled")
    @ConfigEditorBoolean
    public boolean partyEnabled = true;

    @Expose
    @ConfigOption(name = "PLAYER tab", desc = "Show the PLAYER tab. Its messages appear only in ALL when disabled")
    @ConfigEditorBoolean
    public boolean playerEnabled = true;

    @Expose
    @ConfigOption(name = "MSG tab", desc = "Show the MSG tab (whispers sent and received). Its messages appear only in ALL when disabled")
    @ConfigEditorBoolean
    public boolean msgEnabled = true;

    @Expose
    @ConfigOption(name = "GUILD tab", desc = "Show the GUILD tab. Its messages appear only in ALL when disabled")
    @ConfigEditorBoolean
    public boolean guildEnabled = false;

    @Expose
    @ConfigOption(name = "COOP tab", desc = "Show the COOP tab. Its messages appear only in ALL when disabled")
    @ConfigEditorBoolean
    public boolean coopEnabled = true;

    @Expose
    @ConfigOption(name = "Bar Background", desc = "Background colour of idle and active tab boxes")
    @ConfigEditorColour
    public String barBgColor = "0:128:0:0:0";

    @Expose
    @ConfigOption(name = "Bar Hover Background", desc = "Background colour of a hovered tab box")
    @ConfigEditorColour
    public String barHoverBgColor = "0:80:255:255:255";

    @Expose
    @ConfigOption(name = "Bar Active Colour", desc = "Colour of the 1px underline shown on the active tab")
    @ConfigEditorColour
    public String barActiveColor = "0:255:85:255:255";

    @Expose
    @ConfigOption(name = "Bar Text Shadow", desc = "Draw tab labels with a drop shadow")
    @ConfigEditorBoolean
    public boolean barTextShadow = true;

    @Expose
    @ConfigOption(name = "Bar Alignment", desc = "Horizontal position of the bar above the chat input")
    @ConfigEditorDropdown(values = {"Left", "Center", "Right"})
    public int barAlignment = 0;

    @Expose
    public List<String> tabOrder = new ArrayList<>(
            Arrays.asList("ALL", "PARTY", "PLAYER", "MSG", "GUILD", "COOP"));

    @Expose
    @Category(name = "Hover Preview", desc = "Preview a tab's messages while hovering it. Unlocks instant switching once it triggers")
    public HoverPreview hoverPreview = new HoverPreview();

    public static class HoverPreview {

        @Expose
        @ConfigOption(name = "Hover Preview", desc = "Preview a tab's messages while hovering it. Unlocks instant switching between tabs once triggered")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Preview Delay", desc = "Milliseconds of hover before the preview triggers. Unlock resets when the mouse leaves the bar")
        @ConfigEditorSliderAnnotation(minValue = 50f, maxValue = 2000f, minStep = 50f)
        public int delayMs = 500;

        @Expose
        @ConfigOption(name = "Preview Text Colour", desc = "Colour of the previewed tab's label. Speed 0 keeps the picked colour")
        @ConfigEditorColour
        public String textColor = "255:255:255:70:180";

        @Expose
        @ConfigOption(name = "Chroma Mode", desc = "All Same: one colour. Fade: colour shifts across the text")
        @ConfigEditorDropdown(values = {"All Same", "Fade"})
        public int chromaMode = 1;

        @Expose
        @ConfigOption(name = "Chroma Size", desc = "Width of the Fade gradient in pixels. Smaller = a tighter rainbow across the label")
        @ConfigEditorSliderAnnotation(minValue = 20f, maxValue = 400f, minStep = 5f)
        public float chromaSize = 110f;
    }

    @Expose
    @Category(name = "Unread", desc = "Show an unread counter on each tab")
    public UnreadBadge unreadBadge = new UnreadBadge();

    public static class UnreadBadge {

        @Expose
        @ConfigOption(name = "Unread Badges", desc = "Show an unread counter on tabs with messages you have not seen")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Badge Style", desc = "Count: a number (99+ cap). Dot: a small coloured square")
        @ConfigEditorDropdown(values = {"Count", "Dot"})
        public int badgeStyle = 0;

        @Expose
        @ConfigOption(name = "Badge Colour", desc = "Background colour of the badge")
        @ConfigEditorColour
        public String badgeColor = "0:255:208:48:48";

        @Expose
        @ConfigOption(name = "Hide Empty Tabs", desc = "Hide tabs that have not seen any messages yet this session. ALL always shows.")
        @ConfigEditorBoolean
        public boolean hideEmptyTabs = false;
    }
}

package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ChatPingConfig {

    @Expose
    @ConfigOption(name = "Chat Ping", desc = "Play a sound and highlight the message when your name is mentioned in chat")
    @ConfigEditorBoolean
    public boolean chatPing = true;

    @Expose
    @ConfigOption(name = "Ping All Messages", desc = "Also ping when your name is mentioned in party, donate, guild and coop chat")
    @ConfigEditorBoolean
    public boolean chatPingAllMessages = true;

    @Expose
    @ConfigOption(name = "Ping Sound", desc = "Sound to play when your name is mentioned")
    @ConfigEditorDropdown(values = {"note.pling", "random.orb", "random.levelup", "mob.endermen.portal", "random.pop"}, initialIndex = 0)
    public int chatPingSound = 0;

    @Expose
    @ConfigOption(name = "Ping Volume", desc = "Volume of the ping sound")
    @ConfigEditorSliderAnnotation(minValue = 0.1f, maxValue = 2.0f, minStep = 0.1f)
    public float chatPingVolume = 1.0f;

    @Expose
    @ConfigOption(name = "Ping Pitch", desc = "Pitch of the ping sound")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2.0f, minStep = 0.1f)
    public float chatPingPitch = 1.0f;

    @Expose
    @ConfigOption(name = "Ping Highlight Color", desc = "Color to highlight the message when your name is pinged. None = no highlight.")
    @ConfigEditorDropdown(values = {"None", "Yellow", "Red", "Aqua", "Green", "Light Purple", "Gold", "White"}, initialIndex = 1)
    public int chatPingColor = 1;
}

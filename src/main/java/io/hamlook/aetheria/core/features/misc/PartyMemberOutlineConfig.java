package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class PartyMemberOutlineConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Draw a colored outline around party members while they're actually visible on screen. Blocks/walls between you and them hide it like normal - it never shows through terrain.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Outline Color", desc = "Color of the party member outline")
    @ConfigEditorColour
    public String outlineColor = "255:220:80:220:0";

    @Expose
    @ConfigOption(name = "Disable in Dungeons", desc = "Turn the outline off while inside a dungeon (class colors and nametags already make party members easy to spot there)")
    @ConfigEditorBoolean
    public boolean disableInDungeons = true;
}

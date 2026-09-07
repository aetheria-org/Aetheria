package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorDropdown;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class BossHighlightConfig {

    @Expose
    @ConfigOption(name = "Bonzo Highlight", desc = "Highlight Bonzo (Floor 1 boss). Box = bounding box, Outline = body glow, Off = disabled")
    @ConfigEditorDropdown(values = {"Box", "Outline", "Off"})
    public int bonzoHighlight = 2;

    @Expose
    @ConfigOption(name = "Bonzo Color", desc = "Color used for Bonzo highlight")
    @ConfigEditorColour
    public String bonzoColor = "200:255:140:0:255";

    @Expose
    @ConfigOption(name = "Scarf Highlight", desc = "Highlight Scarf (Floor 2 boss). Box = bounding box, Outline = body glow, Off = disabled")
    @ConfigEditorDropdown(values = {"Box", "Outline", "Off"})
    public int scarfHighlight = 2;

    @Expose
    @ConfigOption(name = "Scarf Color", desc = "Color used for Scarf highlight")
    @ConfigEditorColour
    public String scarfColor = "200:180:0:255:255";

    @Expose
    @ConfigOption(name = "Scarf's Minions Highlight", desc = "Highlight Scarf's undead minions. Box = bounding box, Outline = body glow, Off = disabled")
    @ConfigEditorDropdown(values = {"Box", "Outline", "Off"})
    public int scarfMinionHighlight = 2;

    @Expose
    @ConfigOption(name = "Scarf's Minions Color", desc = "Color used for Scarf's minions highlight")
    @ConfigEditorColour
    public String scarfMinionColor = "150:180:0:200:255";

    @Expose
    @ConfigOption(name = "Professor Highlight", desc = "Highlight The Professor and his guardians (Floor 3 boss). Box = bounding box, Outline = body glow, Off = disabled")
    @ConfigEditorDropdown(values = {"Box", "Outline", "Off"})
    public int professorHighlight = 2;

    @Expose
    @ConfigOption(name = "Professor Color", desc = "Color used for The Professor and his guardians highlight")
    @ConfigEditorColour
    public String professorColor = "200:0:200:255:255";
}

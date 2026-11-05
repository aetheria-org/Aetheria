package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class SecretHighlightsConfig {

    @Expose
    @Category(name = "Lever Highlight", desc = "Solid highlight for levers that have not been clicked")
    public Lever lever = new Lever();

    @Expose
    @Category(name = "Essence Highlight", desc = "Solid highlight for wither essence skulls")
    public Essence essence = new Essence();

    @Expose
    @Category(name = "Bat Highlight", desc = "Highlight for bats while the room still has a bat secret")
    public Bats bats = new Bats();

    @Expose
    @Category(name = "Item Highlight", desc = "Highlight for dropped secret items inside pickup range of an uncollected item secret")
    public Items items = new Items();

    public static SecretHighlightsConfig get() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.dungeons.secrets.highlights;
    }

    public static class Lever {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Draw unclicked levers as one solid colour")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color for solid levers")
        @ConfigEditorColour
        public String color = "0:255:255:255:90";

        @Expose
        @ConfigOption(name = "Through Walls", desc = "Draw solid levers even when behind walls")
        @ConfigEditorBoolean
        public boolean throughWalls = true;
    }

    public static class Essence {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Draw essence skulls as a solid block")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color for solid essence")
        @ConfigEditorColour
        public String color = "0:255:205:90:255";

        @Expose
        @ConfigOption(name = "Through Walls", desc = "Draw solid essence even when behind walls")
        @ConfigEditorBoolean
        public boolean throughWalls = true;
    }

    public static class Bats {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Draw bats as a highlight")
        @ConfigEditorBoolean
        public boolean enabled = true;

        //#if MC < 1.21
        @Expose
        @ConfigOption(name = "Type", desc = "Box = ESP box; Solid = filled model shell")
        @ConfigEditorDropdown(values = {"Box", "Solid"}, initialIndex = 1)
        public int type = 1;

        @Expose
        @ConfigOption(name = "Through Walls", desc = "Draw bats even when behind walls")
        @ConfigEditorBoolean
        public boolean throughWalls = true;
        //#else
        //$$ @Expose
        //$$ @ConfigOption(name = "Type", desc = "Box = see-through ESP box; Outline = glow outline")
        //$$ @ConfigEditorDropdown(values = {"Box", "Outline"}, initialIndex = 0)
        //$$ public int type = 0;
        //#endif

        @Expose
        @ConfigOption(name = "Color", desc = "Color for bat highlight")
        @ConfigEditorColour
        public String color = "0:255:120:120:255";
    }

    public static class Items {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Draw dropped secret items as a highlight")
        @ConfigEditorBoolean
        public boolean enabled = true;

        //#if MC < 1.21
        @Expose
        @ConfigOption(name = "Type", desc = "Box = ESP box; Solid = filled model shell")
        @ConfigEditorDropdown(values = {"Box", "Solid"}, initialIndex = 1)
        public int type = 1;

        @Expose
        @ConfigOption(name = "Through Walls", desc = "Draw items even when behind walls")
        @ConfigEditorBoolean
        public boolean throughWalls = true;
        //#else
        //$$ @Expose
        //$$ @ConfigOption(name = "Type", desc = "Box = see-through ESP box; Outline = glow outline")
        //$$ @ConfigEditorDropdown(values = {"Box", "Outline"}, initialIndex = 0)
        //$$ public int type = 0;
        //#endif

        @Expose
        @ConfigOption(name = "Color", desc = "Color for item highlight")
        @ConfigEditorColour
        public String color = "0:255:255:80:80";
    }
}

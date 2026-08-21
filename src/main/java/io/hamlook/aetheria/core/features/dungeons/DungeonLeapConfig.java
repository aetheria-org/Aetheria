package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;

public class DungeonLeapConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Dungeon Leap Overlay", desc = "Create an Overlay for Spirit Leap/InfiniLeap Menu, with a Dungeon Map Display")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean dungeonLeapOverlay = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Use Arrow instead of Head",desc = "Use the arrow icons instead of the player icons for map preview")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean useArrowIcons = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Player Button List",desc = "Render a player list so you can click them instead of clicking the players in the map.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean playerBList = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Clickable Player Icons",desc = "Be able to click the player heads/arrows in the map preview to leap to them.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean clickablePlayers = true;

}

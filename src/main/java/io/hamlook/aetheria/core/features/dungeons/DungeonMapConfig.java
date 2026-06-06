package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class DungeonMapConfig {

    @Expose
    @ConfigOption(name = "Enable Map",desc = "Enable rendering of dungeon map")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    public Position dungeonMapPos = new Position(5, 400);

    @Expose
    @ConfigOption(name = "Edit Position",desc = "Edit the position of the dungeon map hud")
    @ConfigEditorButton(runnableId = "editDungeonMapPos",buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the overlay (alpha controls opacity; 0 = fully transparent)")
    @ConfigEditorColour
    public String bgColor = "0:0:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int cornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the Dungeon Breaker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float scale = 1f;

    @Expose
    @ConfigOption(name = "Name Offset from Head",desc = "Control how below the name is from the player head")
    @ConfigEditorSliderAnnotation(minValue = 1f,maxValue = 20f,minStep = 1f)
    public float nameOffset = 6f;

    @Expose
    @ConfigOption(name = "Name Font Size",desc = "Control how big the name display is")
    @ConfigEditorSliderAnnotation(minValue = 0.25f,maxValue = 2f,minStep = 0.05f)
    public float nameSize = 1f;

    @Expose
    @ConfigOption(name = "Head Scale",desc = "Control how big the head display is")
    @ConfigEditorSliderAnnotation(minValue = 0.25f,maxValue = 2f,minStep = 0.05f)
    public float headScale = 1f;

    @Expose
    @ConfigOption(name = "Show Player Head",desc = "Show Player Heads in the Dungeon Map")
    @ConfigEditorBoolean
    public boolean showPlayerHead = true;

    @Expose
    @ConfigOption(name = "Show Player Username",desc = "Show Player's Username in Dungeon Map")
    @ConfigEditorBoolean
    public boolean showPlayerUsername = true;

    @Expose
    @ConfigOption(name = "Show Player Rank",desc = "Show Player's rank in username display in Dungeon Map")
    @ConfigEditorBoolean
    public boolean showPlayerRank = true;



}

package io.hamlook.aetheria.core.features.debug;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class Debug {

    @Expose
    @Category(name = "Scoreboard Debug", desc = "Debug tools for the scoreboard")
    public ScoreboardDebugConfig scoreboardDebugConfig = new ScoreboardDebugConfig();

    @Expose
    @ConfigOption(name = "Room Overlay: Show Hash", desc = "Show room hash in the dungeon room overlay when the room is not detected")
    @ConfigEditorBoolean
    public boolean dungeonRoomDebug = false;

    @Expose
    @ConfigOption(name = "Dungeon Room Highlight", desc = "Draw debug ESP boxes for the detected dungeon room: room bounds and the origin corner block")
    @ConfigEditorBoolean
    public boolean dungeonRoomHighlight = false;

    @Expose
    @ConfigOption(name = "Enable debug features", desc = "DO NOT TURN ON UNLESS YOU KNOW WHAT YOUR DOING!")
    @ConfigEditorBoolean
    public boolean enableDebug = false;

    @Expose
    @ConfigOption(name = "Reload Repo", desc = "Re-fetch all data from the remote repo")
    @ConfigEditorButton(runnableId = "reloadRepo", buttonText = "Reload")
    public boolean reloadRepoButton = false;

    @Expose
    @ConfigOption(name = "Copy NBT Data", desc = "Copy the NBT data of hovered item using RCTRL")
    @ConfigEditorBoolean
    public boolean copyNBTData = false;

    @Expose
    @ConfigOption(name = "Copy NBT Key", desc = "The Key to copy NBT data using Copy NBT Data feature")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RCONTROL)
    public int copyNBTKey = Keyboard.KEY_RCONTROL;
}
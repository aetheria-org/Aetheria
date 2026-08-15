package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class DungeonRoomOverlayConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show current room name overlay while in dungeons")
    @ConfigEditorBoolean
    public boolean dungeonRoomOverlay = false;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the overlay")
    @ConfigEditorColour
    public String dungeonRoomOverlayBgColor = "0:0:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int dungeonRoomOverlayCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the room name overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float dungeonRoomOverlayScale = 1f;

    @Expose
    @ConfigOption(name = "Show Alias", desc = "Also show the room's short alias in the overlay")
    @ConfigEditorBoolean
    public boolean dungeonRoomOverlayShowAlias = false;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag the overlay to reposition it")
    @ConfigEditorButton(runnableId = "openDungeonRoomOverlayEditor", buttonText = "Edit")
    public boolean editDungeonRoomOverlayPosDummy = false;

    @Expose
    public Position dungeonRoomOverlayPos = new Position(4, 140);
}

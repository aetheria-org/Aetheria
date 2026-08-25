package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class DungeonMapConfig {

    @Expose
    @ConfigOption(name = "Enable Map", desc = "Enable rendering of dungeon map")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    public Position dungeonMapPos = new Position(87, 261);

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Edit the position of the dungeon map hud")
    @ConfigEditorButton(runnableId = "editDungeonMapPos", buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    @Category(name = "Appearance", desc = "Visual appearance of the dungeon map")
    public Appearance appearance = new Appearance();

    @Expose
    @Category(name = "Players", desc = "Player head and name display on the map")
    public Players players = new Players();

    @Expose
    @Category(name = "Rooms", desc = "Room name and checkmark display")
    public Rooms rooms = new Rooms();

    public static class Appearance {

        @Expose
        @ConfigOption(name = "Background Color", desc = "Background color of the overlay (alpha controls opacity; 0 = fully transparent)")
        @ConfigEditorColour
        public String bgColor = "0:102:24:24:24";

        @Expose
        @ConfigOption(name = "Background Flow Chroma", desc = "Animate the background with flowing rainbow chroma")
        @ConfigEditorBoolean
        public boolean bgFlowChroma = false;

        @Expose
        @ConfigOption(name = "Corner Radius", desc = "Roundness of overlay corners")
        @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
        public int cornerRadius = 4;

        @Expose
        @ConfigOption(name = "Scale", desc = "Size of the Dungeon Map overlay")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
        public float scale = 1f;

        @Expose
        @ConfigOption(name = "Cell Block Size", desc = "Block size of one dungeon cell including border (default 32)")
        @ConfigEditorSliderAnnotation(minValue = 20f, maxValue = 50f, minStep = 1f)
        public int cellSizeBlocks = 32;

        @Expose
        @Category(name = "Border", desc = "Border drawn around the map")
        public Border border = new Border();

        public static class Border {

            @Expose
            @ConfigOption(name = "Border Enabled", desc = "Draw a border around the map")
            @ConfigEditorBoolean
            public boolean borderEnabled = true;

            @Expose
            @ConfigOption(name = "Border Color", desc = "Border color (alpha controls opacity; 0 = fully transparent)")
            @ConfigEditorColour
            public String borderColor = "245:255:255:78:115";

            @Expose
            @ConfigOption(name = "Border Thickness", desc = "Thickness of the map border in pixels")
            @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 4f, minStep = 1f)
            public int borderThickness = 2;

            @Expose
            @ConfigOption(name = "Border Flow Chroma", desc = "Animate the border with flowing rainbow chroma")
            @ConfigEditorBoolean
            public boolean borderFlowChroma = true;

            @Expose
            @ConfigOption(name = "Flow Chroma Size", desc = "Width of the flow chroma gradient in pixels")
            @ConfigEditorSliderAnnotation(minValue = 20f, maxValue = 400f, minStep = 5f)
            public float flowChromaSize = 120f;
        }
    }

    public static class Players {

        @Expose
        @ConfigOption(name = "Show Player Markers", desc = "Show player markers on the dungeon map")
        @ConfigEditorBoolean
        public boolean showPlayerHead = true;

        @Expose
        @Category(name = "You", desc = "Your own marker style and size")
        public Self self = new Self();

        @Expose
        @Category(name = "Teammates", desc = "Other players' marker style and size")
        public Teammates teammates = new Teammates();

        @Expose
        @ConfigOption(name = "Show Player Username", desc = "Show Player's Username in Dungeon Map")
        @ConfigEditorBoolean
        public boolean showPlayerUsername = true;

        @Expose
        @ConfigOption(name = "Show Player Rank", desc = "Show Player's rank in username display in Dungeon Map")
        @ConfigEditorBoolean
        public boolean showPlayerRank = false;

        @Expose
        @ConfigOption(name = "Use Entity Position for Self", desc = "Use your real entity position for your own marker so it updates every frame (smoother) instead of waiting for map decoration updates")
        @ConfigEditorBoolean
        public boolean accurateSelfPosition = true;

        @Expose
        @ConfigOption(name = "Name Font Size", desc = "Control how big the name display is")
        @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2f, minStep = 0.05f)
        public float nameSize = 1f;

        @Expose
        @ConfigOption(name = "Name Offset from Marker", desc = "Control how below the name is from the player marker")
        @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 20f, minStep = 1f)
        public float nameOffset = 6f;
    }

    public static class Self {

        @Expose
        @ConfigOption(name = "Icon Style", desc = "Head shows your skin face. Arrow shows a Hypixel-map-style directional pointer (white). Head + Direction frames the head with your frame color and adds a direction nub")
        @ConfigEditorDropdown(values = {"Head", "Arrow", "Head + Direction"}, initialIndex = 2)
        public int iconStyle = 2;

        @Expose
        @ConfigOption(name = "Marker Scale", desc = "Control how big your own marker is")
        @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2f, minStep = 0.05f)
        public float markerScale = 1.15f;

        @Expose
        @ConfigOption(name = "Frame Color", desc = "Frame color for your marker in Head + Direction mode")
        @ConfigEditorColour
        public String frameColor = "0:255:85:255:85";

        @Expose
        @ConfigOption(name = "Flow Chroma Frame", desc = "Animate the frame with flowing rainbow chroma. Only active when Frame Color has a chroma speed above 0 — at speed 0 the solid picked color is used")
        @ConfigEditorBoolean
        public boolean frameFlowChroma = true;
    }

    public static class Teammates {

        @Expose
        @ConfigOption(name = "Icon Style", desc = "Head shows each player's skin face. Arrow shows Hypixel-map-style directional pointers (blue/yellow/orange/red per teammate). Head + Direction frames the head with the frame color and adds a direction nub")
        @ConfigEditorDropdown(values = {"Head", "Arrow", "Head + Direction"})
        public int iconStyle = 0;

        @Expose
        @ConfigOption(name = "Marker Scale", desc = "Control how big other players' markers are")
        @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2f, minStep = 0.05f)
        public float markerScale = 1f;

        @Expose
        @ConfigOption(name = "Frame Color", desc = "Frame color for teammates in Head + Direction mode")
        @ConfigEditorColour
        public String frameColor = "0:255:255:255:85";

        @Expose
        @ConfigOption(name = "Flow Chroma Frame", desc = "Animate the frame with flowing rainbow chroma. Only active when Frame Color has a chroma speed above 0 — at speed 0 the solid picked color is used")
        @ConfigEditorBoolean
        public boolean frameFlowChroma = true;
    }

    public static class Rooms {

        @Expose
        @ConfigOption(name = "Show Visited Room Names", desc = "Display the name of each room you have visited on the dungeon map")
        @ConfigEditorBoolean
        public boolean showVisitedRoomNames = true;

        @Expose
        @ConfigOption(name = "Room Name Font Size", desc = "Control how big the room name display is")
        @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2f, minStep = 0.05f)
        public float roomnameSize = 1f;

        @Expose
        @ConfigOption(name = "Room Checkmarks", desc = "Room checkmark icons based on room state (Default / NEU icon sets)")
        @ConfigEditorDropdown(values = {"Default", "NEU"}, initialIndex = 1)
        public int mapCheckmark = 1;

        @Expose
        @ConfigOption(name = "Split Room Markers", desc = "For multi-cell rooms, draw the checkmark and the room name in separate cells so they don't overlap. Off = both centered (legacy). Single-cell rooms always draw the name over the checkmark.")
        @ConfigEditorBoolean
        public boolean splitRoomMarkers = true;

        @Expose
        @ConfigOption(name = "Color Text", desc = "Color room names based on room state")
        @ConfigEditorBoolean
        public boolean mapColorText = true;
    }
}
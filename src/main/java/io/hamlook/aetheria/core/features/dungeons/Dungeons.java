package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class Dungeons {

    @Expose
    @ConfigOption(name = "Blood Mob Highlight", desc = "Highlight blood room mobs. Box = bounding box, Outline = body glow, Off = disabled")
    @ConfigEditorDropdown(values = {"Box", "Outline", "Off"})
    public int bloodMobHighlight = 2;

    @Expose
    @ConfigOption(name = "Blood Mob Color", desc = "Color used for blood mob box and outline highlight")
    @ConfigEditorColour
    public String bloodMobColor = "200:255:50:50:255";

    @Expose
    @Category(name = "Crush Alert", desc = "Alert when Storm is near a pillar in F7/M7 phase 2")
    public CrushAlertConfig crushAlert = new CrushAlertConfig();

    @Expose
    @Category(name = "Boss Highlights", desc = "Highlight dungeon bosses and their minions")
    public BossHighlightConfig bossHighlight = new BossHighlightConfig();

    @Expose
    @Category(name = "Splits Overlay", desc = "Run timers, end-of-run stats and overlay settings")
    public DungeonOverlayConfig dungeonOverlay = new DungeonOverlayConfig();

    @Expose
    @Category(name = "D.Breaker Overlay", desc = "Shows Dungeon Breaker charges while in dungeons")
    public DungeonBreakerConfig dungeonBreaker = new DungeonBreakerConfig();

    @Expose
    @Category(name = "D.Room Overlay", desc = "Shows the name of your current dungeon room on screen")
    public DungeonRoomOverlayConfig dungeonRoomOverlayConfig = new DungeonRoomOverlayConfig();

    @Expose
    @Category(name = "Chest Case Opening", desc = "CS:GO style animation when opening dungeon chests")
    public CaseOpeningConfig caseOpening = new CaseOpeningConfig();

    @Expose
    @Category(name = "Secret Finder", desc = "Secret finder with  secret ESP, and display options")
    public DungeonSecretFinderConfig dungeonSecretFinder = new DungeonSecretFinderConfig();

    @Expose
    @Category(name = "Dungeon Map", desc = "Custom Dungeon Map display overlay")
    public DungeonMapConfig dungeonMapConfig = new DungeonMapConfig();

    @Expose
    @ConfigOption(name = "Hide Blessing Messages", desc = "Hides the 'DUNGEON BUFF! ... found a Blessing of ...' chat spam in dungeons")
    @ConfigEditorBoolean
    public boolean hideBlessingMessages = false;

    @Expose
    public java.util.Map<String, Long> floorPbs = new java.util.HashMap<>();

    public long getPb(String key) { Long v = floorPbs.get(key); return v == null ? 0L : v; }
    public void setPb(String key, long ms) { floorPbs.put(key, ms); }
}
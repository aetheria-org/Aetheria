package io.hamlook.aetheria.core.features.diana;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class Diana {

    @Expose
    @ConfigOption(name = "Diana Tracker", desc = "Enables tracking")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause tracking while chat GUI is open; resumes on next tracked action")
    @ConfigEditorBoolean
    public boolean pauseOnChat = false;

    @Expose
    @ConfigOption(name = "Edit Overlay Positions", desc = "Drag all Diana overlays to reposition them individually")
    @ConfigEditorButton(runnableId = "openDianaOverlayEditor", buttonText = "Edit")
    public boolean editOverlayPosDummy = false;

    @Expose
    @Category(name = "Event Overlay", desc = "Diana Event HUD – playtime, burrows, and mob rates")
    public EventOverlayConfig eventOverlay = new EventOverlayConfig();

    @Expose
    @Category(name = "Loot Overlay", desc = "Diana Loot HUD – chimeras, rare drops, and coins")
    public LootOverlayConfig lootOverlay = new LootOverlayConfig();

    @Expose
    @Category(name = "Inquisitor Overlay", desc = "Live HP bar for the nearest Minos Inquisitor")
    public InquisitorHpConfig inquisitorHp = new InquisitorHpConfig();

    @Expose
    @Category(name = "DianaMob Overlay", desc = "Live HP bar for the nearest non-Inquisitor Diana mob")
    public DianaMobHpConfig dianaMobHp = new DianaMobHpConfig();
}

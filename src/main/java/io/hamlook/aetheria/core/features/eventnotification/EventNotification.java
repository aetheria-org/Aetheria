package io.hamlook.aetheria.core.features.eventnotification;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class EventNotification {

    @Expose
    @ConfigOption(name = "Enable", desc = "Master switch for SkyBlock event popup notifications")
    @ConfigEditorBoolean
    public boolean masterEnabled = true;

    @Expose
    @ConfigOption(name = "Edit Overlay Position", desc = "Drag to reposition the event notification popup")
    @ConfigEditorButton(runnableId = "openEventNotifierEditor", buttonText = "Edit")
    public boolean editEventNotifierPosDummy = false;

    @Expose
    public Position overlayPos = new Position(0, 6, true, false);

    @Expose
    @ConfigOption(name = "Overlay Scale", desc = "Size of the event notification popup")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    public float overlayScale = 1f;

    @Expose
    @ConfigOption(name = "Notification Duration", desc = "How long a notification stays fully visible before it starts fading out (seconds)")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 10f, minStep = 1f)
    public float notificationDuration = 8f;

    @Expose
    @Category(name = "Election", desc = "Election Booth Opens! / Election Over!")
    public EventTypeConfig election = new EventTypeConfig();

    @Expose
    @Category(name = "Oringo", desc = "Traveling Zoo")
    public EventTypeConfig oringo = new EventTypeConfig();

    @Expose
    @Category(name = "Dark Auction", desc = "Dark Auction")
    public EventTypeConfig darkAuction = new EventTypeConfig();

    @Expose
    @Category(name = "Farming Contest", desc = "Farming Contest")
    public EventTypeConfig farmingContest = new EventTypeConfig();

    @Expose
    @Category(name = "Spooky Festival", desc = "Spooky Festival")
    public EventTypeConfig spooky = new EventTypeConfig();

    @Expose
    @Category(name = "Jerry Workshop", desc = "Jerry Workshop Opens")
    public EventTypeConfig jerry = new EventTypeConfig();

    @Expose
    @Category(name = "Mining Fiesta", desc = "Rare mayor-perk event; only appears once scheduled for the SkyBlock year")
    public EventTypeConfig miningFiesta = new EventTypeConfig();

    @Expose
    @Category(name = "Fishing Festival", desc = "Rare mayor-perk event; only appears once scheduled for the SkyBlock year")
    public EventTypeConfig fishingFestival = new EventTypeConfig();

    @Expose
    @Category(name = "New Year", desc = "New Year")
    public EventTypeConfig newYear = new EventTypeConfig();
}

package io.hamlook.aetheria.features.misc.ghosttracker;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ScavengerGainEvent;
import io.hamlook.aetheria.init.RegisterEvents;

@RegisterEvents
public class ScavengerGainListener {

    @HandleEvent
    public void onScavengerGain(ScavengerGainEvent event) {
        GhostStats.getInstance().addScavenger(event.getAmount());
    }
}

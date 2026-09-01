package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;

public class ActionBarUpdateEvent extends AetheriaEvent {

    private final String text;

    public ActionBarUpdateEvent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
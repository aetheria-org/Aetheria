package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import lombok.Getter;

@Getter
public class ActionBarXpGainEvent extends AetheriaEvent {

    private final String formattedText;

    public ActionBarXpGainEvent(String formattedText) {
        this.formattedText = formattedText;
    }

}
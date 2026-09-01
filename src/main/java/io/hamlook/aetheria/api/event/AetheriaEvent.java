package io.hamlook.aetheria.api.event;

import lombok.Getter;

public abstract class AetheriaEvent {

    @Getter
    private boolean cancelled = false;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean post() {
        return AetheriaEventBus.INSTANCE.post(this);
    }

    public interface Cancellable {
    }
}

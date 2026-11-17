package io.hamlook.aetheria.api.event;

import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import lombok.Getter;

public abstract class AetheriaEvent {

    @Getter
    private boolean cancelled = false;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean post() {
        if (this instanceof Rendering) {
            DrawContextUtils.setContext(((Rendering) this).getContext());
            try {
                return AetheriaEventBus.INSTANCE.post(this);
            } finally {
                DrawContextUtils.clearContext();
            }
        }
        return AetheriaEventBus.INSTANCE.post(this);
    }

    public interface Cancellable {
    }

    public interface Rendering {
        DrawContext getContext();
    }
}

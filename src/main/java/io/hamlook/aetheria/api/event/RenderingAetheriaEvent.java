package io.hamlook.aetheria.api.event;

import io.hamlook.aetheria.utils.compat.DrawContext;

public abstract class RenderingAetheriaEvent extends AetheriaEvent implements AetheriaEvent.Rendering {

    protected final DrawContext context;

    protected RenderingAetheriaEvent(DrawContext context) {
        this.context = context;
    }

    @Override
    public DrawContext getContext() {
        return context;
    }
}

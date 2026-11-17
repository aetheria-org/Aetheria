package io.hamlook.aetheria.core.config.migrations;

import io.hamlook.aetheria.core.config.ConfigFixContext;
import io.hamlook.aetheria.core.config.ConfigMigrationHandler;

public final class ItemAnimationsCategory implements ConfigMigrationHandler {

    @Override
    public int version() {
        return 3;
    }

    @Override
    public void apply(ConfigFixContext ctx) {
        ctx.move("misc.noItemSwitchAnimation", "cosmetics.itemAnimations.noItemSwitchAnimation");
    }
}

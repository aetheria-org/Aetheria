package io.hamlook.aetheria.core.config.migrations;

import io.hamlook.aetheria.core.config.ConfigFixContext;
import io.hamlook.aetheria.core.config.ConfigMigrationHandler;

public final class ChatSubcategories implements ConfigMigrationHandler {

    @Override
    public int version() {
        return 2;
    }

    @Override
    public void apply(ConfigFixContext ctx) {
        String b = "chat";
        ctx.move(b + ".compactingEnabled", b + ".compacting.compactingEnabled");
        ctx.move(b + ".expireTimeSeconds", b + ".compacting.expireTimeSeconds");
        ctx.move(b + ".consecutiveOnly", b + ".compacting.consecutiveOnly");
        ctx.move(b + ".timestampsEnabled", b + ".timestamps.timestampsEnabled");
        ctx.move(b + ".timestamp24Hour", b + ".timestamps.timestamp24Hour");
        ctx.move(b + ".timestampShowSeconds", b + ".timestamps.timestampShowSeconds");
        ctx.move(b + ".timestampStyle", b + ".timestamps.timestampStyle");
        ctx.move(b + ".chatHeads", b + ".heads.chatHeads");
        ctx.move(b + ".offsetNonPlayerMessages", b + ".heads.offsetNonPlayerMessages");
        ctx.move(b + ".hideHeadOnConsecutive", b + ".heads.hideHeadOnConsecutive");
        ctx.move(b + ".chatCopyEnabled", b + ".copy.chatCopyEnabled");
        ctx.move(b + ".chatCopyFormatted", b + ".copy.chatCopyFormatted");
        ctx.move(b + ".chatBackgroundOpacity", b + ".appearance.chatBackgroundOpacity");
        ctx.move(b + ".animatedChat", b + ".appearance.animatedChat");
    }
}

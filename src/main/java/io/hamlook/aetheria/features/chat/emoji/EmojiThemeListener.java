package io.hamlook.aetheria.features.chat.emoji;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;

@RegisterEvents
public class EmojiThemeListener {

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        EmojiSheetStore.drainDeferred();
        EmojiSheetStore.selectTheme(EmojiSheetStore.configuredTheme());
    }
}

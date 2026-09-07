package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.events.ASMTickEvent
import io.hamlook.aetheria.events.ASMWorldLoadEvent
import io.hamlook.aetheria.events.TickPhase
import io.hamlook.aetheria.init.RegisterEvents

@RegisterEvents
class ChatUtilsFeature {

    private var ticks = 0

    @HandleEvent
    fun onClientTick(event: ASMTickEvent) {
        if (event.phase != TickPhase.START) return
        if (++ticks >= 12000) {          // ~10 minutes at 20 TPS
            ChatCompactHandler.cleanupExpired()
            ticks = 0
        }
    }

    @HandleEvent
    fun onWorldLoad(event: ASMWorldLoadEvent) {
        ticks = 0
        ChatCompactHandler.reset()
    }
}

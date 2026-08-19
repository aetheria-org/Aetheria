package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.init.RegisterEvents
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

@RegisterEvents
class ChatUtilsFeature {

    private var ticks = 0

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (++ticks >= 12000) {          // ~10 minutes at 20 TPS
            ChatCompactHandler.cleanupExpired()
            ticks = 0
        }
    }

    @SubscribeEvent
    fun onWorldLoad() {
        ticks = 0
        ChatCompactHandler.reset()
    }
}

package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.command.brigadier.CommandCategory
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent
import io.hamlook.aetheria.init.RegisterEvents

@RegisterEvents
class PretendThisDoesntExist {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("ATHRthisisatestdontusethispls") {
            description = "Easter egg test command"
            category = CommandCategory.INTERNAL
            simpleCallback { DVD.forceCornerHit() }
        }
    }
}

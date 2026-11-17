package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.command.brigadier.CommandCategory
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.compat.ClipboardCompat

@RegisterEvents
class CopyToClipboardCommand {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("copytoclipboard") {
            description = "Copy text to clipboard"
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { args ->
                if (args.isEmpty()) return@legacyCallbackArgs
                try {
                    ClipboardCompat.setClipboard(args.joinToString(" "))
                } catch (_: Exception) { }
            }
        }
    }
}

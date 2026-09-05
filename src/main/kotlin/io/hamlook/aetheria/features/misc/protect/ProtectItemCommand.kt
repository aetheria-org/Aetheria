package io.hamlook.aetheria.features.misc.protect

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.command.brigadier.BrigadierArguments
import io.hamlook.aetheria.command.brigadier.CommandCategory
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.item.ItemUtils

@RegisterEvents
class ProtectItemCommand {

    private companion object {
        private const val PREFIX = "§b[ItemProtect] §r"
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("athrprotect") {
            description = "Protect items from being traded or sold"
            aliases = listOf("aetheriaprotect", "asmprotect", "jefprotect")
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                val player = MinecraftCompat.getLocalPlayer() ?: return@simpleCallback
                toggleProtection(player)
            }
            literal("list") {
                description = "List protected items"
                simpleCallback { listProtected() }
            }
            literal("clear") {
                description = "Clear all protected items"
                simpleCallback { clearProtected() }
            }
            argCallback("sub", BrigadierArguments.greedyString(), listOf("list", "clear")) { sub ->
                when (sub.lowercase()) {
                    "list" -> listProtected()
                    "clear" -> clearProtected()
                    else -> msg("§cUsage: /athrprotect [list|clear]")
                }
            }
        }
    }

    private fun listProtected() {
        val uuids = ProtectedItemStorage.protectedUuids

        if (uuids.isEmpty()) {
            msg("§eNo protected items.")
            return
        }

        msg("§aProtected items (${uuids.size}):")

        uuids.forEach {
            ChatUtils.sendMessage(" §7- $it")
        }
    }

    private fun clearProtected() {
        val count = ProtectedItemStorage.protectedUuids.size

        ProtectedItemStorage.protectedUuids.clear()
        ProtectedItemStorage.save()

        msg("§aCleared $count protected item(s).")
    }

    private fun toggleProtection(player: net.minecraft.entity.player.EntityPlayer) {
        val held = player.heldItem

        if (held == null) {
            msg("§cYou are not holding an item!")
            return
        }

        val uuid = ItemUtils.getItemUuid(held)

        if (uuid == null) {
            msg("§cThis item has no SkyBlock UUID and cannot be protected.")
            return
        }

        if (ProtectedItemStorage.contains(uuid)) {
            ProtectedItemStorage.remove(uuid)

            msg("§e${held.displayName} §7is no longer protected.")
        } else {
            ProtectedItemStorage.add(uuid)

            msg("§a${held.displayName} §7is now protected!")
        }
    }

    private fun msg(message: String) {
        ChatUtils.sendMessage(PREFIX + message)
    }
}

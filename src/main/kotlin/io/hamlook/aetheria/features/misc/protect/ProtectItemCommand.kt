package io.hamlook.aetheria.features.misc.protect

import io.hamlook.aetheria.command.SimpleCommand
import io.hamlook.aetheria.init.RegisterCommand
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.item.ItemUtils
import net.minecraft.client.Minecraft
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos

@RegisterCommand
class ProtectItemCommand : SimpleCommand() {

    companion object {
        private val mc = Minecraft.getMinecraft()

        private const val PREFIX = "§b[ItemProtect] §r"

        private val TAB_OPTIONS = listOf(
            "list", "clear"
        )
    }

    override fun getName() = "athrprotect"

    override fun getAliases() = listOf(
        "aetheriaprotect", "asmprotect", "jefprotect", "athrprotect"
    )

    override fun getUsage() = "/athrprotect [list|clear]"

    override fun execute(sender: ICommandSender, args: Array<String>) {
        val player = mc.thePlayer ?: return

        when (args.firstOrNull()?.lowercase()) {
            null -> toggleProtection(player)

            "list" -> listProtected()

            "clear" -> clearProtected()

            else -> showUsage()
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender, args: Array<String>, pos: BlockPos
    ): List<String> {
        return if (args.size == 1) TAB_OPTIONS else emptyList()
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

    private fun toggleProtection(player: EntityPlayer) {
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

    private fun showUsage() {
        msg("§cUsage: ${getUsage()}")
    }

    private fun msg(message: String) {
        ChatUtils.sendMessage(PREFIX + message)
    }
}
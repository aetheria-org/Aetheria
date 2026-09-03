package io.hamlook.aetheria.utils.compat

import net.minecraft.command.ICommand
import net.minecraftforge.client.ClientCommandHandler

object CommandCompat {
    @JvmStatic
    fun registerCommand(command: ICommand) {
        ClientCommandHandler.instance.registerCommand(command)
    }

    @JvmStatic
    fun executeCommand(command: String) {
        val player = MinecraftCompat.getMinecraft().thePlayer ?: return
        ClientCommandHandler.instance.executeCommand(player, command)
    }
}

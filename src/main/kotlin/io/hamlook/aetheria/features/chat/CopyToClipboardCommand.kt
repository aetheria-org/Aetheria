package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.command.ASMCommand
import io.hamlook.aetheria.init.RegisterCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.command.CommandException
import net.minecraft.command.ICommandSender

@RegisterCommand
class CopyToClipboardCommand : ASMCommand() {

    override fun getName() = "copytoclipboard"

    override fun getUsage() = "/copytoclipboard <text>"

    @Throws(CommandException::class)
    override fun execute(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) return
        try {
            GuiScreen.setClipboardString(args.joinToString(" "))
        } catch (_: Exception) { }
    }
}

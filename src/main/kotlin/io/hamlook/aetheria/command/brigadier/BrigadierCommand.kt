package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import io.hamlook.aetheria.utils.chat.ChatUtils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

class BrigadierCommand(
    root: BaseBrigadierBuilder,
    private val dispatcher: CommandDispatcher<Any?>,
) : CommandBase() {
    private val aliases: List<String> = root.aliases
    private val node: CommandNode<Any?>

    init {
        val builder = root.builder
        node = builder.build()
        root.node = node
        dispatcher.register(builder as com.mojang.brigadier.builder.LiteralArgumentBuilder<Any?>)
    }

    override fun getCommandName(): String = node.name
    override fun getCommandUsage(sender: ICommandSender): String = "/${node.name}"
    override fun getCommandAliases(): List<String> = aliases
    override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val input = if (args.isEmpty()) node.name else "${node.name} ${args.joinToString(" ")}"
        try {
            dispatcher.execute(input, sender)
        } catch (e: CommandSyntaxException) {
            val message = e.message ?: "Error when parsing command."
            ChatUtils.sendMessage("§c$message")
        } catch (e: Exception) {
            io.hamlook.aetheria.Aetheria.logger.severe("[ATHR] Command execution failed: ${e.message}")
            ChatUtils.sendMessage("§cCommand failed: ${e.message}")
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos,
    ): List<String> {
        val input = if (args.isEmpty()) node.name else "${node.name} ${args.joinToString(" ")}"
        return try {
            dispatcher.getCompletionSuggestions(dispatcher.parse(input, sender)).get().list.map { it.text }
        } catch (e: Exception) {
            io.hamlook.aetheria.Aetheria.logger.warning("[ATHR] Tab completion failed: ${e.message}")
            emptyList()
        }
    }
}

package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.context.CommandContext

class BrigadierCommandContext(private val context: CommandContext<Any?>) {

    val source: Any get() = context.source as Any

    fun <T> getArg(argument: BrigadierArgument<T>): T {
        @Suppress("UNCHECKED_CAST")
        return context.getArgument(argument.name, argument.type) as T
    }

    fun <T> get(argument: BrigadierArgument<T>): T = getArg(argument)
}

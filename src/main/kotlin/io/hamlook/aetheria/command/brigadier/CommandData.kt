package io.hamlook.aetheria.command.brigadier

interface CommandData {
    val name: String
    var aliases: List<String>
    var category: CommandCategory
    val descriptor: String

    fun getAllNames(): List<String> {
        return mutableListOf(name).apply { addAll(aliases) }
    }
}

package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import io.hamlook.aetheria.Aetheria
import io.hamlook.aetheria.api.event.AetheriaEventBus

object CommandsRegistry {
    private val dispatcher: CommandDispatcher<Any?> = CommandDispatcher()
    private val registeredNames: MutableSet<String> = mutableSetOf()
    private val commandNodes: MutableList<CommandData> = mutableListOf()

    // Opaque sender: no command reads .source (only args via getArg),
    // so a singleton shim suffices for any dispatcher we forward into.
    @JvmField
    val shimSource: Any = object {
        override fun toString(): String = "ATHR-CommandShim"
    }

    fun registerAll() {
        Aetheria.logger.info("[ATHR] Firing CommandRegistrationEvent")
        val event = CommandRegistrationEvent(dispatcher)
        AetheriaEventBus.INSTANCE.post(event)
        for (cmd in event.commands) {
            registeredNames.addAll(cmd.getAllNames().map { it.lowercase() })
        }
        commandNodes.clear()
        commandNodes.addAll(event.commands)
        Aetheria.logger.info("[ATHR] Registered ${event.commands.size} Brigadier commands")
    }

    @JvmStatic
    fun forwardExecute(rawInput: String): Boolean {
        val input = rawInput.trimStart('/').trim()
        if (input.isEmpty()) return false
        return try {
            dispatcher.execute(input, shimSource)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun getCommandNodes(): List<CommandData> = commandNodes.toList()

    fun isRegistered(name: String?): Boolean {
        if (name == null) return false
        return registeredNames.contains(name.lowercase())
    }

    fun firstWordOf(input: String?): String? {
        if (input == null) return null
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed[0] == '/') return null
        val sp = trimmed.indexOf(' ')
        return if (sp == -1) trimmed else trimmed.substring(0, sp)
    }
}

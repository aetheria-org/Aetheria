package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import io.hamlook.aetheria.api.event.AetheriaEvent
import java.util.function.Consumer

class CommandRegistrationEvent(
    val dispatcher: CommandDispatcher<Any?>,
) : AetheriaEvent() {
    private val builders = mutableListOf<CommandData>()

    val commands: List<CommandData> get() = builders

    fun registerBrigadier(name: String, builder: BaseBrigadierBuilder.() -> Unit) {
        val command = BaseBrigadierBuilder(name).apply(builder)
        command.hasUniqueName(builders)
        command.addToRegister(dispatcher, builders)
    }

    fun registerBrigadier(name: String, builder: Consumer<BaseBrigadierBuilder>) {
        val command = BaseBrigadierBuilder(name).apply { builder.accept(this) }
        command.hasUniqueName(builders)
        command.addToRegister(dispatcher, builders)
    }

    private fun CommandData.hasUniqueName(existing: List<CommandData>) {
        val allNames = getAllNames()
        for (existingCmd in existing) {
            for (existingName in existingCmd.getAllNames()) {
                if (existingName in allNames) {
                    io.hamlook.aetheria.Aetheria.logger.warning("[ATHR] Duplicate command name: $existingName")
                }
            }
        }
    }

    private fun BaseBrigadierBuilder.addToRegister(
        dispatcher: CommandDispatcher<Any?>,
        builders: MutableList<CommandData>,
    ) {
        val command = toCommand(dispatcher)
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(command)
        builders.add(this)
    }
}

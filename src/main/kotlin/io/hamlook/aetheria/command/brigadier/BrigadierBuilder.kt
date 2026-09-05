package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.command.ICommand
import com.mojang.brigadier.CommandDispatcher
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

typealias LiteralCommandBuilder = BrigadierBuilder<LiteralArgumentBuilder<Any?>>
typealias ArgumentCommandBuilder<T> = BrigadierBuilder<RequiredArgumentBuilder<Any?, T>>

class BaseBrigadierBuilder(override val name: String) : CommandData,
    BrigadierBuilder<LiteralArgumentBuilder<Any?>>(LiteralArgumentBuilder.literal<Any?>(name)) {
    override var aliases: List<String> = emptyList()
    override var category: CommandCategory = CommandCategory.USERS_ACTIVE
    override val descriptor: String get() = description
    lateinit var node: CommandNode<Any?>
    fun toCommand(dispatcher: CommandDispatcher<Any?>): ICommand = BrigadierCommand(this, dispatcher)
}

open class BrigadierBuilder<B : ArgumentBuilder<Any?, B>>(
    val builder: ArgumentBuilder<Any?, B>,
    private val hasGreedyArg: Boolean = false,
) {
    @JvmField var description: String = ""

    private fun checkGreedy() =
        require(!hasGreedyArg) { "Cannot add an argument/literal to a builder that has a greedy argument." }

    fun callback(block: BrigadierCommandContext.() -> Unit) {
        this.builder.executes {
            try {
                block(BrigadierCommandContext(it))
            } catch (e: Exception) {
                io.hamlook.aetheria.Aetheria.logger.severe("[ATHR] Command callback failed: ${e.message}")
            }
            1
        }
    }

    fun simpleCallback(block: () -> Unit) {
        this.builder.executes {
            try {
                block()
            } catch (e: Exception) {
                io.hamlook.aetheria.Aetheria.logger.severe("[ATHR] Command callback failed: ${e.message}")
            }
            1
        }
    }

    fun simpleCallback(block: Runnable) {
        this.builder.executes {
            try {
                block.run()
            } catch (e: Exception) {
                io.hamlook.aetheria.Aetheria.logger.severe("[ATHR] Command callback failed: ${e.message}")
            }
            1
        }
    }

    fun legacyCallbackArgs(block: (Array<String>) -> Unit) {
        argCallback("allArgs", BrigadierArguments.greedyString()) { allArgs ->
            block(allArgs.split(" ").toTypedArray())
        }
        simpleCallback { block(emptyArray()) }
    }

    fun legacyCallbackArgs(block: Consumer<Array<String>>) {
        argCallback("allArgs", BrigadierArguments.greedyString()) { allArgs ->
            block.accept(allArgs.split(" ").toTypedArray())
        }
        simpleCallback(Runnable { block.accept(emptyArray()) })
    }

    fun literal(vararg names: String, action: LiteralCommandBuilder.() -> Unit) {
        checkGreedy()
        for (name in names) {
            if (name.contains(' ')) {
                val parts = name.split(' ', limit = 2)
                literal(parts[0]) { literal(parts[1]) { action(this) } }
                continue
            }
            val child = BrigadierBuilder(LiteralArgumentBuilder.literal<Any?>(name))
            child.action()
            this.builder.then(child.builder)
        }
    }

    fun literal(name: String, action: Consumer<LiteralCommandBuilder>) {
        checkGreedy()
        val child = BrigadierBuilder(LiteralArgumentBuilder.literal<Any?>(name))
        action.accept(child)
        this.builder.then(child.builder)
    }

    fun literalCallback(vararg names: String, block: BrigadierCommandContext.() -> Unit) =
        literal(*names) { callback(block) }

    fun <T> arg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: Collection<String>,
        action: ArgumentCommandBuilder<T>.(BrigadierArgument<T>) -> Unit,
    ) = argWithProvider(name, argument, suggestions.toSuggestionProvider(), action)

    fun <T> arg(
        name: String,
        argument: ArgumentType<T>,
        action: ArgumentCommandBuilder<T>.(BrigadierArgument<T>) -> Unit,
    ) = argWithProvider(name, argument, null, action)

    fun <T> argCallback(
        name: String,
        argument: ArgumentType<T>,
        suggestions: Collection<String>,
        block: BrigadierCommandContext.(T) -> Unit,
    ) = argWithProvider(name, argument, suggestions.toSuggestionProvider()) { callback { block(getArg(it)) } }

    fun <T> argCallback(
        name: String,
        argument: ArgumentType<T>,
        callback: BrigadierCommandContext.(T) -> Unit,
    ) = argWithProvider(name, argument, null) { callback { callback(getArg(it)) } }

    private fun <T> argWithProvider(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>?,
        action: ArgumentCommandBuilder<T>.(BrigadierArgument<T>) -> Unit,
    ) {
        if (!name.contains(' ')) {
            internalArg(name, argument, suggestions) { action(BrigadierArgument.of(name)) }
            return
        }
        val parts = name.split(' ', limit = 2)
        literal(parts[0]) {
            internalArg(parts[1], argument, suggestions) { @Suppress("UNCHECKED_CAST") action(BrigadierArgument.of<T>(parts[1])) }
        }
    }

    private fun <T> internalArg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        action: ArgumentCommandBuilder<T>.() -> Unit,
    ) {
        checkGreedy()
        if (name.contains(' ')) {
            val parts = name.split(' ', limit = 2)
            literal(parts[0]) { internalArg(parts[1], argument, suggestions, action) }
            return
        }
        val isGreedy = argument.javaClass.simpleName.lowercase().contains("greedy")
        val child = BrigadierBuilder(
            RequiredArgumentBuilder.argument<Any?, T>(name, argument).apply {
                if (suggestions != null) suggests(suggestions)
            },
            isGreedy,
        )
        child.action()
        this.builder.then(child.builder)
    }

    private fun Collection<String>.toSuggestionProvider(): SuggestionProvider<Any?> =
        SuggestionProvider { _, builder ->
            val remaining = builder.remaining.lowercase()
            for (s in this@toSuggestionProvider) {
                if (s.lowercase().startsWith(remaining)) {
                    builder.suggest(s)
                }
            }
            CompletableFuture.completedFuture(builder.build())
        }
}

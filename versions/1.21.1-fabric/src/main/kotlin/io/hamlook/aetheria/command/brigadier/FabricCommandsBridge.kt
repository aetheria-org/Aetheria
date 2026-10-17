package io.hamlook.aetheria.command.brigadier

import io.hamlook.aetheria.utils.chat.ChatUtils
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object FabricCommandsBridge {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (node in CommandsRegistry.getCommandNodes()) {
                for (alias in node.getAllNames()) {
                    val run =
                        ClientCommandManager.literal(alias).executes { ctx -> runForwarded(ctx.input) }
                            .then(
                                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                    .executes { ctx -> runForwarded(ctx.input) },
                            )
                    dispatcher.register(run)
                }
            }
        }
    }

    private fun runForwarded(input: String): Int {
        if (!CommandsRegistry.forwardExecute(input)) {
            ChatUtils.sendMessage("Unknown command. Type /athr for options.")
        }
        return 1
    }
}

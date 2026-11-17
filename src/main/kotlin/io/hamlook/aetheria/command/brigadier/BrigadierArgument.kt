package io.hamlook.aetheria.command.brigadier

@Suppress("UNCHECKED_CAST")
class BrigadierArgument<T>(val name: String, val type: Class<T>) {
    companion object {
        fun <T> of(name: String): BrigadierArgument<T> {
            return BrigadierArgument(name, Any::class.java as Class<T>)
        }
    }
}

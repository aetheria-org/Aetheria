package io.hamlook.aetheria.command.brigadier

enum class CommandCategory(val color: String, val categoryName: String, val description: String) {
    MAIN("§6", "Main Command", "Core Aetheria commands"),
    USERS_ACTIVE("§e", "Normal Command", "Commands for regular use"),
    USERS_RESET("§e", "Normal Reset Command", "Commands that reset tracker data"),
    DEVELOPER_TEST("§5", "Developer Test Commands", "Commands for testing features"),
    DEVELOPER_DEBUG("§9", "Developer Debug Commands", "Commands for debugging and diagnostics"),
    INTERNAL("§8", "Internal Command", "Commands not meant to be called manually"),
    COMMUNITY("§b", "Community Command", "Community and social features"),
    EXTERNAL_SERVICE("§d", "External Service", "Commands that interact with external services"),
    ;

    companion object {
        val developmentCategories = listOf(DEVELOPER_DEBUG, DEVELOPER_TEST, INTERNAL)
    }
}

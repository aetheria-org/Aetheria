package io.hamlook.aetheria.features.misc.protect

import io.hamlook.aetheria.utils.ColorUtils
import io.hamlook.aetheria.utils.ContainerUtils
import io.hamlook.aetheria.utils.item.ItemUtils
import net.minecraft.inventory.ContainerChest

object ProtectionChecks {


    fun isDangerousGui(container: ContainerChest?): Boolean {
        if (container == null) return false

        val title = getCleanTitle(container)
        return isDangerousTitle(title)
    }


    fun hasSellItem(container: ContainerChest?): Boolean {
        if (container == null) return false

        val inventory = container.lowerChestInventory
        val size = inventory.sizeInventory

        for (i in 0 until size) {
            val stack = inventory.getStackInSlot(i) ?: continue

            // Check display name for "Sell Item" button
            if (stack.hasDisplayName()) {
                val displayName = ColorUtils.stripColor(stack.displayName)
                if (displayName == "Sell Item") {
                    return true
                }
            }

            // Check lore for buyback button (after item is sold)
            val loreLines = ItemUtils.getLoreLines(stack)
            for (line in loreLines) {
                val cleanLine = ColorUtils.stripColor(line).lowercase()

                if (cleanLine.contains("click to buyback")) {
                    return true
                }
            }
        }

        return false
    }


    fun shouldBlockMovement(container: ContainerChest?): Boolean {
        return isDangerousGui(container) || hasSellItem(container)
    }

    private fun getCleanTitle(container: ContainerChest): String {
        return ContainerUtils.getTitle(container)?.lowercase() ?: ""
    }


    private fun isDangerousTitle(title: String): Boolean {
        // Auction House
        if (title.contains("auction")) return true

        // Trading menus
        if (title.startsWith("you ") || title.contains("trading")) return true
        if (title.contains("trades") || title.contains("exchange")) return true

        // Salvage menus
        if (title.contains("salvage")) return true

        // Shop menus (but not workshop)
        if (title.contains("shop") && !title.contains("workshop")) return true

        // Bazaar sell offers
        if (title.contains("sell offer") || title.contains("create sell offer")) return true

        return false
    }
}
package io.hamlook.aetheria.utils.compat

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.registry.ClientRegistry

object KeybindCompat {
    @JvmStatic
    fun registerKeyBinding(keybinding: KeyBinding) {
        ClientRegistry.registerKeyBinding(keybinding)
    }
}

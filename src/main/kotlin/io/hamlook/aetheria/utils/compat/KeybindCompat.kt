package io.hamlook.aetheria.utils.compat

//#if FORGE
import net.minecraftforge.fml.client.registry.ClientRegistry
//#else
//$$ import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
//$$ import net.minecraft.client.util.InputUtil
//#endif
import net.minecraft.client.settings.KeyBinding

object KeybindCompat {
    @JvmStatic
    fun registerKeyBinding(keybinding: KeyBinding): Unit {
        //#if FORGE
        ClientRegistry.registerKeyBinding(keybinding)
        //#else
        //$$ KeyBindingHelper.registerKeyBinding(keybinding)
        //#endif
    }

    @JvmStatic
    fun createKeyBinding(description: String, code: Int, category: String): KeyBinding =
        //#if FORGE
        KeyBinding(description, code, category)
        //#else
        //$$ KeyBinding(description, InputUtil.Type.KEYSYM, code, category)
        //#endif
}

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
    fun wasPressed(keybinding: KeyBinding): Boolean =
        //#if MC < 1.16
        keybinding.isPressed
        //#else
        //$$ keybinding.wasPressed()
        //#endif

    @JvmStatic
    fun isPlayerListKeyDown(): Boolean =
        //#if MC < 1.16
        KeyboardCompat.isKeyDown(MinecraftCompat.getMinecraft().gameSettings.keyBindPlayerList.keyCode)
        //#else
        //$$ MinecraftCompat.getMinecraft().options.playerListKey.isPressed()
        //#endif

    @JvmStatic
    fun inventoryKeyMatches(keyCode: Int): Boolean =
        //#if MC < 1.16
        keyCode == MinecraftCompat.getMinecraft().gameSettings.keyBindInventory.keyCode
        //#else
        //$$ keyCode == InputUtil.fromTranslationKey(MinecraftCompat.getMinecraft().options.inventoryKey.boundKeyTranslationKey).code
        //#endif

    @JvmStatic
    fun createKeyBinding(description: String, code: Int, category: String): KeyBinding =
        //#if FORGE
        KeyBinding(description, code, category)
        //#else
        //$$ KeyBinding(description, InputUtil.Type.KEYSYM, code, category)
        //#endif
}

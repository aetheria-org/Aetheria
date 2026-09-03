package io.hamlook.aetheria.utils.compat

import net.minecraftforge.fml.common.Loader

object ModCompat {
    @JvmStatic
    fun isModLoaded(modId: String): Boolean = Loader.isModLoaded(modId)

    @JvmStatic
    fun getMcVersion(): String = Loader.instance().getMCVersionString()

    @JvmStatic
    fun exitJava(code: Int) {
        System.exit(code)
    }
}

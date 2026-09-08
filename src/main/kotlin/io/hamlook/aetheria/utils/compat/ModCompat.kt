package io.hamlook.aetheria.utils.compat

import net.minecraftforge.fml.common.Loader
import java.io.File

object ModCompat {
    @JvmStatic
    fun isModLoaded(modId: String): Boolean = Loader.isModLoaded(modId)

    @JvmStatic
    fun getMcVersion(): String = Loader.instance().getMCVersionString()

    @JvmStatic
    fun getModIds(): List<String> = Loader.instance().getModList().map { it.modId }

    @JvmStatic
    fun getModJar(): File? = Loader.instance().indexedModList["aetheria"]?.source

    @JvmStatic
    fun exitJava(code: Int) {
        System.exit(code)
    }
}

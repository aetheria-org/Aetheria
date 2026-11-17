package io.hamlook.aetheria.utils.compat

//#if FORGE
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.FMLCommonHandler
//#else
//$$ import net.fabricmc.loader.api.FabricLoader
//$$ import kotlin.system.exitProcess
//#endif
import java.io.File
import java.net.URI

object ModCompat {
    @JvmStatic
    fun isModLoaded(modId: String): Boolean {
        //#if FORGE
        return Loader.isModLoaded(modId)
        //#else
        //$$ return FabricLoader.getInstance().isModLoaded(modId)
        //#endif
    }

    @JvmStatic
    fun getMcVersion(): String {
        //#if MC < 1.16
        return Loader.instance().getMCVersionString()
        //#elseif FORGE
        //$$ return net.minecraftforge.fml.loading.FMLEnvironment.dist.runtimeVersion
        //#else
        //$$ return net.minecraft.SharedConstants.getGameVersion().name
        //#endif
    }

    @JvmStatic
    fun getModIds(): List<String> {
        //#if MC < 1.16
        return Loader.instance().getModList().map { it.modId }
        //#elseif FORGE
        //$$ return Loader.instance().getModList().map { it.modId }
        //#else
        //$$ return FabricLoader.getInstance().allMods.map { it.metadata.id }
        //#endif
    }

    @JvmStatic
    fun getModJar(): File? {
        //#if MC < 1.16
        return Loader.instance().indexedModList["aetheria"]?.source
        //#elseif FORGE
        //$$ return Loader.instance().indexedModList["aetheria"]?.source
        //#else
        //$$ return FabricLoader.getInstance().getModContainer("aetheria").map { it.getRootPaths().firstOrNull()?.toUri()?.let { uri: URI -> File(uri) } }.orElse(null)
        //#endif
    }

    @JvmStatic
    fun exitJava(code: Int) {
        //#if FORGE
        FMLCommonHandler.instance().handleExit(code)
        //#else
        //$$ exitProcess(code)
        //#endif
    }
}

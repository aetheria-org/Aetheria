package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import net.minecraftforge.common.MinecraftForge
//#endif

object EventBusCompat {
    @JvmStatic
    fun register(instance: Any) {
        //#if MC < 1.16
        MinecraftForge.EVENT_BUS.register(instance)
        //#endif
    }

    @JvmStatic
    fun unregister(instance: Any) {
        //#if MC < 1.16
        MinecraftForge.EVENT_BUS.unregister(instance)
        //#endif
    }
}

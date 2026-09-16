package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import net.minecraft.client.renderer.RenderHelper
//#else
//$$ import net.minecraft.client.render.DiffuseLighting
//#endif

object RenderHelperCompat {

    @JvmStatic
    fun enableStandardItemLighting() {
        //#if MC < 1.16
        RenderHelper.enableStandardItemLighting()
        //#else
        //$$ DiffuseLighting.enableGuiDepthLighting()
        //#endif
    }

    @JvmStatic
    fun disableStandardItemLighting() {
        //#if MC < 1.16
        RenderHelper.disableStandardItemLighting()
        //#else
        //$$ DiffuseLighting.disableGuiDepthLighting()
        //#endif
    }

    @JvmStatic
    fun enableGUIStandardItemLighting() {
        //#if MC < 1.16
        RenderHelper.enableGUIStandardItemLighting()
        //#else
        //$$ DiffuseLighting.enableGuiDepthLighting()
        //#endif
    }
}

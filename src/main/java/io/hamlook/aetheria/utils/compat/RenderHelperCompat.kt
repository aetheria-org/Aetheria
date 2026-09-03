package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.RenderHelper

object RenderHelperCompat {

    @JvmStatic
    fun enableStandardItemLighting() {
        RenderHelper.enableStandardItemLighting()
    }

    @JvmStatic
    fun disableStandardItemLighting() {
        RenderHelper.disableStandardItemLighting()
    }

    @JvmStatic
    fun enableGUIStandardItemLighting() {
        RenderHelper.enableGUIStandardItemLighting()
    }
}

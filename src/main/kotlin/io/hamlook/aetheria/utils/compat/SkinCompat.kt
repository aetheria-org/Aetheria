package io.hamlook.aetheria.utils.compat

import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.ResourceLocation

object SkinCompat {

    @JvmStatic
    fun getSkinTexture(info: NetworkPlayerInfo): ResourceLocation =
        //#if MC < 1.16
        info.locationSkin
        //#else
        //$$ info.skinTextures.texture()
        //#endif
}

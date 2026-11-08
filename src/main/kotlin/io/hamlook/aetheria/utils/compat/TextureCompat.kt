package io.hamlook.aetheria.utils.compat

import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
//#if MC < 1.16
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import io.hamlook.aetheria.Aetheria
//$$ import net.minecraft.client.texture.NativeImage
//$$ import net.minecraft.client.texture.NativeImageBackedTexture
//#endif

object TextureCompat {

    //#if MC < 1.16
    private class UploadedTexture(private var image: BufferedImage?) : AbstractTexture() {
        override fun loadTexture(resourceManager: IResourceManager) {
            val picture = image ?: return
            image = null
            TextureUtil.uploadTextureImageAllocate(getGlTextureId(), picture, false, false)
        }
    }
    //#else
    //$$ private val retained = HashMap<String, NativeImageBackedTexture>()
    //$$ private val retainedSizes = HashMap<String, Long>()
    //$$ private val oneShotTextures = HashMap<String, NativeImageBackedTexture>()
    //$$ private val oneShotSizes = HashMap<String, IntArray>()
    //$$ private val sizeCache = HashMap<String, IntArray>()
    //$$ private val sizeMissWarned = HashSet<String>()
    //#endif

    //#if MC < 1.16
    //#else
    //$$ private fun copyArgb(native: NativeImage, image: BufferedImage) {
    //$$     for (y in 0 until image.height) {
    //$$         for (x in 0 until image.width) {
    //$$             native.setColor(x, y, argbToAbgr(image.getRGB(x, y)))
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ private fun argbToAbgr(argb: Int): Int =
    //$$     (argb and 0xFF00FF00.toInt()) or ((argb and 0xFF) shl 16) or ((argb shr 16) and 0xFF)
    //#endif

    @JvmStatic
    @JvmOverloads
    fun uploadTexture(location: ResourceLocation, image: BufferedImage?, linear: Boolean = false): Boolean {
        if (image == null) return false
        //#if MC < 1.16
        val tm = MinecraftCompat.getMinecraft().getTextureManager()
        tm.deleteTexture(location)
        tm.loadTexture(location, DynamicTexture(image))
        if (linear) {
            tm.bindTexture(location)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        }
        return true
        //#else
        //$$ val tm = MinecraftCompat.getMinecraft().getTextureManager()
        //$$ val key = location.toString()
        //$$ val dims = image.width.toLong() shl 32 or image.height.toLong()
        //$$ var tex = retained[key]
        //$$ if (tex == null || retainedSizes[key] != dims) {
        //$$     tex?.close()
        //$$     val native = NativeImage(NativeImage.Format.RGBA, image.width, image.height, false)
        //$$     copyArgb(native, image)
        //$$     tex = NativeImageBackedTexture(native)
        //$$     tm.registerTexture(location, tex)
        //$$     retained[key] = tex
        //$$     retainedSizes[key] = dims
        //$$ } else {
        //$$     val current = tex.image ?: return false
        //$$     copyArgb(current, image)
        //$$     tex.upload()
        //$$ }
        //$$ if (linear) tex.setFilter(true, false)
        //$$ return true
        //#endif
    }

    @JvmStatic
    fun prepareImage(image: BufferedImage): Any {
        //#if MC < 1.16
        return image
        //#else
        //$$ val native = NativeImage(NativeImage.Format.RGBA, image.width, image.height, false)
        //$$ copyArgb(native, image)
        //$$ return native
        //#endif
    }

    @JvmStatic
    @JvmOverloads
    fun uploadPrepared(location: ResourceLocation, prepared: Any, linear: Boolean = false) {
        //#if MC < 1.16
        val tm = MinecraftCompat.getMinecraft().getTextureManager()
        tm.deleteTexture(location)
        tm.loadTexture(location, UploadedTexture(prepared as BufferedImage))
        if (linear) {
            tm.bindTexture(location)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        }
        //#else
        //$$ val tm = MinecraftCompat.getMinecraft().getTextureManager()
        //$$ val key = location.toString()
        //$$ val native = prepared as NativeImage
        //$$ oneShotTextures.remove(key)?.clearGlId()
        //$$ val texture = NativeImageBackedTexture(native)
        //$$ tm.registerTexture(location, texture)
        //$$ oneShotTextures[key] = texture
        //$$ oneShotSizes[key] = intArrayOf(native.width, native.height)
        //$$ sizeCache.remove(key)
        //$$ if (linear) texture.setFilter(true, false)
        //$$ if (RenderSystem.isOnRenderThread()) texture.setImage(null)
        //#endif
    }

    @JvmStatic
    fun releaseTexture(location: ResourceLocation) {
        //#if MC < 1.16
        val tm = MinecraftCompat.getMinecraft().getTextureManager()
        tm.deleteTexture(location)
        tm.loadTexture(location, DynamicTexture(1, 1))
        //#else
        //$$ val tm = MinecraftCompat.getMinecraft().getTextureManager()
        //$$ val key = location.toString()
        //$$ oneShotSizes.remove(key)
        //$$ sizeCache.remove(key)
        //$$ oneShotTextures.remove(key)?.clearGlId()
        //$$ tm.destroyTexture(location)
        //#endif
    }

    @JvmStatic
    fun bindTexture(location: ResourceLocation) {
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(location)
    }

    //#if MC < 1.16
    //#else
    //$$ @JvmStatic
    //$$ fun describeTexture(location: ResourceLocation): String {
    //$$     return try {
    //$$         val t = MinecraftCompat.getMinecraft().getTextureManager().getTexture(location)
    //$$         if (t == null) "null" else t.javaClass.simpleName
    //$$     } catch (e: Exception) {
    //$$         "err"
    //$$     }
    //$$ }
    //#endif

    /**
     * Makes [location] the sampled texture for subsequently emitted GL quads
     * (draws inside a pushed matrix: rotated/scaled heads, arrows). Legacy
     * binds (fixed-function samples the bound object); modern sets shader
     * sampler 0 (a manual bind alone leaves it stale).
     */
    @JvmStatic
    fun bindShaderTexture(location: ResourceLocation) {
        //#if MC < 1.16
        bindTexture(location)
        //#else
        //$$ RenderSystem.setShaderTexture(0, location)
        //#endif
    }

    /**
     * Draws a textured rect with normalized (0..1) UVs. Legacy binds then
     * emits a GL quad; modern resolves the texture size at runtime and draws
     * through the DrawContext region overload (source size and dest size are
     * independent — stretched 9-slice tiles and downscaled icons both need
     * the region form; a manual bind alone leaves sampler 0 stale on modern).
     *
     * Blending is enabled for the draw and disabled after it. This is required
     * on modern: the DrawContext textured-quad overload draws immediately and
     * does not touch blend state, while every queued GUI layer flush ends with
     * `RenderSystem.disableBlend()`. Without it the quad is drawn with alpha
     * ignored, so fully transparent texels that still carry colour paint as
     * opaque (a rim in the sprite's own colour, square corners on round art).
     */
    @JvmStatic
    fun drawBoundTextured(location: ResourceLocation, x: Float, y: Float, w: Float, h: Float,
                          u0: Float, v0: Float, u1: Float, v1: Float) {
        GlStateManagerCompat.enableBlend()
        GlStateManagerCompat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        //#if MC < 1.16
        bindTexture(location)
        GlStateManagerCompat.enableTexture2D()
        val vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX)
        vb.pos(x.toDouble(), (y + h).toDouble(), 0.0).tex(u0.toDouble(), v1.toDouble()).endVertex()
        vb.pos((x + w).toDouble(), (y + h).toDouble(), 0.0).tex(u1.toDouble(), v1.toDouble()).endVertex()
        vb.pos((x + w).toDouble(), y.toDouble(), 0.0).tex(u1.toDouble(), v0.toDouble()).endVertex()
        vb.pos(x.toDouble(), y.toDouble(), 0.0).tex(u0.toDouble(), v0.toDouble()).endVertex()
        vb.draw()
        //#else
        //$$ val dims = textureSize(location)
        //$$ val tw = dims[0]
        //$$ val th = dims[1]
        //$$ val regionW = Math.round((u1 - u0) * tw)
        //$$ val regionH = Math.round((v1 - v0) * th)
        //$$ DrawContextUtils.drawTexturedRegion(location, x.toInt(), y.toInt(), w.toInt(), h.toInt(),
        //$$     u0 * tw, v0 * th, regionW, regionH, tw, th)
        //#endif
        GlStateManagerCompat.disableBlend()
    }

    @JvmStatic
    fun dynamicLocation(key: String): ResourceLocation =
        TextCompat.createResourceLocation("aetheria", "dynamic/" + key.lowercase())

    //#if MC < 1.16
    //#else
    //$$ /**
    //$$  * Pixel dims of a texture. Runtime-registered textures (emoji sheets,
    //$$  * skins, icons) expose their image; static files are loaded lazily as
    //$$  * `ResourceTexture`, which hides the dims, so those are read from the
    //$$  * resource pack instead. Only resolved dims are cached: a texture
    //$$  * registered after the first call is still picked up.
    //$$  */
    //$$ @JvmStatic
    //$$ fun textureSize(location: Identifier): IntArray {
    //$$     val key = location.toString()
    //$$     sizeCache[key]?.let { return it }
    //$$     val dims = readDims(location)
    //$$     if (dims == null) {
    //$$         if (sizeMissWarned.add(key)) {
    //$$             Aetheria.logger.warning("[ATHR] textureSize miss for $key, using 256x256")
    //$$         }
    //$$         return intArrayOf(256, 256)
    //$$     }
    //$$     sizeCache[key] = dims
    //$$     return dims
    //$$ }
    //$$
    //$$ private fun readDims(location: Identifier): IntArray? {
    //$$     oneShotSizes[location.toString()]?.let { return it }
    //$$     val registered = MinecraftCompat.getMinecraft().textureManager.getTexture(location) as? NativeImageBackedTexture
    //$$     registered?.image?.let { return intArrayOf(it.width, it.height) }
    //$$     return try {
    //$$         val res = MinecraftCompat.getMinecraft().resourceManager.getResource(location).orElse(null)
    //$$             ?: return null
    //$$         res.inputStream.use { stream ->
    //$$             NativeImage.read(stream)?.let { img ->
    //$$                 val dims = intArrayOf(img.width, img.height)
    //$$                 img.close()
    //$$                 dims
    //$$             }
    //$$         }
    //$$     } catch (e: Exception) {
    //$$         null
    //$$     }
    //$$ }
    //#endif
}

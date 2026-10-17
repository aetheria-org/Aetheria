package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
//#if MC > 1.21
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import net.minecraft.client.texture.NativeImage
//#endif

object TextureCompat {

    //#if MC > 1.21
    //$$ private val retained = HashMap<String, NativeImageBackedTexture>()
    //$$ private val retainedSizes = HashMap<String, Long>()
    //$$
    //$$ private fun copyArgb(native: NativeImage, image: BufferedImage) {
    //$$     for (y in 0 until image.height) {
    //$$         for (x in 0 until image.width) {
    //$$             val argb = image.getRGB(x, y)
    //$$             native.setColor(x, y, argbToAbgr(argb))
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
     */
    @JvmStatic
    fun drawBoundTextured(location: ResourceLocation, x: Float, y: Float, w: Float, h: Float,
                          u0: Float, v0: Float, u1: Float, v1: Float) {
        //#if MC < 1.16
        bindTexture(location)
        GlStateManagerCompat.enableTexture2D()
        GlStateManagerCompat.enableBlend()
        GlStateManagerCompat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        val vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX)
        vb.pos(x.toDouble(), (y + h).toDouble(), 0.0).tex(u0.toDouble(), v1.toDouble()).endVertex()
        vb.pos((x + w).toDouble(), (y + h).toDouble(), 0.0).tex(u1.toDouble(), v1.toDouble()).endVertex()
        vb.pos((x + w).toDouble(), y.toDouble(), 0.0).tex(u1.toDouble(), v0.toDouble()).endVertex()
        vb.pos(x.toDouble(), y.toDouble(), 0.0).tex(u0.toDouble(), v0.toDouble()).endVertex()
        vb.draw()
        GlStateManagerCompat.disableBlend()
        //#else
        //$$ val dims = textureSize(location)
        //$$ val tw = dims[0]
        //$$ val th = dims[1]
        //$$ val regionW = ((u1 - u0) * tw).toInt()
        //$$ val regionH = ((v1 - v0) * th).toInt()
        //$$ DrawContextUtils.drawTexturedRegion(location, x.toInt(), y.toInt(), w.toInt(), h.toInt(),
        //$$     u0 * tw, v0 * th, regionW, regionH, tw, th)
        //#endif
    }

    @JvmStatic
    fun dynamicLocation(key: String): ResourceLocation =
        TextCompat.createResourceLocation("aetheria", "dynamic/" + key.lowercase())

    //#if MC < 1.21
    //#else
    //$$ private val sizeCache = HashMap<String, IntArray>()
    //$$ private val sizeMissWarned = HashSet<String>()
    //$$
    //$$ /**
    //$$  * Pixel dims of a registered/static texture, read once per id from the
    //$$  * resource itself. `getTexture` returns an async `ResourceTexture` for
    //$$  * static files (never a `NativeImageBackedTexture`), so it cannot
    //$$  * supply dims — hence this cache.
    //$$  */
    //$$ @JvmStatic
    //$$ fun textureSize(location: Identifier): IntArray {
    //$$     val key = location.toString()
    //$$     sizeCache[key]?.let { return it }
    //$$     val dims = readDims(location) ?: intArrayOf(256, 256).also {
    //$$         if (sizeMissWarned.add(key)) {
    //$$             io.hamlook.aetheria.Aetheria.logger.warning("[ATHR] textureSize miss for $key, using 256x256")
    //$$         }
    //$$     }
    //$$     sizeCache[key] = dims
    //$$     return dims
    //$$ }
    //$$
    //$$ private fun readDims(location: Identifier): IntArray? {
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

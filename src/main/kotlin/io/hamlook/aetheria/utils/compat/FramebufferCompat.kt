package io.hamlook.aetheria.utils.compat

import net.minecraft.client.shader.Framebuffer
//#if MC > 1.21
//$$ import net.minecraft.client.gl.SimpleFramebuffer
//$$ import org.lwjgl.opengl.GL30
//#endif

object FramebufferCompat {

    @JvmStatic
    fun createStencilFramebuffer(w: Int, h: Int): Framebuffer =
        //#if MC < 1.21
        Framebuffer(w, h, true).also { it.enableStencil() }
        //#else
        //$$ createModern(w, h)
        //#endif

    //#if MC > 1.21
    //$$ private fun createModern(w: Int, h: Int): Framebuffer {
    //$$     try {
    //$$         val fb = SimpleFramebuffer(w, h, false, false)
    //$$         fb.beginWrite(true)
    //$$         val rb = GL30.glGenRenderbuffers()
    //$$         GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rb)
    //$$         GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, w, h)
    //$$         GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rb)
    //$$         fb.checkFramebufferStatus()
    //$$         fb.endWrite()
    //$$         return fb
    //$$     } catch (e: Exception) {
    //$$         return SimpleFramebuffer(w, h, true, false)
    //$$     }
    //$$ }
    //#endif
}

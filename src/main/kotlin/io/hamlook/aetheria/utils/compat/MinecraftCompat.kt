package io.hamlook.aetheria.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity

object MinecraftCompat {
    @JvmStatic
    fun getMinecraft(): Minecraft =
        //#if MC < 1.16
        Minecraft.getMinecraft()
        //#else
        //$$ MinecraftClient.getInstance()
        //#endif

    @JvmStatic
    fun getLocalPlayer(): EntityPlayerSP? =
        //#if MC < 1.16
        Minecraft.getMinecraft().thePlayer
        //#else
        //$$ MinecraftClient.getInstance().player
        //#endif

    @JvmStatic
    fun getLocalPlayerOrError(): EntityPlayerSP =
        getLocalPlayer() ?: error("thePlayer is null")

    @JvmStatic
    fun getLocalWorld(): WorldClient? =
        //#if MC < 1.16
        Minecraft.getMinecraft().theWorld
        //#else
        //$$ MinecraftClient.getInstance().world
        //#endif

    @JvmStatic
    fun getLocalWorldOrError(): WorldClient =
        getLocalWorld() ?: error("theWorld is null")

    @JvmStatic
    fun localPlayerExists(): Boolean = getLocalPlayer() != null

    @JvmStatic
    fun localWorldExists(): Boolean = getLocalWorld() != null

    @JvmStatic
    fun isLocalPlayer(entity: Entity?): Boolean =
        entity != null && entity == getLocalPlayer()

    @JvmStatic
    fun getShowDebugHud(): Boolean =
        //#if MC < 1.16
        Minecraft.getMinecraft().gameSettings.showDebugInfo
        //#elseif MC < 1.21
        //$$ MinecraftClient.getInstance().debugHud.shouldShowDebugHud()
        //#else
        //$$ MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()
        //#endif

    @JvmStatic
    fun getGameSettings(): GameSettings =
        //#if MC < 1.16
        Minecraft.getMinecraft().gameSettings
        //#else
        //$$ MinecraftClient.getInstance().options
        //#endif

    @JvmStatic
    fun getDisplayWidth(): Int =
        //#if MC < 1.16
        Minecraft.getMinecraft().displayWidth
        //#else
        //$$ MinecraftClient.getInstance().window.width.toInt()
        //#endif

    @JvmStatic
    fun getDisplayHeight(): Int =
        //#if MC < 1.16
        Minecraft.getMinecraft().displayHeight
        //#else
        //$$ MinecraftClient.getInstance().window.height.toInt()
        //#endif

    @JvmStatic
    fun getFontRenderer(): FontRenderer =
        //#if MC < 1.16
        Minecraft.getMinecraft().fontRendererObj
        //#else
        //$$ MinecraftClient.getInstance().textRenderer
        //#endif

    @JvmStatic
    fun getCurrentScreen(): GuiScreen? =
        //#if MC < 1.16
        Minecraft.getMinecraft().currentScreen
        //#else
        //$$ MinecraftClient.getInstance().currentScreen
        //#endif

    @JvmStatic
    fun getRenderGlobal(): RenderGlobal =
        //#if MC < 1.16
        Minecraft.getMinecraft().renderGlobal
        //#else
        //$$ MinecraftClient.getInstance().worldRenderer
        //#endif

    @JvmStatic
    fun getIngameGui(): net.minecraft.client.gui.GuiIngame =
        //#if MC < 1.16
        Minecraft.getMinecraft().ingameGUI
        //#else
        //$$ MinecraftClient.getInstance().inGameHud
        //#endif
}

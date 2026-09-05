package io.hamlook.aetheria.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity

object MinecraftCompat {
    @JvmStatic
    fun getMinecraft(): Minecraft = Minecraft.getMinecraft()

    @JvmStatic
    fun getLocalPlayer(): EntityPlayerSP? = Minecraft.getMinecraft().thePlayer

    @JvmStatic
    fun getLocalPlayerOrError(): EntityPlayerSP =
        getLocalPlayer() ?: error("thePlayer is null")

    @JvmStatic
    fun getLocalWorld(): WorldClient? = Minecraft.getMinecraft().theWorld

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
    fun getShowDebugHud(): Boolean = Minecraft.getMinecraft().gameSettings.showDebugInfo

    @JvmStatic
    fun getGameSettings(): GameSettings = Minecraft.getMinecraft().gameSettings

    @JvmStatic
    fun getDisplayWidth(): Int = Minecraft.getMinecraft().displayWidth

    @JvmStatic
    fun getDisplayHeight(): Int = Minecraft.getMinecraft().displayHeight

    @JvmStatic
    fun getFontRenderer(): net.minecraft.client.gui.FontRenderer =
        Minecraft.getMinecraft().fontRendererObj

    @JvmStatic
    fun getRenderGlobal(): net.minecraft.client.renderer.RenderGlobal =
        Minecraft.getMinecraft().renderGlobal

    @JvmStatic
    fun isCtrlKeyDown(): Boolean = GuiScreen.isCtrlKeyDown()

    @JvmStatic
    fun isShiftKeyDown(): Boolean = GuiScreen.isShiftKeyDown()
}

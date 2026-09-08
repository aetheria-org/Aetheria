package io.hamlook.aetheria.utils.compat

import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object WorldCompat {

    @JvmStatic
    fun getLoadedPlayers(world: WorldClient): List<EntityPlayer> = world.playerEntities

    @JvmStatic
    fun getAllEntities(world: World): List<Entity> = world.loadedEntityList
}
